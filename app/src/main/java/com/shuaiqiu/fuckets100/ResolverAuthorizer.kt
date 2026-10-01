package com.shuaiqiu.fuckets100

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 答案解析 (answer_parser) 功能的内部授权门控。
 *
 * 调用 libresolver.so 内部联网授权: 后端 /client-auth/features/authorize
 * 返回 Ed25519 签名 grant, Native 层验签通过才放行。后台关闭该功能后,
 * 下一次授权请求立即被拒绝, 答案解析随之阻断。
 *
 * 服务器公钥需与生产后端一致 (可在后台 `软件认证 -> 签名公钥` 查看)。
 */
internal object ResolverAuthorizer {

    const val BASE_URL = "https://api.lastudio.cc"
    const val SOFTWARE_KEY = "fe"
    const val FEATURE_KEY = "answer_parser"

    // 生产后端 Ed25519 公钥 (urlsafe base64 no-pad)。换钥时需同步更新。
    const val PUBLIC_KEY_B64 = "8i06qnGdq6lm6wj8NDm22Nh0_nP6_k38Kg_Lz0xHIiY"

    @Volatile private var lastCloudAuthTime = 0L
    @Volatile private var lastCloudAuthToken: String? = null
    @Volatile private var lastResolverAuthTime = 0L
    @Volatile private var lastResolverAuthToken: String? = null
    private const val AUTH_CACHE_TTL_MS = 3 * 60 * 1000L // 3 分钟内存缓存，避免频繁点击重复请求网络

    fun invalidateAuthCache() {
        lastCloudAuthTime = 0L
        lastCloudAuthToken = null
        lastResolverAuthTime = 0L
        lastResolverAuthToken = null
    }

    /**
     * 内部授权门控。
     * @throws ResolverAuthorizationDeniedException 未登录/功能关闭/网络失败等
     */
    suspend fun requireAuthorized(context: Context) {
        val reason = ensureAuthorized(context)
        if (reason != null) throw ResolverAuthorizationDeniedException(reason)
    }

    /** 配置 SO 共用的后端、签名公钥和 access token，不提前指定实际功能权限。 */
    suspend fun requireConfigured(context: Context) {
        val reason = configureCloud(context)
        if (reason != null) throw ResolverAuthorizationDeniedException(reason)
    }

    suspend fun requireCloudAuthorized(context: Context) {
        val reason = ensureCloudAuthorized(context)
        if (reason != null) throw ResolverAuthorizationDeniedException(reason)
    }

    private suspend fun ensureCloudAuthorized(context: Context): String? = withContext(Dispatchers.IO) {
        val manager = FeAccountManager.get(context)
        val (available, unavailableReason) = manager.isFeatureAvailable("cloud_mode")
        if (!available) {
            invalidateAuthCache()
            return@withContext unavailableReason ?: "云端模式当前版本不可用"
        }
        val token = runCatching { manager.requireAccessToken() }.getOrNull()
        if (token.isNullOrBlank()) {
            invalidateAuthCache()
            return@withContext "请先登录 LA Studio 账号"
        }
        val now = System.currentTimeMillis()
        if (now - lastCloudAuthTime < AUTH_CACHE_TTL_MS && lastCloudAuthToken == token) {
            return@withContext null
        }
        configureCloud(context)?.let {
            invalidateAuthCache()
            return@withContext it
        }
        try {
            val result = JSONObject(CloudNative.cloudAuthorizeFeature())
            if (result.optBoolean("ok", false) &&
                result.optJSONObject("result")?.optBoolean("allowed", false) == true
            ) {
                lastCloudAuthTime = System.currentTimeMillis()
                lastCloudAuthToken = token
                null
            } else {
                invalidateAuthCache()
                val errObj = result.optJSONObject("error")
                val reasonCode = errObj?.optString("reason") ?: result.optString("reason")
                val msg = errObj?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: result.optString("message").takeIf { it.isNotBlank() }
                if (reasonCode == "membership_required" && (msg == null || msg.contains("停用") || msg.contains("不可用"))) {
                    "该功能需要开通会员"
                } else {
                    msg ?: "云端模式当前不可用"
                }
            }
        } catch (e: Exception) {
            "无法连接云端授权服务，请检查网络后重试"
        }
    }

