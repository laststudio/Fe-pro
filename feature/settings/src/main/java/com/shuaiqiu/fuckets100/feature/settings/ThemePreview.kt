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

internal enum class ThemeDisplayMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED,
}


@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
internal fun ThemePreviewCard() {
    val configuration = LocalConfiguration.current
    val ratio = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()
    // The preview uses the exact scheme already installed by FeThemeWrapper.
    // Recomputing a full Material color scheme here blocks the first navigation frame.
    val scheme = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(ratio),
            color = scheme.surfaceContainer,
            shape = MaterialTheme.shapes.extraLarge,
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
                            shape = MaterialTheme.shapes.small,
                        ) {}
                        if (showInfoCard) {
                            TonalCard(
                                containerColor = scheme.surfaceBright,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                shape = MaterialTheme.shapes.small,
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
internal fun ColorButton(
    color: Color,
    selected: Boolean,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
    fallbackKeyColor: Int,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val enabled = color != Color.Unspecified || dynamicAvailable
    val scheme = previewColorScheme(color, isDark, isAmoled, paletteStyle, colorSpec, fallbackKeyColor)
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
        shape = MaterialTheme.shapes.extraLarge,
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
internal fun previewColorScheme(
    color: Color,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
    fallbackKeyColor: Int,
): androidx.compose.material3.ColorScheme {
    val context = LocalContext.current
    val seed = if (color == Color.Unspecified) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) androidx.compose.material3.dynamicDarkColorScheme(context).primary
            else androidx.compose.material3.dynamicLightColorScheme(context).primary
        } else {
            Color(fallbackKeyColor)
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

internal val keyColorOptions = listOf(
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

internal val spec2025PaletteStyles = listOf(
    PaletteStyle.TonalSpot,
    PaletteStyle.Neutral,
    PaletteStyle.Vibrant,
    PaletteStyle.Expressive,
)

internal fun ColorSpec.SpecVersion.effectiveFor(style: PaletteStyle): ColorSpec.SpecVersion =
    if (this == ColorSpec.SpecVersion.SPEC_2025 && style !in spec2025PaletteStyles) {
        ColorSpec.SpecVersion.SPEC_2021
    } else {
        this
    }

internal val ThemeDisplayMode.label: String
    get() = when (this) {
        ThemeDisplayMode.SYSTEM -> "跟随系统"
        ThemeDisplayMode.LIGHT -> "浅色"
        ThemeDisplayMode.DARK -> "深色"
        ThemeDisplayMode.AMOLED -> "AMOLED"
    }

internal val PaletteStyle.label: String
    get() = when (this) {
        PaletteStyle.TonalSpot -> "柔和色调"
        PaletteStyle.Neutral -> "中性色调"
        PaletteStyle.Vibrant -> "鲜艳色调"
        PaletteStyle.Expressive -> "表现力色调"
        else -> name
    }

internal val PaletteStyle.description: String
    get() = when (this) {
        PaletteStyle.TonalSpot -> "平衡、自然的默认配色"
        PaletteStyle.Neutral -> "更低饱和度的克制配色"
        PaletteStyle.Vibrant -> "更高饱和度的明快配色"
        PaletteStyle.Expressive -> "对比鲜明的个性配色"
        else -> "兼容配色"
    }

internal val ColorSpec.SpecVersion.label: String
    get() = when (this) {
        ColorSpec.SpecVersion.SPEC_2021 -> "Material 2021"
        ColorSpec.SpecVersion.SPEC_2025 -> "Material 2025"
    }
