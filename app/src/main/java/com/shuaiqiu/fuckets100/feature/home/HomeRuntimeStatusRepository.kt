package com.shuaiqiu.fuckets100.feature.home

import android.content.Context
import android.os.SystemClock
import com.shuaiqiu.fuckets100.ETS100AuthManager
import com.shuaiqiu.fuckets100.PermissionsHelper
import com.shuaiqiu.fuckets100.RootManager
import com.shuaiqiu.fuckets100.ZWCHelper
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
            HomeRuntimeStatus(
                hasFilesPerm = PermissionsHelper.hasAllFilesAccess(),
                hasRootAvailable = RootManager.isRootAvailable(),
                hasDirectReadAvailable = ZWCHelper.isDirectReadAvailable(),
                cloudLoggedIn = ETS100AuthManager.isLoggedIn(context),
            ).also(::update)
        }
    }

    fun update(status: HomeRuntimeStatus) {
        cached = status
        lastRefreshTime = SystemClock.elapsedRealtime()
    }

    private companion object {
        const val CACHE_TTL_MS = 30_000L
    }
}
