package com.shuaiqiu.fuckets100

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 个人中心数据接口 (SoftwareAccess 鉴权)。
 *
 * 独立于 [FeAccountApi], 避免与既有改动耦合。调用前通过
 * [FeAccountManager.requireAccessToken] 获取有效 access token。
 */
class FeAccountProfileApi {

    data class AccountBindings(
        val email: EmailBinding?,
        val etingshuo: EtingshuoBinding?,
    ) {
        data class EmailBinding(val email: String, val isVerified: Boolean)
        data class EtingshuoBinding(val account: String)
    }

    suspend fun bindings(token: String): AccountBindings {
        val json = JSONObject(request("GET", "/client-auth/account/bindings", token))
        val email = json.optJSONObject("email")?.let {
            AccountBindings.EmailBinding(
                email = it.optString("email", ""),
                isVerified = it.optBoolean("is_verified", false),
            )
        }
        val etingshuo = json.optJSONObject("etingshuo")?.let {
            AccountBindings.EtingshuoBinding(account = it.optString("account", ""))
        }
        return AccountBindings(email, etingshuo)
    }

    suspend fun changePassword(token: String, old: String, new: String, new2: String) {
        request(
            "POST",
            "/client-auth/account/change-password",
            token,
            body = JSONObject()
                .put("old_passwd", old)
                .put("new_passwd", new)
                .put("new_passwd2", new2)
                .toString(),
        )
    }

    suspend fun devices(token: String): List<JSONObject> =
        requestArray("/client-auth/account/devices", token)

    suspend fun sessions(token: String): List<JSONObject> =
        requestArray("/client-auth/account/sessions", token)

    suspend fun revokeSession(token: String, rowId: Int) {
        request("DELETE", "/client-auth/account/sessions/$rowId", token)
    }

    suspend fun revokeDevice(token: String, rowId: Int) {
        request("DELETE", "/client-auth/account/devices/$rowId", token)
    }

    suspend fun unbindPlatformAccount(token: String) {
        request("DELETE", "/client-auth/platform-account", token)
    }

    private suspend fun requestArray(path: String, token: String): List<JSONObject> {
        val array = JSONArray(request("GET", path, token))
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    private suspend fun request(
        method: String,
        path: String,
        token: String,
        body: String? = null,
    ): String = suspendCancellableCoroutine { continuation ->
        val connection = (URL(FeAccountApi.BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "SoftwareAccess $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
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
                    Result.success(responseBody)
                }
            } catch (e: FeAccountApiException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(FeAccountNetworkException(e.message ?: "无法连接 LA Studio 服务"))
            } finally {
                connection.disconnect()
            }
            continuation.resumeWith(result)
        }
    }

    private companion object {
        const val TIMEOUT_MS = 8_000
    }
}
