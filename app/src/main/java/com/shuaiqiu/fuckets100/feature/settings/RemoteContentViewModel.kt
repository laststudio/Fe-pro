package com.shuaiqiu.fuckets100.feature.settings

import androidx.lifecycle.ViewModel
import com.shuaiqiu.fuckets100.RemoteConfigManager
import com.shuaiqiu.fuckets100.UpdateStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RemoteContentViewModel @Inject constructor(
    private val remoteConfigManager: RemoteConfigManager,
) : ViewModel() {
    suspend fun checkStatus(): UpdateStatus = remoteConfigManager.checkStatus()
}
