package com.shuaiqiu.fuckets100

import com.shuaiqiu.fuckets100.core.common.AccountAccess
import com.shuaiqiu.fuckets100.core.common.AccountAccessException

/**
 * 把账号实现适配为跨模块契约，避免其它数据模块依赖 FeAccountManager。
 */
internal class FeAccountAccess(private val manager: FeAccountManager) : AccountAccess {
    override val isGateChecking: Boolean
        get() = manager.gateState.value is FeAccountGateState.Checking

    override suspend fun checkGate() = manager.checkGate()

    override suspend fun requireAccessToken(): String = try {
        manager.requireAccessToken()
    } catch (e: FeAccountNotAuthenticatedException) {
        throw AccountAccessException(
            statusCode = 401,
            notAuthenticated = true,
            message = "请先登录 LastStudio 账号",
        )
    } catch (e: FeAccountApiException) {
        throw AccountAccessException(
            statusCode = e.error.statusCode,
            notAuthenticated = e.error.statusCode == 401,
            message = if (e.error.statusCode == 401) "登录已失效，请重新登录" else e.error.message,
        )
    } catch (e: FeAccountNetworkException) {
        throw AccountAccessException(
            statusCode = null,
            notAuthenticated = false,
            message = e.message ?: "LastStudio 尚未连接",
        )
    }

    override fun isFeatureAvailable(featureKey: String): Pair<Boolean, String?> =
        manager.isFeatureAvailable(featureKey)

    override fun isIntegrityCheckEnabled(): Boolean = manager.isIntegrityCheckEnabled()
}
