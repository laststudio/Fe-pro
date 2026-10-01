package com.shuaiqiu.fuckets100

import android.content.Context
import com.shuaiqiu.fuckets100.core.common.AppBuildInfo

/**
 * 平台代码无法直接读到 app 的 BuildConfig，这里集中构造构建信息。
 * 后续由 Hilt/provider 注入替代静态访问。
 */
object BuildInfoProvider {
    fun get(context: Context): AppBuildInfo = AppBuildInfo(
        applicationId = BuildConfig.APPLICATION_ID,
        versionCode = BuildConfig.VERSION_CODE,
        versionName = BuildConfig.VERSION_NAME,
        expectedPackageName = BuildConfig.EXPECTED_PACKAGE_NAME,
        expectedSignatureSha256 = BuildConfig.EXPECTED_SIGNATURE_SHA256,
    )
}
