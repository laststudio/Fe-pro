package com.shuaiqiu.fuckets100

/**
 * libresolver.so 的 JNI 绑定。
 *
 * 对应 Rust 侧 `src/jni_bridge.rs` 的 `Java_com_shuaiqiu_fuckets100_ResolverNative_*` 导出。
 * 普通 C FFI 同样保留 (Python/桌面联调用)。
 */
internal object ResolverNative {

    init {
        System.loadLibrary("resolver")
    }

    /**
     * 配置内部授权 (base_url / software_key / feature_key / 服务器公钥 / version_code)。
     * @return 0 成功, 非 0 参数错误
     */
    external fun setAuthConfig(
        baseUrl: String,
        softwareKey: String,
        featureKey: String,
        publicKeyB64: String,
        appVersionCode: Long,
    ): Int

    /** 设置当前访问令牌 (仅存内存)。 */
    external fun setAccessToken(token: String)

    /** 清除访问令牌。 */
    external fun clearAccessToken()

    /** 启用/关闭内部授权。 */
    external fun setAuthEnabled(enabled: Boolean)

    /** 查询内部授权是否启用。 */
    external fun isAuthEnabled(): Boolean

    /**
     * 仅执行一次内部授权。
     * @return 授权 JSON (含 allowed/grant/decision_id) 或错误 JSON。
     */
    external fun authorizeFeature(): String

    /** 授权后单任务解析: content/info/content2 为 JSON 字符串。返回包裹结果 JSON。 */
    external fun parseTaskAuthorized(content: String, info: String, content2: String): String

    /** 授权后批量解析: tasks 为 JSON 数组字符串。返回包裹结果 JSON。 */
    external fun parseBatchAuthorized(tasksJson: String): String

    /** cloud_mode 授权后用畅言一次性断言换取 ETS100 token，并返回 ecard 选择结果。 */
    external fun cloudLoginChangyan(
        login: String,
        userId: String,
        captchaResult: String,
        deviceCode: String,
        preferredAccountId: String,
    ): String

    /** 仅验证 cloud_mode 功能，返回统一 JSON。 */
    external fun cloudAuthorizeFeature(): String

    /** so 自身完整性自校验（反调试 TracerPid + 内存段完整性）。返回 JSON。 */
    external fun selfIntegrityCheck(): String
}
