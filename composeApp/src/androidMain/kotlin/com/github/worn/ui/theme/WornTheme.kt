package com.github.worn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object WornColors {
    // Backgrounds
    val BgPage = Color(0xFFF5F0EB)
    val BgCard = Color(0xFFFFFFFF)
    val BgElevated = Color(0xFFEDE8E1)

    // Borders
    val BorderSubtle = Color(0xFFE0D9D0)
    val BorderStrong = Color(0xFFC8C0B5)

    // Accents
    val AccentGreen = Color(0xFF7A9468)
    val AccentGreenEnd = Color(0xFF6B8A58)
    val AccentGreenDark = Color(0xFF5C6E50)
    val AccentIndigo = Color(0xFF6B7B8E)
    val AccentCoral = Color(0xFFA87560)
    val DeleteRed = Color(0xFFC45B4A)

    // Gradients
    val SaveGradientStart = Color(0xFF8FA47D)
    val SaveGradientEnd = Color(0xFF6B7F5E)

    // Text
    val TextPrimary = Color(0xFF2C2924)
    val TextSecondary = Color(0xFF7D776F)
    val TextMuted = Color(0xFFB5AFA8)
    val TextOnColor = Color(0xFFFFFFFF)

    // Icons
    val IconMuted = Color(0xFFA09A92)

    // Category dots
    val CategoryDotTop = Color(0xFF444444)
    val CategoryDotBottom = Color(0xFF2B4570)
    val CategoryDotDress = Color(0xFFA87560)
    val CategoryDotOuterwear = Color(0xFF7A9468)
    val CategoryDotShoes = Color(0xFF8B6914)
    val CategoryDotAccessory = Color(0xFFB59D6E)
}

private val WornLightColorScheme = lightColorScheme(
    primary = WornColors.AccentGreen,
    onPrimary = WornColors.TextOnColor,
    background = WornColors.BgPage,
    onBackground = WornColors.TextPrimary,
    surface = WornColors.BgCard,
    onSurface = WornColors.TextPrimary,
    surfaceVariant = WornColors.BgElevated,
    onSurfaceVariant = WornColors.TextSecondary,
    outline = WornColors.BorderSubtle,
    outlineVariant = WornColors.BorderStrong,
)

@Composable
fun WornTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WornLightColorScheme,
        content = content,
    )
}
