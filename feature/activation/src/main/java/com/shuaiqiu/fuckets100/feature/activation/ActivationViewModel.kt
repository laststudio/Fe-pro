package com.shuaiqiu.fuckets100.feature.activation

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shuaiqiu.fuckets100.ETS100AuthManager
import com.shuaiqiu.fuckets100.PermissionsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ActivationPageSnapshot(
    val hasFilesPermission: Boolean,
    val cloudLoggedIn: Boolean,
    val cloudPhone: String?,
    val etsAppInfo: Pair<Boolean, String>?,
)

data class ActivationUiState(
    val snapshot: ActivationPageSnapshot? = null,
)

@HiltViewModel
class ActivationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private var cached: ActivationPageSnapshot? = null
    private var updatedAt = 0L

    private val _uiState = MutableStateFlow(ActivationUiState(snapshot = cached))
    val uiState: StateFlow<ActivationUiState> = _uiState.asStateFlow()

    fun refresh(force: Boolean) {
        val current = cached
        val fresh = current != null && SystemClock.elapsedRealtime() - updatedAt < CACHE_TTL_MS
        if (!force && fresh) {
            _uiState.update { it.copy(snapshot = current) }
            return
        }
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                ActivationPageSnapshot(
                    hasFilesPermission = PermissionsHelper.hasAllFilesAccess(),
                    cloudLoggedIn = ETS100AuthManager.isLoggedIn(context),
                    cloudPhone = ETS100AuthManager.getPhone(context),
                    etsAppInfo = loadEtsAppInfo(context),
                )
            }
            cached = snapshot
            updatedAt = SystemClock.elapsedRealtime()
            _uiState.update { it.copy(snapshot = snapshot) }
        }
    }

    private fun loadEtsAppInfo(
        context: Context,
        packageName: String = "com.ets100.secondary",
    ): Pair<Boolean, String>? = try {
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        Pair(true, packageInfo.versionName ?: "未知")
    } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
        Pair(false, "")
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val CACHE_TTL_MS = 15_000L
    }
}
