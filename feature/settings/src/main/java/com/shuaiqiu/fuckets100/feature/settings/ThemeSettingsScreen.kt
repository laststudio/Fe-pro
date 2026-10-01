package com.shuaiqiu.fuckets100.feature.settings

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveDialog
import com.shuaiqiu.fuckets100.label
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveScaffold
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveSegmentedDropdownItem
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveSegmentedListItem
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveToggleButton
import com.shuaiqiu.fuckets100.core.designsystem.SegmentedColumn
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard
import com.shuaiqiu.fuckets100.core.designsystem.TopBarBackButton
import com.shuaiqiu.fuckets100.core.designsystem.expressiveTopAppBarColors

import com.shuaiqiu.fuckets100.PredictiveBackAnimation
import com.shuaiqiu.fuckets100.PredictiveBackExitDirection


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    onDarkModeChanged: ((Boolean) -> Unit)? = null,
    onAutoDarkModeChanged: ((Boolean) -> Unit)? = null,
    onDynamicColorChanged: ((Boolean) -> Unit)? = null,
    onKeyColorChanged: ((Int) -> Unit)? = null,
    onPaletteStyleChanged: ((PaletteStyle) -> Unit)? = null,
    onColorSpecChanged: ((ColorSpec.SpecVersion) -> Unit)? = null,
    onAmoledChanged: ((Boolean) -> Unit)? = null,
    onPredictiveBackChanged: ((PredictiveBackAnimation) -> Unit)? = null,
    onPredictiveBackExitDirectionChanged: ((PredictiveBackExitDirection) -> Unit)? = null,
    viewModel: ThemeSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDarkMode = uiState.isDarkMode
    val isAutoDarkMode = uiState.isAutoDarkMode
    val useDynamicColor = uiState.useDynamicColor
    val keyColor = uiState.keyColor
    val paletteStyle = uiState.paletteStyle
    val colorSpec = uiState.colorSpec
    val isAmoled = uiState.isAmoled
    val predictiveBackAnimation = uiState.predictiveBackAnimation
    val predictiveBackExitDirection = uiState.predictiveBackExitDirection
    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val dynamicColorActive = useDynamicColor && dynamicColorAvailable
    val systemDark = isSystemInDarkTheme()
    val displayMode = when {
        isAutoDarkMode -> ThemeDisplayMode.SYSTEM
        isDarkMode && isAmoled -> ThemeDisplayMode.AMOLED
        isDarkMode -> ThemeDisplayMode.DARK
        else -> ThemeDisplayMode.LIGHT
    }
    val previewDark = displayMode == ThemeDisplayMode.DARK ||
        displayMode == ThemeDisplayMode.AMOLED ||
        displayMode == ThemeDisplayMode.SYSTEM && systemDark
    val previewAmoled = isAmoled && previewDark
    val effectiveSpec = colorSpec.effectiveFor(paletteStyle)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val haptic = LocalHapticFeedback.current

    fun selectDisplayMode(mode: ThemeDisplayMode) {
        val autoDark = mode == ThemeDisplayMode.SYSTEM
        val dark = mode == ThemeDisplayMode.DARK || mode == ThemeDisplayMode.AMOLED
        val amoled = when (mode) {
            ThemeDisplayMode.SYSTEM -> isAmoled
            ThemeDisplayMode.AMOLED -> true
            else -> false
        }
        viewModel.setDisplayMode(autoDark = autoDark, dark = dark, amoled = amoled)
        onAutoDarkModeChanged?.invoke(autoDark)
        onDarkModeChanged?.invoke(dark)
        onAmoledChanged?.invoke(amoled)
    }

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                title = { Text("主题") },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { paddingValues ->
        val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val captionBarPadding = WindowInsets.captionBar.asPaddingValues().calculateBottomPadding()

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemePreviewCard()

            Spacer(Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    ColorButton(
                        color = Color.Unspecified,
                        selected = dynamicColorActive,
                        isDark = previewDark,
                        isAmoled = previewAmoled,
                        paletteStyle = paletteStyle,
                        colorSpec = effectiveSpec,
                        fallbackKeyColor = keyColor,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                viewModel.setDynamicColor(true)
                                onDynamicColorChanged?.invoke(true)
                            }
                        },
                    )
                }
                if (!dynamicColorActive && keyColor !in keyColorOptions) {
                    item {
                        ColorButton(
                            color = Color(keyColor),
                            selected = true,
                            isDark = previewDark,
                            isAmoled = previewAmoled,
                            paletteStyle = paletteStyle,
                            colorSpec = effectiveSpec,
                            fallbackKeyColor = keyColor,
                            onClick = {},
                        )
                    }
                }
                items(keyColorOptions) { color ->
                    ColorButton(
                        color = Color(color),
                        selected = !dynamicColorActive && keyColor == color,
                        isDark = previewDark,
                        isAmoled = previewAmoled,
                        paletteStyle = paletteStyle,
                        colorSpec = effectiveSpec,
                        fallbackKeyColor = keyColor,
                        onClick = {
                            viewModel.setKeyColor(color)
                            onKeyColorChanged?.invoke(color)
                            onDynamicColorChanged?.invoke(false)
                        },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    ThemeDisplayMode.entries.forEachIndexed { index, mode ->
                        ExpressiveToggleButton(
                            checked = displayMode == mode,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    selectDisplayMode(mode)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { role = Role.RadioButton },
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                ThemeDisplayMode.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    ThemeDisplayMode.SYSTEM -> Icons.Default.Brightness4
                                    ThemeDisplayMode.LIGHT -> Icons.Default.Brightness7
                                    ThemeDisplayMode.DARK -> Icons.Default.Brightness3
                                    ThemeDisplayMode.AMOLED -> Icons.Default.Brightness1
                                },
                                contentDescription = mode.label,
                            )
                        }
                    }
                }

                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    items = listOf<@Composable () -> Unit>(
                        {
                            ExpressiveSegmentedDropdownItem(
                                index = 0,
                                count = 2,
                                title = "调色板风格",
                                items = PaletteStyle.entries.map { it.label },
                                selectedIndex = PaletteStyle.entries.indexOf(paletteStyle).coerceAtLeast(0),
                                onItemSelected = { index ->
                                    val value = PaletteStyle.entries[index]
                                    viewModel.setPaletteStyle(value)
                                    onPaletteStyleChanged?.invoke(value)
                                },
                                supportingContent = { Text(paletteStyle.description) },
                                leadingContent = { Icon(Icons.Default.Style, contentDescription = null) },
                            )
                        },
                        {
                            ExpressiveSegmentedDropdownItem(
                                index = 1,
                                count = 2,
                                title = "色彩标准",
                                items = ColorSpec.SpecVersion.entries.map { it.label },
                                selectedIndex = ColorSpec.SpecVersion.entries.indexOf(colorSpec).coerceAtLeast(0),
                                onItemSelected = { index ->
                                    val value = ColorSpec.SpecVersion.entries[index]
                                    viewModel.setColorSpec(value)
                                    onColorSpecChanged?.invoke(value)
                                },
                                supportingContent = {
                                    Text(
                                        if (effectiveSpec == colorSpec) "Material 动态色彩算法"
                                        else "当前风格不支持 2025，实际使用 2021",
                                    )
                                },
                                leadingContent = { Icon(Icons.Default.Tune, contentDescription = null) },
                            )
                        },
                    ),
                )

                val showExitDirection = predictiveBackAnimation == PredictiveBackAnimation.Scale
                val predictiveBackItemCount = if (showExitDirection) 2 else 1
                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    title = "预测性返回",
                    items = listOf<@Composable () -> Unit>({
                            ExpressiveSegmentedDropdownItem(
                                index = 0,
                                count = predictiveBackItemCount,
                                title = "预测性返回动画",
                                items = PredictiveBackAnimation.entries.map { it.label },
                                selectedIndex = PredictiveBackAnimation.entries
                                    .indexOf(predictiveBackAnimation)
                                    .coerceAtLeast(0),
                                onItemSelected = { index ->
                                    // Only update the animation setting here. Rewriting NavDisplay's
                                    // internal transition state makes the previous scene jump briefly.
                                    val value = PredictiveBackAnimation.entries[index]
                                    viewModel.setPredictiveBackAnimation(value)
                                    onPredictiveBackChanged?.invoke(value)
                                },
                                leadingContent = { Icon(Icons.Default.Tune, contentDescription = null) },
                            )
                        }) + if (showExitDirection) {
                        listOf<@Composable () -> Unit>({
                            ExpressiveSegmentedDropdownItem(
                                index = 1,
                                count = predictiveBackItemCount,
                                title = "退出方向",
                                items = PredictiveBackExitDirection.entries.map { it.label },
                                selectedIndex = PredictiveBackExitDirection.entries
                                    .indexOf(predictiveBackExitDirection)
                                    .coerceAtLeast(0),
                                onItemSelected = { index ->
                                    val value = PredictiveBackExitDirection.entries[index]
                                    viewModel.setPredictiveBackExitDirection(value)
                                    onPredictiveBackExitDirectionChanged?.invoke(value)
                                },
                                leadingContent = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                            )
                        })
                    } else {
                        emptyList()
                    },
                )
            }

            Spacer(Modifier.height(16.dp + navigationBarPadding + captionBarPadding))
        }
    }

}
