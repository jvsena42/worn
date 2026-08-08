@file:OptIn(ExperimentalMaterial3Api::class)

package com.github.worn.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared colours for the screen-level top app bars.
 *
 * The container is [androidx.compose.material3.ColorScheme.surface] rather than the M3 default so
 * the bar reads as part of the page instead of as a separate slab — the app's screens are a single
 * tinted sheet, and a distinct bar surface would cut them in half.
 *
 * `scrolledContainerColor` is the same value on purpose: the elevation tint M3 applies once the
 * bar collapses would introduce a colour the palette does not contain.
 */
@Composable
fun wornTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surface,
    scrolledContainerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    subtitleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

/**
 * Nudges the app-bar title out to the screens' 24dp content gutter.
 *
 * M3 indents the title 16dp, which leaves it 8dp left of the chips and cards below it. The bar
 * exposes no title-padding parameter, so the offset is applied to the title content itself.
 */
val WornTopAppBarTitlePadding = Modifier.padding(start = 8.dp)
