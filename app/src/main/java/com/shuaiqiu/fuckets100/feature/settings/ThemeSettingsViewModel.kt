package com.shuaiqiu.fuckets100.feature.settings

import androidx.lifecycle.ViewModel
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.shuaiqiu.fuckets100.PredictiveBackAnimation
import com.shuaiqiu.fuckets100.PredictiveBackExitDirection
import com.shuaiqiu.fuckets100.SettingsManager
import com.shuaiqiu.fuckets100.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ThemeSettingsUiState(
    val isDarkMode: Boolean,
    val isAutoDarkMode: Boolean,
    val useDynamicColor: Boolean,
    val keyColor: Int,
    val paletteStyle: PaletteStyle,
    val colorSpec: ColorSpec.SpecVersion,
    val isAmoled: Boolean,
    val predictiveBackAnimation: PredictiveBackAnimation,
    val predictiveBackExitDirection: PredictiveBackExitDirection,
)

@HiltViewModel
class ThemeSettingsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(loadState())
    val uiState: StateFlow<ThemeSettingsUiState> = _uiState.asStateFlow()

    fun setDisplayMode(autoDark: Boolean, dark: Boolean, amoled: Boolean) {
        ThemeManager.saveAutoDarkMode(autoDark)
        ThemeManager.saveDarkMode(dark)
        ThemeManager.saveAmoled(amoled)
        _uiState.update {
            it.copy(isAutoDarkMode = autoDark, isDarkMode = dark, isAmoled = amoled)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        ThemeManager.saveDynamicColor(enabled)
        _uiState.update { it.copy(useDynamicColor = enabled) }
    }

    fun setKeyColor(color: Int) {
        ThemeManager.saveKeyColor(color)
        ThemeManager.saveDynamicColor(false)
        _uiState.update { it.copy(keyColor = color, useDynamicColor = false) }
    }

    fun setPaletteStyle(value: PaletteStyle) {
        ThemeManager.savePaletteStyle(value)
        _uiState.update { it.copy(paletteStyle = value) }
    }

    fun setColorSpec(value: ColorSpec.SpecVersion) {
        ThemeManager.saveColorSpec(value)
        _uiState.update { it.copy(colorSpec = value) }
    }

    fun setPredictiveBackAnimation(value: PredictiveBackAnimation) {
        SettingsManager.savePredictiveBackAnimation(value)
        _uiState.update { it.copy(predictiveBackAnimation = value) }
    }

    fun setPredictiveBackExitDirection(value: PredictiveBackExitDirection) {
        SettingsManager.savePredictiveBackExitDirection(value)
        _uiState.update { it.copy(predictiveBackExitDirection = value) }
    }

    private companion object {
        fun loadState(): ThemeSettingsUiState = ThemeSettingsUiState(
            isDarkMode = ThemeManager.getSavedDarkMode(),
            isAutoDarkMode = ThemeManager.getSavedAutoDarkMode(),
            useDynamicColor = ThemeManager.getSavedDynamicColor(),
            keyColor = ThemeManager.getSavedKeyColor(),
            paletteStyle = ThemeManager.getSavedPaletteStyle(),
            colorSpec = ThemeManager.getSavedColorSpec(),
            isAmoled = ThemeManager.getSavedAmoled(),
            predictiveBackAnimation = SettingsManager.getPredictiveBackAnimation(),
            predictiveBackExitDirection = SettingsManager.getPredictiveBackExitDirection(),
        )
    }
}
