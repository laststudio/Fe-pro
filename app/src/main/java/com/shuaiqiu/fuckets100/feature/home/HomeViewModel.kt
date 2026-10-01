package com.shuaiqiu.fuckets100.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shuaiqiu.fuckets100.ActivationMode
import com.shuaiqiu.fuckets100.core.common.AppBuildInfo
import com.shuaiqiu.fuckets100.FeApplication
import com.shuaiqiu.fuckets100.LegalAgreementRepository
import com.shuaiqiu.fuckets100.UpdateStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val runtimeStatus: HomeRuntimeStatus,
    val remoteStatus: UpdateStatus? = null,
    val isRefreshing: Boolean = false,
    val appVersion: String,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val runtimeStatusRepository: HomeRuntimeStatusRepository,
    buildInfo: AppBuildInfo,
) : ViewModel() {
    private var lastMode: ActivationMode? = null
    private var initialized = false

    private val _uiState = MutableStateFlow(
        HomeUiState(
            runtimeStatus = HomeRuntimeStatus(
                hasFilesPerm = false,
                hasRootAvailable = false,
                hasDirectReadAvailable = false,
                cloudLoggedIn = false,
            ),
            remoteStatus = FeApplication.remoteStatus,
            appVersion = "v${buildInfo.versionName} (${buildInfo.versionCode})",
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            FeApplication.remoteStatusFlow.collect { status ->
                _uiState.update { it.copy(remoteStatus = status) }
            }
        }
    }

    fun onRuntimeInputsChanged(mode: ActivationMode, refreshRevision: Int) {
        val force = refreshRevision > 0 || lastMode != mode || !initialized
        lastMode = mode
        initialized = true
        viewModelScope.launch {
            val status = runtimeStatusRepository.load(force)
            _uiState.update { it.copy(runtimeStatus = status) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val status = runtimeStatusRepository.load(force = true)
                _uiState.update { it.copy(runtimeStatus = status) }
                FeApplication.syncRemoteStatus()
                LegalAgreementRepository.refresh()
                delay(350)
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun onResumed() {
        viewModelScope.launch {
            val status = runtimeStatusRepository.load(force = true)
            _uiState.update { it.copy(runtimeStatus = status) }
        }
    }
}
