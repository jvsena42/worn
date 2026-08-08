package com.github.worn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.window.core.layout.WindowWidthSizeClass
import com.github.worn.domain.model.AppShortcut
import com.github.worn.ui.components.Tab
import com.github.worn.ui.components.WornBottomBar
import com.github.worn.ui.screen.GapsScreen
import com.github.worn.ui.screen.OutfitsScreen
import com.github.worn.ui.screen.SettingsScreen
import com.github.worn.ui.screen.TryItScreen
import com.github.worn.ui.screen.WardrobeScreen
import com.github.worn.ui.theme.WornTheme
import com.github.worn.ui.util.SharedPhoto
import com.github.worn.ui.util.ShortcutCommand
import kotlinx.coroutines.launch

private val tabs = Tab.entries.toList()

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("FunctionNaming")
@Composable
fun App(
    sharedPhoto: SharedPhoto? = null,
    onSharedPhotoConsumed: () -> Unit = {},
    shortcut: ShortcutCommand? = null,
    onShortcutConsumed: () -> Unit = {},
) {
    WornTheme {
        val pagerState = rememberPagerState(pageCount = { tabs.size })
        val scope = rememberCoroutineScope()
        val windowInfo = currentWindowAdaptiveInfo()
        val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

        val onTabSelected: (Tab) -> Unit = { tab ->
            val index = tabs.indexOf(tab)
            if (index >= 0) {
                // Not animateScrollToPage: animating composes every page it scrolls past.
                scope.launch { pagerState.scrollToPage(index) }
            }
        }

        LaunchedEffect(sharedPhoto) {
            if (sharedPhoto != null) onTabSelected(Tab.TRY_IT)
        }

        LaunchedEffect(shortcut) {
            when (shortcut?.shortcut) {
                // The tab switch is the whole job, so this one is done here.
                AppShortcut.TRY_IT -> {
                    onTabSelected(Tab.TRY_IT)
                    onShortcutConsumed()
                }
                // WardrobeScreen consumes this once the sheet is actually open.
                AppShortcut.ADD_ITEM -> onTabSelected(Tab.WARDROBE)
                null -> Unit
            }
        }

        // Scaffold rather than a Box overlay: it measures the bar and hands the pager a bottom
        // inset that already accounts for it, which is what lets every screen drop the hardcoded
        // 95dp clearance they each used to subtract by hand.
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            bottomBar = {
                WornBottomBar(
                    activeTab = tabs[pagerState.currentPage],
                    onTabSelected = onTabSelected,
                    isCompact = isCompact,
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true },
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                // Bottom padding only. This Scaffold exists for the bottom bar; each screen owns
                // its own top inset through its TopAppBar, and consuming innerPadding wholesale
                // applied the status-bar inset twice — which showed up as a ~90dp dead band above
                // every title that no app-bar height parameter could explain.
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
            ) { page ->
                when (tabs[page]) {
                    Tab.WARDROBE -> WardrobeScreen(
                        onTabSelected = onTabSelected,
                        openAddSheet = shortcut?.takeIf { it.shortcut == AppShortcut.ADD_ITEM },
                        onAddSheetOpened = onShortcutConsumed,
                    )
                    Tab.OUTFITS -> OutfitsScreen(onTabSelected = onTabSelected)
                    Tab.GAPS -> GapsScreen(onTabSelected = onTabSelected)
                    Tab.TRY_IT -> TryItScreen(
                        onTabSelected = onTabSelected,
                        sharedPhoto = sharedPhoto,
                        onSharedPhotoConsumed = onSharedPhotoConsumed,
                    )
                    Tab.SETTINGS -> SettingsScreen(onTabSelected = onTabSelected)
                }
            }
        }
    }
}

