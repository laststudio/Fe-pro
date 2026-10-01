package com.shuaiqiu.fuckets100

import android.content.Context
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import com.shuaiqiu.fuckets100.core.common.AppBuildInfo
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class FeAccountApi(
    private val context: Context,
    private val buildInfo: AppBuildInfo,
) {
    private val activeConnections = java.util.Collections.synchronizedSet(
        mutableSetOf<HttpURLConnection>(),
    )
    data class LoginTokens(val accessToken: String, val refreshToken: String)
    data class RefreshTokens(val accessToken: String, val refreshToken: String)
    data class CloudSession(
        val accountDisplay: String,
        val token: String,
        val deviceCode: String,
        val parentAccountId: String,
        val selectedAccount: JSONObject,
        val validAccounts: List<JSONObject>,
    )
    data class SoftwarePolicy(
        val killSwitch: Boolean,
        val latestVersionCode: Int,
        val forceUpdate: Boolean,
        val updateUrl: String,
        val message: String,
        val integrityCheckEnabled: Boolean = true,
        val features: Map<String, Boolean> = emptyMap(),
    )

    suspend fun softwarePolicy(): SoftwarePolicy {
        val response = request("GET", "/software/$SOFTWARE_KEY/config?version_code=${buildInfo.versionCode}")
        val json = JSONObject(response.body)
        val featuresJson = json.optJSONObject("features")
        val features = mutableMapOf<String, Boolean>()
        if (featuresJson != null) {
            val keys = featuresJson.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                features[k] = featuresJson.optBoolean(k, true)
            }
        }
        return SoftwarePolicy(
            killSwitch = json.optBoolean("isKillSwitchOn", false),
            latestVersionCode = json.optInt("latestVersionCode", buildInfo.versionCode),
            forceUpdate = json.optBoolean("isForce", false),
            updateUrl = json.optString("updateUrl", ""),
            message = if (json.optBoolean("isKillSwitchOn", false)) {
                json.optString("noticeMessage", "").ifBlank { "Fe 当前已暂停使用" }
            } else {
                json.optString("updateMessage", "").ifBlank { "当前版本不可用" }
            },
            integrityCheckEnabled = json.optBoolean("integrityCheckEnabled", true),
            features = features,
        )
    }

    suspend fun login(username: String, password: String, capToken: String?): LoginTokens {
        val device = JSONObject()
            .put("installation_id", installationId())
            .put("platform_device_id", platformDeviceId())
            .put("application_id", buildInfo.applicationId)
            .put("signing_certificate_sha256", signingCertificateSha256())
            .put("version_code", buildInfo.versionCode)
            .put("device_name", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
            .put("platform_version", "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        val body = JSONObject()
            .put("client_key", CLIENT_KEY)
            .put("username", username.trim().lowercase())
            .put("password", password)
            .put("device", device)
        if (!capToken.isNullOrBlank()) body.put("cap_token", capToken)
        val response = request("POST", "/client-auth/login", body = body.toString())
        val json = JSONObject(response.body)
        return LoginTokens(json.getString("access_token"), json.getString("refresh_token"))
    }

    suspend fun refresh(refreshToken: String): RefreshTokens {
        val response = request(
            "POST",
            "/client-auth/refresh",
            authorization = "SoftwareRefresh $refreshToken",
        )
        val json = JSONObject(response.body)
        return RefreshTokens(json.getString("access_token"), json.getString("refresh_token"))
    }

    suspend fun session(accessToken: String): FeAccountSession = FeAuthResponseParser.session(
        request("GET", "/client-auth/session", "SoftwareAccess $accessToken").body,
    )

    suspend fun logout(accessToken: String) {
        request("POST", "/client-auth/logout", "SoftwareAccess $accessToken")
    }

    suspend fun cloudDeviceCode(accessToken: String): String {
        val response = request(
            "GET",
            "/client-auth/cloud/device-code",
            "SoftwareAccess $accessToken",
        )
        return JSONObject(response.body).getString("device_code")
    }

    suspend fun cloudSession(accessToken: String, preferredAccountId: String): CloudSession {
        val response = request(
            "POST",
            "/client-auth/cloud/session",
            "SoftwareAccess $accessToken",
            JSONObject().put("preferred_account_id", preferredAccountId).toString(),
        )
        return parseCloudSession(JSONObject(response.body))
    }

    suspend fun bindChangyanAccount(
        accessToken: String,
        login: String,
        userId: String,
        sucUserToken: String,
        preferredAccountId: String,
    ): CloudSession {
        val response = request(
            "POST",
            "/client-auth/platform-account/changyan",
            "SoftwareAccess $accessToken",
            JSONObject()
                .put("login", login)
                .put("user_id", userId)
                .put("suc_user_token", sucUserToken)
                .put("preferred_account_id", preferredAccountId)
                .toString(),
        )
        return parseCloudSession(JSONObject(response.body))
    }

    private fun parseCloudSession(json: JSONObject): CloudSession {
        val accounts = json.optJSONArray("valid_accounts")
        return CloudSession(
            accountDisplay = json.optString("account_display"),
            token = json.getString("token"),
            deviceCode = json.getString("device_code"),
            parentAccountId = json.getString("parent_account_id"),
            selectedAccount = json.getJSONObject("selected_account"),
            validAccounts = if (accounts == null) emptyList() else {
                (0 until accounts.length()).map { accounts.getJSONObject(it) }
            },
        )
    }

    suspend fun bindEts100Account(
        accessToken: String,
        phone: String,
        password: String,
        deviceCode: String,
    ) {
        val body = JSONObject()
            .put("provider", "ets100-phone")
            .put("account", phone)
            .put("password", password)
            .put("platform_device_code", deviceCode)
        request(
            "POST",
            "/client-auth/platform-account/bind",
            authorization = "SoftwareAccess $accessToken",
            body = body.toString(),
        )
    }

    suspend fun fetchPublicFeatures(versionCode: Int = buildInfo.versionCode): List<FeFeatureConfig> {
        val response = request("GET", "/software/$SOFTWARE_KEY/features?version_code=$versionCode")
        val array = org.json.JSONArray(response.body)
        return (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            val minLevel = item.optInt("min_member_level", 0)
            val minTier = item.optString("min_member_tier").takeIf { it.isNotBlank() } ?: when (minLevel) {
                2 -> "ultra"
                1 -> "pro"
                else -> "free"
            }
            FeFeatureConfig(
                featureKey = item.getString("feature_key"),
                name = item.getString("name"),
                description = item.optString("description", ""),
                status = item.optString("status", "enabled"),
                availableForVersion = item.optBoolean("available_for_version", true),
                reason = item.optString("reason").takeIf { it.isNotBlank() },
                requiresMembership = item.optBoolean("requires_membership", false),
                minMemberLevel = minLevel,
                minMemberTier = minTier,
            )
        }
    }

    suspend fun fetchClientFeatures(accessToken: String, versionCode: Int = buildInfo.versionCode): List<FeClientFeatureConfig> {
        val response = request("GET", "/client-auth/features?version_code=$versionCode", "SoftwareAccess $accessToken")
        val array = org.json.JSONArray(response.body)
        return (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            val minLevel = item.optInt("min_member_level", 0)
            val minTier = item.optString("min_member_tier").takeIf { it.isNotBlank() } ?: when (minLevel) {
                2 -> "ultra"
                1 -> "pro"
                else -> "free"
            }
            FeClientFeatureConfig(
                featureKey = item.getString("feature_key"),
                name = item.getString("name"),
                description = item.optString("description", ""),
                status = item.optString("status", "enabled"),
                authorized = item.optBoolean("authorized", true),
                reason = item.optString("reason").takeIf { it.isNotBlank() },
                requiresMembership = item.optBoolean("requires_membership", false),
                minMemberLevel = minLevel,
                minMemberTier = minTier,
            )
        }
    }

    fun cancelActiveRequests() {
        synchronized(activeConnections) {
            activeConnections.toList().forEach(HttpURLConnection::disconnect)
        }
    }

    private data class Response(val body: String)

    private suspend fun request(
        method: String,
        path: String,
        authorization: String? = null,
        body: String? = null,
    ): Response = suspendCancellableCoroutine { continuation ->
        val connection = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Fe/${buildInfo.versionName} Android")
            setRequestProperty("X-App-Version-Code", buildInfo.versionCode.toString())
            if (authorization != null) setRequestProperty("Authorization", authorization)
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        activeConnections += connection
        continuation.invokeOnCancellation { connection.disconnect() }
        Dispatchers.IO.asExecutor().execute {
            val result = try {
                if (body != null) connection.outputStream.bufferedWriter().use { it.write(body) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (status !in 200..299) {
                    Result.failure(FeAccountApiException(FeAuthResponseParser.error(status, responseBody)))
                } else {
                    Result.success(Response(responseBody))
                }
            } catch (e: FeAccountApiException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(FeAccountNetworkException(e.message ?: "无法连接 LA Studio 服务"))
            } finally {
                activeConnections -= connection
                connection.disconnect()
            }
            continuation.resumeWith(result)
        }
    }

    private fun installationId(): String {
        val prefs = context.getSharedPreferences(DEVICE_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(INSTALLATION_ID, null) ?: java.util.UUID.randomUUID().toString().also {
            check(prefs.edit().putString(INSTALLATION_ID, it).commit())
        }
    }

    private fun platformDeviceId(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            .orEmpty()
        return sha256LowerHex("${buildInfo.applicationId}:$androidId")
    }

    @Suppress("DEPRECATION")
    private fun signingCertificateSha256(): String {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES,
            )
        } else {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNATURES,
            )
        }
        val bytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
            packageInfo.signatures?.firstOrNull()?.toByteArray()
        } ?: error("无法读取应用签名")
        return sha256LowerHex(bytes)
    }

    companion object {
        const val BASE_URL = "https://api.lastudio.cc"
        const val SOFTWARE_KEY = "fe"
        const val CLIENT_KEY = "fe-android"
        const val DEVICE_PREFS = "fe_account_device"
        private const val INSTALLATION_ID = "installation_id"
        private const val TIMEOUT_MS = 8_000
    }
}

class FeAccountApiException(val error: FeApiError) : Exception(error.message)
class FeAccountNetworkException(message: String) : Exception(message)
