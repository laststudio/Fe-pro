package com.shuaiqiu.fuckets100

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

/**
 * 搴旂敤涓婚鍖呰鍣? */
@Composable
fun FeTheme(content: @Composable () -> Unit) {
    val theme = ThemeManager.getSavedTheme()
    val systemDarkMode = isSystemInDarkTheme()
    val effectiveDarkMode = if (ThemeManager.getSavedAutoDarkMode()) {
        systemDarkMode
    } else {
        ThemeManager.getSavedDarkMode()
    }
    val useDynamicColor = ThemeManager.getSavedDynamicColor()

    FeThemeWrapper(
        theme = theme,
        isDarkMode = effectiveDarkMode,
        useDynamicColor = useDynamicColor,
        content = content
    )
}
