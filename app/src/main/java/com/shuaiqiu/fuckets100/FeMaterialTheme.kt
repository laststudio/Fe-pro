package com.shuaiqiu.fuckets100

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

val LocalFeDarkTheme = staticCompositionLocalOf { false }

private val FeTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
)

@Composable
fun FeThemeWrapper(
    theme: AppTheme,
    isDarkMode: Boolean = ThemeManager.getSavedDarkMode(),
    useDynamicColor: Boolean = ThemeManager.getSavedDynamicColor(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val seed = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDarkMode) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
    } else {
        theme.primary
    }
    val colorScheme = rememberDynamicColorScheme(
        seedColor = seed,
        isDark = isDarkMode,
        style = PaletteStyle.TonalSpot,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
    )
    val animatedColorScheme = colorScheme.animateAsState()

    LaunchedEffect(isDarkMode) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkMode
            isAppearanceLightNavigationBars = !isDarkMode
        }
    }

    CompositionLocalProvider(LocalFeDarkTheme provides isDarkMode) {
        MaterialExpressiveTheme(
            colorScheme = animatedColorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = FeTypography,
            content = content,
        )
    }
}

// Keep theme changes continuous instead of replacing every color role in one frame.
@Composable
private fun ColorScheme.animateAsState(): ColorScheme {
    @Composable
    fun animate(color: Color): Color = animateColorAsState(
        targetValue = color,
        animationSpec = spring(),
        label = "theme_color_animation",
    ).value

    return ColorScheme(
        primary = animate(primary),
        onPrimary = animate(onPrimary),
        primaryContainer = animate(primaryContainer),
        onPrimaryContainer = animate(onPrimaryContainer),
        inversePrimary = animate(inversePrimary),
        secondary = animate(secondary),
        onSecondary = animate(onSecondary),
        secondaryContainer = animate(secondaryContainer),
        onSecondaryContainer = animate(onSecondaryContainer),
        tertiary = animate(tertiary),
        onTertiary = animate(onTertiary),
        tertiaryContainer = animate(tertiaryContainer),
        onTertiaryContainer = animate(onTertiaryContainer),
        background = animate(background),
        onBackground = animate(onBackground),
        surface = animate(surface),
        onSurface = animate(onSurface),
        surfaceVariant = animate(surfaceVariant),
        onSurfaceVariant = animate(onSurfaceVariant),
        surfaceTint = animate(surfaceTint),
        inverseSurface = animate(inverseSurface),
        inverseOnSurface = animate(inverseOnSurface),
        error = animate(error),
        onError = animate(onError),
        errorContainer = animate(errorContainer),
        onErrorContainer = animate(onErrorContainer),
        outline = animate(outline),
        outlineVariant = animate(outlineVariant),
        scrim = animate(scrim),
        surfaceBright = animate(surfaceBright),
        surfaceDim = animate(surfaceDim),
        surfaceContainer = animate(surfaceContainer),
        surfaceContainerHigh = animate(surfaceContainerHigh),
        surfaceContainerHighest = animate(surfaceContainerHighest),
        surfaceContainerLow = animate(surfaceContainerLow),
        surfaceContainerLowest = animate(surfaceContainerLowest),
        primaryFixed = animate(primaryFixed),
        primaryFixedDim = animate(primaryFixedDim),
        onPrimaryFixed = animate(onPrimaryFixed),
        onPrimaryFixedVariant = animate(onPrimaryFixedVariant),
        secondaryFixed = animate(secondaryFixed),
        secondaryFixedDim = animate(secondaryFixedDim),
        onSecondaryFixed = animate(onSecondaryFixed),
        onSecondaryFixedVariant = animate(onSecondaryFixedVariant),
        tertiaryFixed = animate(tertiaryFixed),
        tertiaryFixedDim = animate(tertiaryFixedDim),
        onTertiaryFixed = animate(onTertiaryFixed),
        onTertiaryFixedVariant = animate(onTertiaryFixedVariant),
    )
}
