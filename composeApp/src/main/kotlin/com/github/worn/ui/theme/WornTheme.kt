@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.github.worn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun WornTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) WornDarkColorScheme else WornLightColorScheme
    val extras = if (darkTheme) WornDarkExtras else WornLightExtras

    CompositionLocalProvider(LocalWornExtras provides extras) {
        // MaterialExpressiveTheme rather than MaterialTheme: it is the only way to supply a
        // MotionScheme, and `expressive()` gives every Material component spring-based motion
        // instead of the flat easing curves. Nothing else about the theme changes.
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = WornTypography,
            shapes = WornShapes,
            content = content,
        )
    }
}
