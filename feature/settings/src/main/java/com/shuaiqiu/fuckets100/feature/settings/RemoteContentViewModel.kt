package com.shuaiqiu.fuckets100.feature.settings

import androidx.lifecycle.ViewModel
import com.shuaiqiu.fuckets100.RemoteConfigManager
import com.shuaiqiu.fuckets100.UpdateStatus
import com.shuaiqiu.fuckets100.core.common.AppBuildInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RemoteContentViewModel @Inject constructor(
    private val remoteConfigManager: RemoteConfigManager,
    buildInfo: AppBuildInfo,
) : ViewModel() {
    val appVersionName: String = buildInfo.versionName

    suspend fun checkStatus(): UpdateStatus = remoteConfigManager.checkStatus()
}
