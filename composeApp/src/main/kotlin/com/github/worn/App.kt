package com.github.worn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.window.core.layout.WindowWidthSizeClass
import com.github.worn.ui.components.Tab
import com.github.worn.ui.components.WornBottomBar
import com.github.worn.ui.screen.GapsScreen
import com.github.worn.ui.screen.OutfitsScreen
import com.github.worn.ui.screen.SettingsScreen
import com.github.worn.ui.screen.TryItScreen
import com.github.worn.ui.screen.WardrobeScreen
import com.github.worn.ui.theme.WornColors
import com.github.worn.ui.theme.WornTheme
import kotlinx.coroutines.launch

private val tabs = Tab.entries.toList()

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("FunctionNaming")
@Composable
fun App() {
    WornTheme {
        val pagerState = rememberPagerState(pageCount = { tabs.size })
        val scope = rememberCoroutineScope()
        val windowInfo = currentWindowAdaptiveInfo()
        val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

        val onTabSelected: (Tab) -> Unit = { tab ->
            val index = tabs.indexOf(tab)
            if (index >= 0) {
                // scrollToPage, not animateScrollToPage: animating jumps the pager *through* the
                // pages in between, composing each one during the animation. Tapping Settings from
                // Wardrobe would compose Gaps and Try-It on the way. Snapping composes only the
                // destination. Swiping still animates, because that is the gesture driving it.
                scope.launch { pagerState.scrollToPage(index) }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WornColors.BgPage)
                .semantics { testTagsAsResourceId = true },
        ) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (tabs[page]) {
                    Tab.WARDROBE -> WardrobeScreen(onTabSelected = onTabSelected)
                    Tab.OUTFITS -> OutfitsScreen(onTabSelected = onTabSelected)
                    Tab.GAPS -> GapsScreen(onTabSelected = onTabSelected)
                    Tab.TRY_IT -> TryItScreen(onTabSelected = onTabSelected)
                    Tab.SETTINGS -> SettingsScreen(onTabSelected = onTabSelected)
                }
            }

            WornBottomBar(
                activeTab = tabs[pagerState.currentPage],
                onTabSelected = onTabSelected,
                isCompact = isCompact,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            )
        }
    }
}
