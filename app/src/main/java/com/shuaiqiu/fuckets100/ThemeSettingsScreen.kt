package com.shuaiqiu.fuckets100

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.shuaiqiu.fuckets100.component.material.ExpressiveDialog
import com.shuaiqiu.fuckets100.component.material.ExpressiveScaffold
import com.shuaiqiu.fuckets100.component.material.ExpressiveSegmentedListItem
import com.shuaiqiu.fuckets100.component.material.ExpressiveSegmentedSwitchItem
import com.shuaiqiu.fuckets100.component.material.SegmentedColumn
import com.shuaiqiu.fuckets100.component.material.TopBarBackButton
import com.shuaiqiu.fuckets100.component.material.expressiveTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    onThemeChanged: ((AppTheme) -> Unit)? = null,
    onDarkModeChanged: ((Boolean) -> Unit)? = null,
    onAutoDarkModeChanged: ((Boolean) -> Unit)? = null,
    onDynamicColorChanged: ((Boolean) -> Unit)? = null,
    onPaletteStyleChanged: ((PaletteStyle) -> Unit)? = null,
    onColorSpecChanged: ((ColorSpec.SpecVersion) -> Unit)? = null,
    onAmoledChanged: ((Boolean) -> Unit)? = null,
    onPredictiveBackChanged: ((PredictiveBackMode) -> Unit)? = null,
) {
    var selectedTheme by remember { mutableStateOf(ThemeManager.getSavedTheme()) }
    var isDarkMode by remember { mutableStateOf(ThemeManager.getSavedDarkMode()) }
    var isAutoDarkMode by remember { mutableStateOf(ThemeManager.getSavedAutoDarkMode()) }
    var useDynamicColor by remember { mutableStateOf(ThemeManager.getSavedDynamicColor()) }
    var paletteStyle by remember { mutableStateOf(ThemeManager.getSavedPaletteStyle()) }
    var colorSpec by remember { mutableStateOf(ThemeManager.getSavedColorSpec()) }
    var isAmoled by remember { mutableStateOf(ThemeManager.getSavedAmoled()) }
    var predictiveBackMode by remember { mutableStateOf(SettingsManager.getPredictiveBackMode()) }
    var showPredictiveBackDialog by remember { mutableStateOf(false) }
    var showPaletteStyleDialog by remember { mutableStateOf(false) }
    var showColorSpecDialog by remember { mutableStateOf(false) }
    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val systemDarkMode = isSystemInDarkTheme()
    val effectiveDarkMode = if (isAutoDarkMode) systemDarkMode else isDarkMode
    val supportsSpec2025 = paletteStyle.supportsSpec2025()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    ExpressiveScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("主题设置") },
                subtitle = { Text("颜色、显示与返回动画") },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(16.dp))
            SettingsHeading("颜色主题", "选择白天和夜间模式共用的主色调")

            LazyRow(
                contentPadding = PaddingValues(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(ThemeManager.getColorThemes()) { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = selectedTheme == theme,
                        enabled = !(useDynamicColor && dynamicColorAvailable),
                        onClick = {
                            selectedTheme = theme
                            ThemeManager.saveTheme(theme)
                            onThemeChanged?.invoke(theme)
                        },
                    )
                }
            }

            SegmentedColumn(
                title = "配色方式",
                items = listOf(
                    {
                        ExpressiveSegmentedSwitchItem(
                            index = 0,
                            count = 3,
                            checked = useDynamicColor && dynamicColorAvailable,
                            enabled = dynamicColorAvailable,
                            onCheckedChange = { checked ->
                                useDynamicColor = checked
                                ThemeManager.saveDynamicColor(checked)
                                onDynamicColorChanged?.invoke(checked)
                            },
                            headlineContent = { Text("从壁纸提取主题色") },
                            supportingContent = {
                                Text(if (dynamicColorAvailable) "使用系统 Material You 动态配色" else "需要 Android 12 或更高版本")
                            },
                            leadingContent = {
                                Icon(Icons.Default.Wallpaper, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                        )
                    },
                    {
                        ExpressiveSegmentedListItem(
                            index = 1,
                            count = 3,
                            onClick = { showPaletteStyleDialog = true },
                            headlineContent = { Text("调色板风格") },
                            supportingContent = { Text(paletteStyle.description) },
                            leadingContent = {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingContent = { Text(paletteStyle.label, color = MaterialTheme.colorScheme.primary) },
                        )
                    },
                    {
                        ExpressiveSegmentedListItem(
                            index = 2,
                            count = 3,
                            onClick = { showColorSpecDialog = true },
                            headlineContent = { Text("Material 色彩规范") },
                            supportingContent = {
                                Text(if (supportsSpec2025) "2021 与 2025 规范可选" else "当前风格不支持 2025，已回退至 2021")
                            },
                            leadingContent = {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingContent = { Text(colorSpec.label, color = MaterialTheme.colorScheme.primary) },
                        )
                    },
                ),
            )

            Spacer(Modifier.height(24.dp))
            SettingsHeading("显示模式", "在当前彩色主题上切换白天或夜间")
            SegmentedColumn(
                title = "亮度与对比度",
                items = listOf(
                    {
                        ExpressiveSegmentedSwitchItem(
                            index = 0,
                            count = 2,
                            checked = isAutoDarkMode,
                            onCheckedChange = { checked ->
                                isAutoDarkMode = checked
                                ThemeManager.saveAutoDarkMode(checked)
                                onAutoDarkModeChanged?.invoke(checked)
                            },
                            headlineContent = { Text("跟随系统") },
                            supportingContent = { Text(if (systemDarkMode) "当前系统为夜间模式" else "当前系统为日间模式") },
                            leadingContent = { Icon(Icons.Default.Sync, null, tint = MaterialTheme.colorScheme.primary) },
                        )
                    },
                    {
                        ExpressiveSegmentedSwitchItem(
                            index = 1,
                            count = 2,
                            checked = isAmoled,
                            onCheckedChange = { checked ->
                                isAmoled = checked
                                ThemeManager.saveAmoled(checked)
                                onAmoledChanged?.invoke(checked)
                            },
                            headlineContent = { Text("AMOLED 纯黑表面") },
                            supportingContent = {
                                Text(if (effectiveDarkMode) "夜间模式下使用纯黑背景与表面" else "仅在夜间模式下生效")
                            },
                            leadingContent = { Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.primary) },
                        )
                    },
                ),
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                SegmentedButton(
                    selected = !isDarkMode,
                    enabled = !isAutoDarkMode,
                    onClick = {
                        isDarkMode = false
                        ThemeManager.saveDarkMode(false)
                        onDarkModeChanged?.invoke(false)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = themeModeSegmentedButtonColors(),
                    icon = { Icon(Icons.Default.LightMode, contentDescription = null) },
                    label = { Text("日间") },
                )
                SegmentedButton(
                    selected = isDarkMode,
                    enabled = !isAutoDarkMode,
                    onClick = {
                        isDarkMode = true
                        ThemeManager.saveDarkMode(true)
                        onDarkModeChanged?.invoke(true)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = themeModeSegmentedButtonColors(),
                    icon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                    label = { Text("夜间") },
                )
            }

            Spacer(Modifier.height(24.dp))
            SettingsHeading("返回手势", "切换缩放、渐变、系统默认或关闭动画")
            SegmentedColumn(
                items = listOf(
                    {
                        ExpressiveSegmentedListItem(
                            index = 0,
                            count = 1,
                            onClick = { showPredictiveBackDialog = true },
                            headlineContent = { Text("预见性返回动画") },
                            supportingContent = { Text("${predictiveBackMode.label} · ${predictiveBackMode.description}") },
                            leadingContent = { Icon(Icons.Default.Sync, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Text(predictiveBackMode.label, color = MaterialTheme.colorScheme.primary) },
                        )
                    },
                ),
            )
            Spacer(Modifier.height(88.dp))
        }
    }

    if (showPredictiveBackDialog) {
        ExpressiveDialog(
            onDismissRequest = { showPredictiveBackDialog = false },
            title = { Text("预见性返回动画") },
            text = {
                Column {
                    PredictiveBackMode.entries.forEach { mode ->
                        ListItem(
                            headlineContent = { Text(mode.label) },
                            supportingContent = { Text(mode.description) },
                            trailingContent = {
                                if (predictiveBackMode == mode) Icon(Icons.Default.Check, null)
                            },
                            modifier = Modifier.clickable {
                                predictiveBackMode = mode
                                SettingsManager.savePredictiveBackMode(mode)
                                showPredictiveBackDialog = false
                                onPredictiveBackChanged?.invoke(mode)
                            },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPredictiveBackDialog = false }) { Text("关闭") } },
        )
    }

    if (showPaletteStyleDialog) {
        ExpressiveDialog(
            onDismissRequest = { showPaletteStyleDialog = false },
            title = { Text("调色板风格") },
            text = {
                Column {
                    paletteStyles.forEach { style ->
                        ListItem(
                            headlineContent = { Text(style.label) },
                            supportingContent = { Text(style.description) },
                            trailingContent = { if (paletteStyle == style) Icon(Icons.Default.Check, null) },
                            modifier = Modifier.clickable {
                                paletteStyle = style
                                ThemeManager.savePaletteStyle(style)
                                onPaletteStyleChanged?.invoke(style)
                                showPaletteStyleDialog = false
                            },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPaletteStyleDialog = false }) { Text("关闭") } },
        )
    }

    if (showColorSpecDialog) {
        ExpressiveDialog(
            onDismissRequest = { showColorSpecDialog = false },
            title = { Text("Material 色彩规范") },
            text = {
                Column {
                    ColorSpecItem(ColorSpec.SpecVersion.SPEC_2021, colorSpec, enabled = true) { spec ->
                        colorSpec = spec
                        ThemeManager.saveColorSpec(spec)
                        onColorSpecChanged?.invoke(spec)
                        showColorSpecDialog = false
                    }
                    ColorSpecItem(ColorSpec.SpecVersion.SPEC_2025, colorSpec, enabled = supportsSpec2025) { spec ->
                        colorSpec = spec
                        ThemeManager.saveColorSpec(spec)
                        onColorSpecChanged?.invoke(spec)
                        showColorSpecDialog = false
                    }
                    if (!supportsSpec2025) {
                        Text(
                            text = "${paletteStyle.label} 不支持 2025 规范，主题将使用 2021 规范。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showColorSpecDialog = false }) { Text("关闭") } },
        )
    }
}

@Composable
private fun SettingsHeading(title: String, description: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(
        description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
fun ThemeCard(
    theme: AppTheme,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceBright,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(theme.primary),
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = theme.onPrimary,
                        modifier = Modifier.size(24.dp).align(Alignment.Center),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                theme.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ColorSpecItem(
    spec: ColorSpec.SpecVersion,
    selectedSpec: ColorSpec.SpecVersion,
    enabled: Boolean,
    onClick: (ColorSpec.SpecVersion) -> Unit,
) {
    ListItem(
        headlineContent = { Text(spec.label) },
        supportingContent = {
            Text(if (spec == ColorSpec.SpecVersion.SPEC_2025) "更新的 Material 色彩算法" else "兼容的 Material 色彩算法")
        },
        trailingContent = { if (selectedSpec == spec) Icon(Icons.Default.Check, null) },
        modifier = Modifier.clickable(enabled = enabled) { onClick(spec) },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
    )
}

@Composable
private fun themeModeSegmentedButtonColors(): SegmentedButtonColors = SegmentedButtonDefaults.colors(
    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    inactiveContainerColor = MaterialTheme.colorScheme.surfaceBright,
    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledActiveContainerColor = MaterialTheme.colorScheme.primaryContainer,
    disabledActiveContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    disabledInactiveContainerColor = MaterialTheme.colorScheme.surfaceBright,
    disabledInactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f),
)

private val paletteStyles = listOf(
    PaletteStyle.TonalSpot,
    PaletteStyle.Neutral,
    PaletteStyle.Vibrant,
    PaletteStyle.Expressive,
)

private fun PaletteStyle.supportsSpec2025(): Boolean = this in paletteStyles

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
