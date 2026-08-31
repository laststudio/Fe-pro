package com.shuaiqiu.fuckets100

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
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.shuaiqiu.fuckets100.component.material.ExpressiveDialog
import com.shuaiqiu.fuckets100.component.material.ExpressiveScaffold
import com.shuaiqiu.fuckets100.component.material.ExpressiveSegmentedDropdownItem
import com.shuaiqiu.fuckets100.component.material.ExpressiveSegmentedListItem
import com.shuaiqiu.fuckets100.component.material.ExpressiveToggleButton
import com.shuaiqiu.fuckets100.component.material.SegmentedColumn
import com.shuaiqiu.fuckets100.component.material.TonalCard
import com.shuaiqiu.fuckets100.component.material.TopBarBackButton
import com.shuaiqiu.fuckets100.component.material.expressiveTopAppBarColors

private enum class ThemeDisplayMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED,
}

@SuppressLint("RestrictedApi")
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
) {
    var isDarkMode by remember { mutableStateOf(ThemeManager.getSavedDarkMode()) }
    var isAutoDarkMode by remember { mutableStateOf(ThemeManager.getSavedAutoDarkMode()) }
    var useDynamicColor by remember { mutableStateOf(ThemeManager.getSavedDynamicColor()) }
    var keyColor by remember { mutableStateOf(ThemeManager.getSavedKeyColor()) }
    var paletteStyle by remember { mutableStateOf(ThemeManager.getSavedPaletteStyle()) }
    var colorSpec by remember { mutableStateOf(ThemeManager.getSavedColorSpec()) }
    var isAmoled by remember { mutableStateOf(ThemeManager.getSavedAmoled()) }
    var predictiveBackAnimation by remember {
        mutableStateOf(SettingsManager.getPredictiveBackAnimation())
    }
    var predictiveBackExitDirection by remember {
        mutableStateOf(SettingsManager.getPredictiveBackExitDirection())
    }
    val navigationTransition = LocalNavAnimatedContentScope.current.transition
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
        isAutoDarkMode = autoDark
        isDarkMode = dark
        isAmoled = amoled
        ThemeManager.saveAutoDarkMode(autoDark)
        ThemeManager.saveDarkMode(dark)
        ThemeManager.saveAmoled(amoled)
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
            ThemePreviewCard(
                keyColor = keyColor,
                useDynamicColor = dynamicColorActive,
                isDark = previewDark,
                isAmoled = previewAmoled,
                paletteStyle = paletteStyle,
                colorSpec = effectiveSpec,
            )

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
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                useDynamicColor = true
                                ThemeManager.saveDynamicColor(true)
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
                        onClick = {
                            keyColor = color
                            useDynamicColor = false
                            ThemeManager.saveKeyColor(color)
                            ThemeManager.saveDynamicColor(false)
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
                                    paletteStyle = PaletteStyle.entries[index]
                                    ThemeManager.savePaletteStyle(paletteStyle)
                                    onPaletteStyleChanged?.invoke(paletteStyle)
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
                                    colorSpec = ColorSpec.SpecVersion.entries[index]
                                    ThemeManager.saveColorSpec(colorSpec)
                                    onColorSpecChanged?.invoke(colorSpec)
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
                                    navigationTransition.setPlaytimeAfterInitialAndTargetStateEstablished(
                                        navigationTransition.targetState,
                                        navigationTransition.targetState,
                                        navigationTransition.playTimeNanos,
                                    )
                                    predictiveBackAnimation = PredictiveBackAnimation.entries[index]
                                    SettingsManager.savePredictiveBackAnimation(predictiveBackAnimation)
                                    onPredictiveBackChanged?.invoke(predictiveBackAnimation)
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
                                    predictiveBackExitDirection = PredictiveBackExitDirection.entries[index]
                                    SettingsManager.savePredictiveBackExitDirection(predictiveBackExitDirection)
                                    onPredictiveBackExitDirectionChanged?.invoke(predictiveBackExitDirection)
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

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCard(
    keyColor: Int,
    useDynamicColor: Boolean,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
) {
    val configuration = LocalConfiguration.current
    val ratio = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()
    val scheme = previewColorScheme(
        color = if (useDynamicColor) Color.Unspecified else Color(keyColor),
        isDark = isDark,
        isAmoled = isAmoled,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(ratio),
            color = scheme.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, scheme.outlineVariant),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Fe", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                }
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val showInfoCard = maxHeight >= 72.dp
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TonalCard(
                            containerColor = scheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {}
                        if (showInfoCard) {
                            TonalCard(
                                containerColor = scheme.surfaceBright,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                shape = RoundedCornerShape(8.dp),
                            ) {}
                        }
                    }
                }
                Surface(color = scheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.height(40.dp).fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = scheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorButton(
    color: Color,
    selected: Boolean,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val enabled = color != Color.Unspecified || dynamicAvailable
    val scheme = previewColorScheme(color, isDark, isAmoled, paletteStyle, colorSpec)
    val colorDescription = if (color == Color.Unspecified) {
        "动态颜色"
    } else {
        "主题色 #${color.toArgb().toUInt().toString(16).takeLast(6).uppercase()}"
    }

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainer,
        modifier = Modifier
            .size(72.dp)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
                contentDescription = colorDescription
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawArc(scheme.primaryContainer, 180f, 180f, true)
                drawArc(scheme.tertiaryContainer, 0f, 180f, true)
            }
            val scale by animateFloatAsState(if (selected) 1.1f else 1f, label = "keyColorScale")
            Box(
                modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).border(2.dp, scheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier.size(24.dp).clip(CircleShape).background(scheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !selected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) {
                    if (color == Color.Unspecified) {
                        Icon(Icons.Default.Wallpaper, contentDescription = "动态颜色", tint = scheme.primary)
                    } else {
                        Box(Modifier.size(20.dp).background(scheme.primary, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun previewColorScheme(
    color: Color,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
): androidx.compose.material3.ColorScheme {
    val context = LocalContext.current
    val seed = if (color == Color.Unspecified) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) androidx.compose.material3.dynamicDarkColorScheme(context).primary
            else androidx.compose.material3.dynamicLightColorScheme(context).primary
        } else {
            Color(ThemeManager.getSavedKeyColor())
        }
    } else {
        color
    }
    val scheme = rememberDynamicColorScheme(
        seedColor = seed,
        isDark = isDark,
        isAmoled = isDark && isAmoled,
        style = paletteStyle,
        specVersion = colorSpec.effectiveFor(paletteStyle),
    )
    return if (!isDark || !isAmoled) scheme else scheme.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceBright = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color.Black,
        surfaceContainer = Color.Black,
        surfaceContainerHigh = Color.Black,
        surfaceContainerHighest = Color.Black,
    )
}

private val keyColorOptions = listOf(
    0xFFF44336.toInt(),
    0xFFE91E63.toInt(),
    0xFF9C27B0.toInt(),
    0xFF673AB7.toInt(),
    0xFF3F51B5.toInt(),
    0xFF2196F3.toInt(),
    0xFF00BCD4.toInt(),
    0xFF009688.toInt(),
    0xFF4FAF50.toInt(),
    0xFFFFEB3B.toInt(),
    0xFFFFC107.toInt(),
    0xFFFF9800.toInt(),
    0xFF795548.toInt(),
    0xFF607D8F.toInt(),
    0xFFFF9CA8.toInt(),
)

private val spec2025PaletteStyles = listOf(
    PaletteStyle.TonalSpot,
    PaletteStyle.Neutral,
    PaletteStyle.Vibrant,
    PaletteStyle.Expressive,
)

private fun ColorSpec.SpecVersion.effectiveFor(style: PaletteStyle): ColorSpec.SpecVersion =
    if (this == ColorSpec.SpecVersion.SPEC_2025 && style !in spec2025PaletteStyles) {
        ColorSpec.SpecVersion.SPEC_2021
    } else {
        this
    }

private val ThemeDisplayMode.label: String
    get() = when (this) {
        ThemeDisplayMode.SYSTEM -> "跟随系统"
        ThemeDisplayMode.LIGHT -> "浅色"
        ThemeDisplayMode.DARK -> "深色"
        ThemeDisplayMode.AMOLED -> "AMOLED"
    }

private val PaletteStyle.label: String
    get() = when (this) {
        PaletteStyle.TonalSpot -> "柔和色调"
        PaletteStyle.Neutral -> "中性色调"
        PaletteStyle.Vibrant -> "鲜艳色调"
        PaletteStyle.Expressive -> "表现力色调"
        else -> name
    }

private val PaletteStyle.description: String
    get() = when (this) {
        PaletteStyle.TonalSpot -> "平衡、自然的默认配色"
        PaletteStyle.Neutral -> "更低饱和度的克制配色"
        PaletteStyle.Vibrant -> "更高饱和度的明快配色"
        PaletteStyle.Expressive -> "对比鲜明的个性配色"
        else -> "兼容配色"
    }

private val ColorSpec.SpecVersion.label: String
    get() = when (this) {
        ColorSpec.SpecVersion.SPEC_2021 -> "Material 2021"
        ColorSpec.SpecVersion.SPEC_2025 -> "Material 2025"
    }
