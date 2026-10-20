package com.shuaiqiu.fuckets100.feature.settings

import androidx.lifecycle.ViewModel
import com.shuaiqiu.fuckets100.RemoteConfigManager
import com.shuaiqiu.fuckets100.RemoteContentRepository
import com.shuaiqiu.fuckets100.UpdateStatus
import com.shuaiqiu.fuckets100.core.common.AppBuildInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RemoteContentViewModel @Inject constructor(
    private val remoteConfigManager: RemoteConfigManager,
    private val remoteContentRepository: RemoteContentRepository,
    buildInfo: AppBuildInfo,
) : ViewModel() {
    val appVersionName: String = buildInfo.versionName

    /** 远程公告文本读取（带本地缓存），供界面层调用。 */
    suspend fun fetchTextOrNull(url: String, forceRefresh: Boolean = false): String? =
        remoteContentRepository.fetchTextOrNull(url, forceRefresh)

    suspend fun checkStatus(): UpdateStatus = remoteConfigManager.checkStatus()
}
