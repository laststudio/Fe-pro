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

    /**
     * 内部授权门控。
     * @throws ResolverAuthorizationDeniedException 未登录/功能关闭/网络失败等
     */
    suspend fun requireAuthorized(context: Context) {
        val reason = ensureAuthorized(context)
        if (reason != null) throw ResolverAuthorizationDeniedException(reason)
    }

    /**
     * @return null 表示授权通过; 否则返回阻断原因
     */
    suspend fun ensureAuthorized(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val token = FeAccountManager.get(context).requireAccessToken()
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

            val json = ResolverNative.authorizeFeature()
            val obj = JSONObject(json)
            if (obj.optBoolean("allowed", false)) {
                null
            } else {
                obj.optJSONObject("error")?.optString("message")
                    ?: obj.optString("message")
                    ?: "该功能当前不可用"
            }
        } catch (e: FeAccountNotAuthenticatedException) {
            "请先登录 LA Studio 账号"
        } catch (e: FeAccountApiException) {
            if (e.error.statusCode == 401) "登录已失效，请重新登录" else e.error.message
        } catch (e: Exception) {
            "无法连接授权服务，请检查网络后重试"
        }
    }
}

/** 答案解析功能被后端阻断时抛出。 */
internal class ResolverAuthorizationDeniedException(message: String) : Exception(message)

/** 尚未登录 (或登录状态已失效) 时抛出。 */
internal class FeAccountNotAuthenticatedException : Exception("请先登录 LA Studio 账号")
