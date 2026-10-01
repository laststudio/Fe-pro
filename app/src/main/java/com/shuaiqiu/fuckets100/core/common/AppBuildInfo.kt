package com.shuaiqiu.fuckets100.core.common

import com.shuaiqiu.fuckets100.BuildConfig

/**
 * Immutable build metadata that can cross module boundaries without exposing
 * the application module's generated BuildConfig class to library code.
 */
data class AppBuildInfo(
    val applicationId: String,
    val versionCode: Int,
    val versionName: String,
    val expectedPackageName: String,
    val expectedSignatureSha256: String,
) {
    companion object {
        fun fromBuildConfig(): AppBuildInfo = AppBuildInfo(
            applicationId = BuildConfig.APPLICATION_ID,
            versionCode = BuildConfig.VERSION_CODE,
            versionName = BuildConfig.VERSION_NAME,
            expectedPackageName = BuildConfig.EXPECTED_PACKAGE_NAME,
            expectedSignatureSha256 = BuildConfig.EXPECTED_SIGNATURE_SHA256,
        )
    }
}
