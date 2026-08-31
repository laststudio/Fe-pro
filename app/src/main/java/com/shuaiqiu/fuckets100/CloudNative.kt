package com.shuaiqiu.fuckets100

/** libcloud.so 的 JNI 绑定，仅包含云端模式，不包含答案解析器。 */
internal object CloudNative {
    init {
        System.loadLibrary("cloud")
    }

    external fun setAuthConfig(
        baseUrl: String,
        softwareKey: String,
        featureKey: String,
        publicKeyB64: String,
        appVersionCode: Long,
    ): Int

    external fun setAccessToken(token: String)
    external fun clearAccessToken()
    external fun setAuthEnabled(enabled: Boolean)

    external fun cloudAuthorizeFeature(): String

    external fun cloudLoginChangyan(
        login: String,
        userId: String,
        captchaResult: String,
        deviceCode: String,
        preferredAccountId: String,
    ): String

    external fun selfIntegrityCheck(): String
}
