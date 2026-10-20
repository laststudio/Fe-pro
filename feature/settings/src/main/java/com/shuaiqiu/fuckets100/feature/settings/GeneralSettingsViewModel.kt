package com.shuaiqiu.fuckets100.feature.settings

import androidx.lifecycle.ViewModel
import com.shuaiqiu.fuckets100.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GeneralSettingsUiState(
    val compactAnswerDisplay: Boolean,
    val skipCloudReadWarning: Boolean,
)

@HiltViewModel
class GeneralSettingsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(
        GeneralSettingsUiState(
            compactAnswerDisplay = SettingsManager.getCompactAnswerDisplay(),
            skipCloudReadWarning = SettingsManager.getSkipCloudReadWarning(),
        ),
    )
    val uiState: StateFlow<GeneralSettingsUiState> = _uiState.asStateFlow()

    fun setCompactAnswerDisplay(enabled: Boolean) {
        SettingsManager.saveCompactAnswerDisplay(enabled)
        _uiState.update { it.copy(compactAnswerDisplay = enabled) }
    }

    fun setSkipCloudReadWarning(enabled: Boolean) {
        SettingsManager.saveSkipCloudReadWarning(enabled)
        _uiState.update { it.copy(skipCloudReadWarning = enabled) }
    }
}
