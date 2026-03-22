package com.github.worn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object WornColors {
    val BgPage = Color(0xFFF5F0EB)
    val BgCard = Color(0xFFFFFFFF)
    val BgElevated = Color(0xFFEDE8E1)
    val BorderSubtle = Color(0xFFE0D9D0)
    val BorderStrong = Color(0xFFC8C0B5)
    val AccentGreen = Color(0xFF7A9468)
    val TextPrimary = Color(0xFF2C2924)
    val TextSecondary = Color(0xFF7D776F)
    val TextMuted = Color(0xFFB5AFA8)
    val TextOnColor = Color(0xFFFFFFFF)
    val IconMuted = Color(0xFFA09A92)
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
