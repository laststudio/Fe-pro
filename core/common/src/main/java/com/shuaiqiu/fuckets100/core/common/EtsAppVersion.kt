package com.shuaiqiu.fuckets100.core.common

import android.content.Context

/** Information about the installed E 听说 client. */
data class EtsAppVersion(
    val versionName: String,
) {
    val majorVersion: Int? = parseMajorVersion(versionName)
    val isEightSeries: Boolean = majorVersion == EIGHT_SERIES_MAJOR

    companion object {
        const val PACKAGE_NAME = "com.ets100.secondary"
        private const val EIGHT_SERIES_MAJOR = 8

        fun parseMajorVersion(versionName: String): Int? =
            Regex("^\\s*(\\d+)").find(versionName)?.groupValues?.get(1)?.toIntOrNull()

        fun read(context: Context): EtsAppVersion? = try {
            val versionName = context.packageManager
                .getPackageInfo(PACKAGE_NAME, 0)
                .versionName
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: return null
            EtsAppVersion(versionName)
        } catch (_: Exception) {
            null
        }
    }
}
