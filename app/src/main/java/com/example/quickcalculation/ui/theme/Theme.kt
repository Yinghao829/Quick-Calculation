package com.example.quickcalculation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightScheme = lightColorScheme(
    primary = QuickColors.PrimaryLight,
    primaryContainer = QuickColors.LightContainer,
    background = QuickColors.LightBackground,
    surface = QuickColors.LightSurface,
    onPrimary = QuickColors.LightSurface,
    onBackground = QuickColors.LightTextPrimary,
    onSurface = QuickColors.LightTextPrimary,
    onSurfaceVariant = QuickColors.LightTextSecondary,
    outline = QuickColors.LightDivider,
    error = QuickColors.Error,
)

private val DarkScheme = darkColorScheme(
    primary = QuickColors.PrimaryDark,
    primaryContainer = QuickColors.DarkContainer,
    background = QuickColors.DarkBackground,
    surface = QuickColors.DarkSurface,
    onPrimary = QuickColors.DarkBackground,
    onBackground = QuickColors.DarkTextPrimary,
    onSurface = QuickColors.DarkTextPrimary,
    onSurfaceVariant = QuickColors.DarkTextSecondary,
    outline = QuickColors.DarkDivider,
    error = QuickColors.ErrorDark,
)

@Composable
fun QuickCalculationTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(
        LocalSuccessColor provides if (isDark) QuickColors.SuccessDark else QuickColors.Success,
    ) {
        MaterialTheme(
            colorScheme = if (isDark) DarkScheme else LightScheme,
            typography = QuickTypography,
            content = content,
        )
    }
}
