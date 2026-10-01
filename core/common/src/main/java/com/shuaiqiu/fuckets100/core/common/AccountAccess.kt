package com.shuaiqiu.fuckets100.core.common

/**
 * 账号能力的最小契约。
 *
 * 其它数据模块（如 ETS100 授权）通过该接口读取账号状态与 access token，
 * 不再直接依赖账号模块的具体实现，从而消除模块间双向依赖。
 */
interface AccountAccess {
    /** 账号门禁是否正在校验中。 */
    val isGateChecking: Boolean

    /** 触发一次账号门禁校验。 */
    suspend fun checkGate()

    /**
     * 获取当前有效 access token。
     * @throws AccountAccessException 未登录 / 登录失效 / 网络失败时抛出。
     */
    suspend fun requireAccessToken(): String

    /** 功能开关查询：返回 (是否可用, 不可用原因)。 */
    fun isFeatureAvailable(featureKey: String): Pair<Boolean, String?>

    /** 服务端是否启用了软件完整性校验。 */
    fun isIntegrityCheckEnabled(): Boolean
}

/**
 * 账号访问失败的统一异常，跨模块传递时不暴露账号模块内部异常类型。
 */
class AccountAccessException(
    val statusCode: Int?,
    val notAuthenticated: Boolean,
    message: String,
) : Exception(message)

/**
 * 凭据变化时通知其它模块清理授权缓存的回调。
 */
interface AuthCacheInvalidator {
    fun invalidateAuthCache()
}
