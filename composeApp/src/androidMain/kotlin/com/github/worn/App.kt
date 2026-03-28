package com.github.worn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.github.worn.ui.components.Tab
import com.github.worn.ui.screen.GapsScreen
import com.github.worn.ui.screen.OutfitsScreen
import com.github.worn.ui.screen.SettingsScreen
import com.github.worn.ui.screen.TryItScreen
import com.github.worn.ui.screen.WardrobeScreen
import com.github.worn.ui.theme.WornTheme

@Composable
fun App() {
    WornTheme {
        var activeTab by rememberSaveable { mutableStateOf(Tab.WARDROBE) }

        when (activeTab) {
            Tab.WARDROBE -> WardrobeScreen(onTabSelected = { activeTab = it })
            Tab.OUTFITS -> OutfitsScreen(onTabSelected = { activeTab = it })
            Tab.GAPS -> GapsScreen(onTabSelected = { activeTab = it })
            Tab.TRY_IT -> TryItScreen(onTabSelected = { activeTab = it })
            Tab.SETTINGS -> SettingsScreen(onTabSelected = { activeTab = it })
        }
    }
}
