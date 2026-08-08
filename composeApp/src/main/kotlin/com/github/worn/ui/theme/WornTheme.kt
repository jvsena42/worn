package com.github.worn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

object WornDimens {
    val BottomBarClearance = 95.dp
}

@Composable
fun WornTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) WornDarkColorScheme else WornLightColorScheme
    val extras = if (darkTheme) WornDarkExtras else WornLightExtras

    CompositionLocalProvider(LocalWornExtras provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WornTypography,
            shapes = WornShapes,
            content = content,
        )
    }
}
