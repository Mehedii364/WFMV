package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ObsidianColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = ObsidianBg,
    primaryContainer = GoldContainer,
    onPrimaryContainer = GoldOnContainer,
    secondary = GoldSecondary,
    onSecondary = ObsidianBg,
    secondaryContainer = ObsidianSurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldSuccess,
    onTertiary = ObsidianBg,
    tertiaryContainer = EmeraldSuccessContainer,
    onTertiaryContainer = TextPrimary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder,
    outlineVariant = ObsidianSurfaceElevated,
    error = CrimsonError,
    onError = TextPrimary,
    errorContainer = CrimsonErrorContainer,
    onErrorContainer = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = GoldPrimary,
    onPrimary = ObsidianBg,
    primaryContainer = GoldContainer,
    onPrimaryContainer = GoldOnContainer,
    secondary = ObsidianSurfaceElevated,
    onSecondary = TextPrimary,
    background = ObsidianBg, // WFMV is an Obsidian-first ecosystem
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    error = CrimsonError
)

@Composable
fun WfmvTheme(
    darkTheme: Boolean = true, // Default to Premium Obsidian Dark
    content: @Composable () -> Unit
) {
    val colorScheme = ObsidianColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = ObsidianBg.toArgb()
                window.navigationBarColor = ObsidianBg.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    WfmvTheme(darkTheme = darkTheme, content = content)
}
