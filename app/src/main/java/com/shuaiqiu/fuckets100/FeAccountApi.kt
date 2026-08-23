package com.shuaiqiu.fuckets100

import android.content.Context
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal class FeAccountApi(private val context: Context) {
    private val activeConnections = java.util.Collections.synchronizedSet(
        mutableSetOf<HttpURLConnection>(),
    )
    data class LoginTokens(val accessToken: String, val refreshToken: String)
    data class RefreshTokens(val accessToken: String, val refreshToken: String)
    data class SoftwarePolicy(
        val killSwitch: Boolean,
        val latestVersionCode: Int,
        val forceUpdate: Boolean,
        val updateUrl: String,
        val message: String,
    )

    suspend fun softwarePolicy(): SoftwarePolicy {
        val response = request("GET", "/software/$SOFTWARE_KEY/config")
        val json = JSONObject(response.body)
        return SoftwarePolicy(
            killSwitch = json.optBoolean("isKillSwitchOn", false),
            latestVersionCode = json.optInt("latestVersionCode", BuildConfig.VERSION_CODE),
            forceUpdate = json.optBoolean("isForce", false),
            updateUrl = json.optString("updateUrl", ""),
            message = if (json.optBoolean("isKillSwitchOn", false)) {
                json.optString("noticeMessage", "").ifBlank { "Fe 当前已暂停使用" }
            } else {
                json.optString("updateMessage", "").ifBlank { "当前版本不可用" }
            },
        )
    }

    suspend fun login(username: String, password: String, capToken: String?): LoginTokens {
        val device = JSONObject()
            .put("installation_id", installationId())
            .put("platform_device_id", platformDeviceId())
            .put("application_id", BuildConfig.APPLICATION_ID)
            .put("signing_certificate_sha256", signingCertificateSha256())
            .put("version_code", BuildConfig.VERSION_CODE)
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
            setRequestProperty("User-Agent", "Fe/${BuildConfig.VERSION_NAME} Android")
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
        return sha256LowerHex("${BuildConfig.APPLICATION_ID}:$androidId")
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

internal class FeAccountApiException(val error: FeApiError) : Exception(error.message)
internal class FeAccountNetworkException(message: String) : Exception(message)
