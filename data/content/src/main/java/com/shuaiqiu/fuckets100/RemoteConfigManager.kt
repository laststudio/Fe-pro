package com.shuaiqiu.fuckets100

import android.util.Log
import com.shuaiqiu.fuckets100.core.common.AppBuildInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.URL
import java.net.URLConnection

/**
 * 远程配置管理器。
 *
 * 进程级单例，由 Hilt/AppContainer 创建；不再使用全局可变状态，
 * 构建信息通过构造函数注入，替代对 app 层 BuildConfig 的直接依赖。
 */
class RemoteConfigManager(
    private val buildInfo: AppBuildInfo,
) {
    private companion object {
        const val TAG = "RemoteConfigManager"
        const val CONFIG_BASE_URL = "https://api.lastudio.cc/software/fe/config"
        const val TIMEOUT_MS = 5000L  // 5秒超时喵~
    }

    private val configUrls: List<String>
        get() = listOf(
            "$CONFIG_BASE_URL?version_code=${buildInfo.versionCode}",
        )

    @Volatile
    var cachedConfig: RemoteConfig? = null
        private set

    fun isIntegrityCheckEnabled(): Boolean = cachedConfig?.integrityCheckEnabled ?: true

    fun isFeatureEnabled(featureKey: String, defaultValue: Boolean = true): Boolean =
        cachedConfig?.features?.get(featureKey) ?: defaultValue
    
    /**
     * 网络异常 - 用于标识网络问题导致配置获取失败
     */
    class NetworkException(message: String) : Exception(message)

    private fun RemoteConfig.toUpdateStatus(
        isKillSwitch: Boolean,
        showDialog: Boolean,
        message: String,
        isForce: Boolean,
        updateUrl: String,
        noticeMessage: String
    ): UpdateStatus {
        return UpdateStatus(
            isKillSwitch = isKillSwitch,
            showDialog = showDialog,
            message = message,
            isForce = isForce,
            updateUrl = updateUrl,
            noticeMessage = noticeMessage,
            announcementTitle = announcementTitle,
            announcementMessage = announcementMessage,
            announcementUpdatedAt = announcementUpdatedAt,
            announcementUrl = announcementUrl,
            changelogTitle = changelogTitle,
            changelogSummary = changelogSummary,
            changelogContent = changelogContent,
            verificationCode = verificationCode,
            verificationTitle = verificationTitle,
            verificationMessage = verificationMessage,
            donateEnabled = donateEnabled,
            integrityCheckEnabled = integrityCheckEnabled,
            features = features
        )
    }

    private fun RemoteConfig.toRuntimeStatus(ignoreVerification: Boolean = false): UpdateStatus {
        return when {
            isKillSwitchOn -> {
                Log.w(TAG, "🚨 KillSwitch 开启! 应用即将退出")
                toUpdateStatus(
                    isKillSwitch = true,
                    showDialog = false,
                    message = "",
                    isForce = false,
                    updateUrl = "",
                    noticeMessage = "程序异常"
                )
            }
            !ignoreVerification &&
                verificationCode.isNotBlank() &&
                SettingsManager.getLocalVerificationCode() != verificationCode -> {
                Log.w(TAG, "🔐 启动验证未通过，等待用户输入验证码")
                toUpdateStatus(
                    isKillSwitch = false,
                    showDialog = false,
                    message = "",
                    isForce = false,
                    updateUrl = "",
                    noticeMessage = ""
                ).copy(
                    requiresVerification = true
                )
            }
            latestVersionCode > buildInfo.versionCode -> {
                Log.i(TAG, "📢 发现新版本: $latestVersionCode (当前: ${buildInfo.versionCode})")
                Log.d(TAG, "📢 updateMessage: '$updateMessage', updateUrl: '$updateUrl'")
                toUpdateStatus(
                    isKillSwitch = false,
                    showDialog = true,
                    message = updateMessage.ifEmpty { "发现新版本: $latestVersionCode" },
                    isForce = isForce,
                    updateUrl = updateUrl,
                    noticeMessage = ""
                )
            }
            noticeMessage.isNotEmpty() -> {
                Log.i(TAG, "📢 收到公告: $noticeMessage")
                toUpdateStatus(
                    isKillSwitch = false,
                    showDialog = false,
                    message = "",
                    isForce = false,
                    updateUrl = "",
                    noticeMessage = noticeMessage
                )
            }
            else -> {
                Log.d(TAG, "✅ 已是最新版本，无需更新")
                toUpdateStatus(
                    isKillSwitch = false,
                    showDialog = false,
                    message = "",
                    isForce = false,
                    updateUrl = "",
                    noticeMessage = ""
                )
            }
        }
    }
    
    private suspend fun fetchFirstAvailableConfig(): Triple<String, String, Long> {
        val errors = mutableListOf<String>()

        for (configUrl in configUrls) {
            Log.d(TAG, "📡 尝试 URL: $configUrl")
            val startTime = System.currentTimeMillis()
            try {
                val response = withTimeoutOrNull(TIMEOUT_MS) {
                    val urlConnection = URL(configUrl).openConnection() as URLConnection
                    urlConnection.connectTimeout = TIMEOUT_MS.toInt()
                    urlConnection.readTimeout = TIMEOUT_MS.toInt()
                    urlConnection.connect()
                    urlConnection.inputStream.bufferedReader().readText()
                }

                if (response.isNullOrBlank()) {
                    errors += "$configUrl: empty or timeout"
                    Log.w(TAG, "⚠️ 配置源失败: $configUrl empty or timeout")
                    continue
                }

                return Triple(response, configUrl, System.currentTimeMillis() - startTime)
            } catch (e: Exception) {
                errors += "$configUrl: ${e.javaClass.simpleName}: ${e.message}"
                Log.w(TAG, "⚠️ 配置源失败: $configUrl ${e.message}")
            }
        }

        throw NetworkException("所有远程配置源均失败: ${errors.joinToString(" | ")}")
    }
    /**
     * 获取远程配置
     * 在 IO 线程执行，不阻塞主线程
     * 5 秒内无法获取配置或网络失败时抛出可恢复异常。
     * 
     * @param context Context
     * @return RemoteConfig
     * @throws NetworkException 网络失败或超时时抛出
     */
    suspend fun fetchConfig(): RemoteConfig {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "═══════════════════════════════════════════")
                Log.d(TAG, "🌐 开始连接远程配置服务器...")
                Log.d(TAG, "⏱️ 超时设置: ${TIMEOUT_MS}ms")

                val (response, sourceUrl, elapsedMs) = fetchFirstAvailableConfig()
                Log.d(TAG, "✅ 连接成功! 响应时间: ${elapsedMs}ms")
                Log.d(TAG, "📡 成功来源: $sourceUrl")
                Log.d(TAG, "📦 原始响应: ${response.take(200)}...")

                val json = JSONObject(response)
                
                val noticeMessage = json.optString("noticeMessage", "")
                val announcementMessage = json.optString("announcementMessage", noticeMessage)
                val changelogSummary = json.optString("changelogSummary", "")
                val verificationCode = json.optString("verificationCode", "")
                val verificationTitle = json.optString("verificationTitle", "")
                val verificationMessage = json.optString("verificationMessage", "")
                val featuresJson = json.optJSONObject("features")
                val features = mutableMapOf<String, Boolean>()
                if (featuresJson != null) {
                    val keys = featuresJson.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        features[k] = featuresJson.optBoolean(k, true)
                    }
                }

                val config = RemoteConfig(
                    key = json.optString("key", ""),
                    releaseId = if (json.has("releaseId") && !json.isNull("releaseId")) {
                        json.optLong("releaseId")
                    } else null,
                    configRevision = json.optInt("configRevision", 0),
                    version = json.optString("version", ""),
                    latestVersionCode = json.optInt("latestVersionCode", buildInfo?.versionCode ?: 0),
                    updateUrl = json.optString("updateUrl", ""),
                    isKillSwitchOn = json.optBoolean("isKillSwitchOn", false),
                    isForce = json.optBoolean("isForce", false),
                    updateMessage = json.optString("updateMessage", ""),
                    noticeMessage = noticeMessage,
                    announcementTitle = json.optString("announcementTitle", "").ifBlank {
                        if (announcementMessage.isNotBlank()) "公告" else ""
                    },
                    announcementMessage = announcementMessage,
                    announcementUpdatedAt = json.optString("announcementUpdatedAt", ""),
                    announcementUrl = json.optString("announcementUrl", ""),
                    changelogTitle = json.optString("changelogTitle", "").ifBlank {
                        if (changelogSummary.isNotBlank()) "更新日志" else ""
                    },
                    changelogSummary = changelogSummary,
                    changelogContent = json.optString("changelogContent", ""),
                    verificationCode = verificationCode,
                    verificationTitle = verificationTitle,
                    verificationMessage = verificationMessage,
                    donateEnabled = json.optBoolean("donateEnabled", true),
                    integrityCheckEnabled = json.optBoolean("integrityCheckEnabled", true),
                    features = features
                )
                
                Log.d(TAG, "📋 解析后的配置:")
                Log.d(TAG, "   - latestVersionCode: ${config.latestVersionCode}")
                Log.d(TAG, "   - features: ${config.features}")
                Log.d(TAG, "   - updateUrl: ${config.updateUrl}")
                Log.d(TAG, "   - isKillSwitchOn: ${config.isKillSwitchOn}")
                Log.d(TAG, "   - isForce: ${config.isForce}")
                Log.d(TAG, "   - updateMessage: ${config.updateMessage.take(120)}")
                Log.d(TAG, "   - noticeMessage: ${config.noticeMessage.take(120)}")
                Log.d(TAG, "   - announcementTitle: ${config.announcementTitle}")
                Log.d(TAG, "   - announcementMessage: ${config.announcementMessage.take(120)}")
                Log.d(TAG, "   - announcementUpdatedAt: ${config.announcementUpdatedAt}")
                Log.d(TAG, "   - announcementUrl: ${config.announcementUrl}")
                Log.d(TAG, "   - changelogTitle: ${config.changelogTitle}")
                Log.d(TAG, "   - changelogSummary: ${config.changelogSummary.take(120)}")
                Log.d(TAG, "   - verificationCode: ${if (config.verificationCode.isBlank()) "<empty>" else "***"}")
                Log.d(TAG, "   - verificationTitle: ${config.verificationTitle}")
                Log.d(TAG, "   - verificationMessage: ${config.verificationMessage.take(120)}")
                Log.d(TAG, "   - donateEnabled: ${config.donateEnabled}")
                Log.d(TAG, "   - integrityCheckEnabled: ${config.integrityCheckEnabled}")
                Log.d(TAG, "   - 当前版本: ${buildInfo?.versionCode ?: 0}")
                Log.d(TAG, "═══════════════════════════════════════════")
                
                cachedConfig = config
                config
            } catch (e: Exception) {
                when (e) {
                    is NetworkException -> {
                        Log.e(TAG, "❌ 网络异常: ${e.message}")
                    }
                    else -> {
                        Log.e(TAG, "❌ 连接失败: ${e.message}")
                        Log.e(TAG, "   异常类型: ${e.javaClass.simpleName}")
                    }
                }
                // 网络失败时抛出异常，由启动门禁展示可重试状态。
                throw NetworkException(e.message ?: "未知网络错误")
            }
        }
    }
    
    /**
     * 检查是否需要更新或锁定
     * 在 IO 线程执行，不阻塞主线程
     * 网络失败时抛出可恢复异常。
     *
     * @param context Context
     * @return UpdateStatus 更新状态
     * @throws NetworkException 网络失败或超时时抛出
     */
    suspend fun checkStatus(): UpdateStatus {
        return fetchConfig().toRuntimeStatus()
    }

    suspend fun checkStatusIgnoringVerification(): UpdateStatus {
        return fetchConfig().toRuntimeStatus(ignoreVerification = true)
    }
}
