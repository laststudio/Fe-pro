package com.shuaiqiu.fuckets100.core.common

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
)