    /**
     * @return null 表示授权通过; 否则返回阻断原因
     */
    suspend fun ensureAuthorized(context: Context): String? = withContext(Dispatchers.IO) {
        val manager = FeAccountManager.get(context)
        val (available, unavailableReason) = manager.isFeatureAvailable(FEATURE_KEY)
        if (!available) {
            invalidateAuthCache()
            return@withContext unavailableReason ?: "答案解析功能当前版本不可用"
        }
        val token = runCatching { manager.requireAccessToken() }.getOrNull()
        if (token.isNullOrBlank()) {
            invalidateAuthCache()
            return@withContext "请先登录 LA Studio 账号"
        }
        val now = System.currentTimeMillis()
        if (now - lastResolverAuthTime < AUTH_CACHE_TTL_MS && lastResolverAuthToken == token) {
            return@withContext null
        }
        configureResolver(context)?.let {
            invalidateAuthCache()
            return@withContext it
        }
        try {
            val json = ResolverNative.authorizeFeature()
            val obj = JSONObject(json)
            if (obj.optBoolean("allowed", false)) {
                lastResolverAuthTime = System.currentTimeMillis()
                lastResolverAuthToken = token
                null
            } else {
                invalidateAuthCache()
                val errObj = obj.optJSONObject("error")
                val reasonCode = errObj?.optString("reason") ?: obj.optString("reason")
                val msg = errObj?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: obj.optString("message").takeIf { it.isNotBlank() }
                if (reasonCode == "membership_required" && (msg == null || msg.contains("停用") || msg.contains("不可用"))) {
                    "该功能需要开通会员"
                } else {
                    msg ?: "该功能当前不可用"
                }
            }
        } catch (e: Exception) {
            "无法连接授权服务，请检查网络后重试"
        }
    }

    private suspend fun configureResolver(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val manager = FeAccountManager.get(context)
            if (manager.gateState.value is FeAccountGateState.Checking) {
                manager.checkGate()
            }
            AppIntegrityGuard.ensureResolver(context, BuildInfoProvider.get(context), enabled = manager.isIntegrityCheckEnabled())
            val token = manager.requireAccessToken()
            val rc = ResolverNative.setAuthConfig(
                baseUrl = BASE_URL,
                softwareKey = SOFTWARE_KEY,
                featureKey = FEATURE_KEY,
                publicKeyB64 = PUBLIC_KEY_B64,
                appVersionCode = BuildConfig.VERSION_CODE.toLong(),
            )
            if (rc != 0) {
                return@withContext "解析器内部授权配置失败 (code=$rc)"
            }
            ResolverNative.setAccessToken(token)
            ResolverNative.setAuthEnabled(true)
            null
        } catch (e: AppIntegrityGuard.IntegrityFailure) {
            e.message
        } catch (e: FeAccountNotAuthenticatedException) {
            "请先登录 LA Studio 账号"
        } catch (e: FeAccountApiException) {
            if (e.error.statusCode == 401) "登录已失效，请重新登录" else e.error.message
        } catch (e: Exception) {
            "无法连接授权服务，请检查网络后重试"
        }
    }

    private suspend fun configureCloud(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val manager = FeAccountManager.get(context)
            if (manager.gateState.value is FeAccountGateState.Checking) {
                manager.checkGate()
            }
            AppIntegrityGuard.ensureCloud(context, BuildInfoProvider.get(context), enabled = manager.isIntegrityCheckEnabled())
            val token = manager.requireAccessToken()
            val rc = CloudNative.setAuthConfig(
                baseUrl = BASE_URL,
                softwareKey = SOFTWARE_KEY,
                featureKey = "cloud_mode",
                publicKeyB64 = PUBLIC_KEY_B64,
                appVersionCode = BuildConfig.VERSION_CODE.toLong(),
            )
            if (rc != 0) return@withContext "云端库授权配置失败 (code=$rc)"
            CloudNative.setAccessToken(token)
            CloudNative.setAuthEnabled(true)
            null
        } catch (e: AppIntegrityGuard.IntegrityFailure) {
            e.message
        } catch (e: FeAccountNotAuthenticatedException) {
            "请先登录 LA Studio 账号"
        } catch (e: FeAccountApiException) {
            if (e.error.statusCode == 401) "登录已失效，请重新登录" else e.error.message
        } catch (e: Exception) {
            "无法连接云端授权服务，请检查网络后重试"
        }
    }
}

/** 答案解析功能被后端阻断时抛出。 */
internal class ResolverAuthorizationDeniedException(message: String) : Exception(message)

/** 尚未登录 (或登录状态已失效) 时抛出。 */
internal class FeAccountNotAuthenticatedException : Exception("请先登录 LA Studio 账号")
