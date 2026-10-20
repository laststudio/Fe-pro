package com.shuaiqiu.fuckets100

/** 令牌或完整性策略变化后，不得继续复用之前的功能授权结果。 */
internal data class FeatureAuthorizationCache(
    val token: String,
    val checkedAtMillis: Long,
    val integrityEnabled: Boolean,
) {
    fun isValid(currentToken: String, nowMillis: Long, currentIntegrityEnabled: Boolean, ttlMillis: Long): Boolean =
        token == currentToken &&
            integrityEnabled == currentIntegrityEnabled &&
            nowMillis - checkedAtMillis in 0 until ttlMillis
}
