package com.github.worn

import androidx.compose.runtime.Composable
import com.github.worn.ui.screen.WardrobeScreen
import com.github.worn.ui.theme.WornTheme

@Composable
fun App() {
    WornTheme {
        WardrobeScreen()
    }
}
