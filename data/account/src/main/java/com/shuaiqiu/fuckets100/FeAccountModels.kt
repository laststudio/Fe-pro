package com.shuaiqiu.fuckets100

import org.json.JSONObject

data class FeAccountUser(
    val id: Long,
    val username: String,
    val memberLevel: Int,
    val tier: String = when (memberLevel) {
        2 -> "ultra"
        1 -> "pro"
        else -> "free"
    },
    val avatarUrl: String? = null,
) {
    val tierDisplayName: String
        get() = when (tier.lowercase()) {
            "ultra" -> "Ultra 会员"
            "pro" -> "Pro 会员"
            else -> "免费版"
        }
}

data class FeAccountSession(
    val sessionId: String,
    val deviceId: String?,
    val user: FeAccountUser,
    val integrityCheckEnabled: Boolean = true,
)

sealed interface FeAccountGateState {
    data class Checking(val cachedSession: FeAccountSession? = null) : FeAccountGateState
    data object LoginRequired : FeAccountGateState
    data class Ready(val session: FeAccountSession) : FeAccountGateState
    data class Offline(
        val cachedSession: FeAccountSession?,
        val message: String = "已离线",
    ) : FeAccountGateState
    data class Blocked(
        val message: String,
        val retryable: Boolean,
        val updateUrl: String? = null,
        val cachedSession: FeAccountSession? = null,
    ) : FeAccountGateState
}

val FeAccountGateState.cachedSessionOrNull: FeAccountSession?
    get() = when (this) {
        is FeAccountGateState.Checking -> cachedSession
        FeAccountGateState.LoginRequired -> null
        is FeAccountGateState.Ready -> session
        is FeAccountGateState.Offline -> cachedSession
        is FeAccountGateState.Blocked -> cachedSession
    }

data class FeApiError(
    val statusCode: Int,
    val code: String?,
    val message: String,
    val captchaRequired: Boolean,
)

internal object FeAuthResponseParser {
    fun error(statusCode: Int, response: String): FeApiError {
        val fallback = when (statusCode) {
            401 -> "登录状态无效，请重新登录"
            403 -> "当前账号或客户端无权使用此软件"
            429 -> "请求过于频繁，请稍后重试"
            else -> "服务器请求失败（$statusCode）"
        }
        return runCatching {
            val detail = JSONObject(response).opt("detail")
            when (detail) {
                is JSONObject -> {
                    val message = detail.optString("message", fallback)
                    FeApiError(
                        statusCode = statusCode,
                        code = detail.optString("code").takeIf { it.isNotBlank() },
                        message = message,
                        captchaRequired = detail.optBoolean("captcha_required", false) ||
                            message.contains("人机验证"),
                    )
                }
                is String -> FeApiError(
                    statusCode,
                    null,
                    detail.ifBlank { fallback },
                    detail.contains("人机验证"),
                )
                null -> FeApiError(statusCode, null, fallback, false)
                else -> FeApiError(statusCode, null, fallback, false)
            }
        }.getOrElse { FeApiError(statusCode, null, fallback, false) }
    }

    fun session(response: String): FeAccountSession {
        val json = JSONObject(response)
        val user = json.getJSONObject("user")
        val level = user.optInt("member_level", 0)
        val tier = user.optString("tier").takeIf { it.isNotBlank() } ?: when (level) {
            2 -> "ultra"
            1 -> "pro"
            else -> "free"
        }
        return FeAccountSession(
            sessionId = json.getString("session_id"),
            deviceId = json.optString("device_id").takeIf { it.isNotBlank() },
            user = FeAccountUser(
                id = user.getLong("id"),
                username = user.getString("username"),
                memberLevel = level,
                tier = tier,
                avatarUrl = user.optString("avatar_url").takeIf { it.isNotBlank() },
            ),
            integrityCheckEnabled = json.optBoolean("integrity_check_enabled", true),
        )
    }
}

data class FeFeatureConfig(
    val featureKey: String,
    val name: String,
    val description: String = "",
    val status: String = "enabled",
    val availableForVersion: Boolean = true,
    val reason: String? = null,
    val requiresMembership: Boolean = false,
    val minMemberLevel: Int = 0,
    val minMemberTier: String = when (minMemberLevel) {
        2 -> "ultra"
        1 -> "pro"
        else -> "free"
    },
) {
    val requiredTierDisplayName: String
        get() = when (minMemberTier.lowercase()) {
            "ultra" -> "Ultra 会员"
            "pro" -> "Pro 会员"
            else -> "会员"
        }
}

data class FeClientFeatureConfig(
    val featureKey: String,
    val name: String,
    val description: String = "",
    val status: String = "enabled",
    val authorized: Boolean = true,
    val reason: String? = null,
    val requiresMembership: Boolean = false,
    val minMemberLevel: Int = 0,
    val minMemberTier: String = when (minMemberLevel) {
        2 -> "ultra"
        1 -> "pro"
        else -> "free"
    },
) {
    val requiredTierDisplayName: String
        get() = when (minMemberTier.lowercase()) {
            "ultra" -> "Ultra 会员"
            "pro" -> "Pro 会员"
            else -> "会员"
        }
}
