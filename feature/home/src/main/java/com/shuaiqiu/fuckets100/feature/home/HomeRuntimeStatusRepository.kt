package com.shuaiqiu.fuckets100.feature.home

import android.content.Context
import android.os.SystemClock
import com.shuaiqiu.fuckets100.ETS100AuthManager
import com.shuaiqiu.fuckets100.PermissionsHelper
import com.shuaiqiu.fuckets100.RootManager
import com.shuaiqiu.fuckets100.ZWCHelper
import com.shuaiqiu.fuckets100.core.common.EtsAppVersion
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class HomeRuntimeStatusRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var cached: HomeRuntimeStatus? = null
    private var lastRefreshTime = 0L

    fun cachedOrNull(force: Boolean): HomeRuntimeStatus? {
        val value = cached ?: return null
        val fresh = SystemClock.elapsedRealtime() - lastRefreshTime < CACHE_TTL_MS
        return value.takeIf { !force && fresh }
    }

    suspend fun load(force: Boolean): HomeRuntimeStatus {
        cachedOrNull(force)?.let { return it }
        return withContext(Dispatchers.IO) {
            // force 表示用户主动要求刷新（下拉/回到前台），而手动在 Magisk 管理器里授权
            // 不会产生任何回调，必须绕过 RootManager 的进程级缓存，否则永远读到授权前的旧结果
            if (force) RootManager.clearCache()
            HomeRuntimeStatus(
                hasFilesPerm = PermissionsHelper.hasAllFilesAccess(),
                hasRootAvailable = RootManager.isRootAvailable(),
                hasDirectReadAvailable = ZWCHelper.isDirectReadAvailable(),
                cloudLoggedIn = ETS100AuthManager.isLoggedIn(context),
                etsAppInfo = loadEtsAppInfo(context),
            ).also(::update)
        }
    }

    fun update(status: HomeRuntimeStatus) {
        cached = status
        lastRefreshTime = SystemClock.elapsedRealtime()
    }

    private fun loadEtsAppInfo(context: Context): Pair<Boolean, String>? = try {
        val packageInfo = context.packageManager.getPackageInfo(EtsAppVersion.PACKAGE_NAME, 0)
        true to (packageInfo.versionName?.trim()?.takeIf { it.isNotEmpty() } ?: "未知")
    } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
        false to ""
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val CACHE_TTL_MS = 30_000L
    }
}
