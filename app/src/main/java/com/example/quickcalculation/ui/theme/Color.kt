package com.example.quickcalculation.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object QuickColors {
    // 语义色（浅色：600 档；深色：500 档，见 VISUAL_DESIGN_SCHEME.md）
    val Success = Color(0xFF16A34A)     // Green 600
    val Error = Color(0xFFDC2626)       // Red 600
    val Warning = Color(0xFFFA8C16)

    val SuccessDark = Color(0xFF22C55E) // Green 500
    val ErrorDark = Color(0xFFEF4444)   // Red 500

    // 主色
    val PrimaryLight = Color(0xFF2563EB) // Blue 600
    val PrimaryDark = Color(0xFF3B82F6)  // Blue 500

    // 浅色
    val LightBackground = Color(0xFFF5F7FA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightTextPrimary = Color(0xFF1A1D24)
    val LightTextSecondary = Color(0xFF6B7280)
    val LightTextTertiary = Color(0xFF9CA3AF)
    val LightDivider = Color(0xFFE9EDF2)
    val LightContainer = Color(0xFFF0F2F5)

    // 深色
    val DarkBackground = Color(0xFF0B0D10)
    val DarkSurface = Color(0xFF171A1F)
    val DarkTextPrimary = Color(0xFFEDEFF2)
    val DarkTextSecondary = Color(0xFFD1D5DB)
    val DarkTextTertiary = Color(0xFF8A8F98)
    val DarkDivider = Color(0xFF262B33)
    val DarkContainer = Color(0xFF1E222A)
}

/** 成功色在 Material3 ColorScheme 中无对应槽位，用 CompositionLocal 随主题切换。 */
val LocalSuccessColor = staticCompositionLocalOf { QuickColors.Success }
