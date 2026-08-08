@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.github.worn.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The screen-level app bar: one title, an optional subtitle, optional actions.
 *
 * Medium rather than Large, with heights tightened below the M3 defaults. The default large bar
 * reserves 152dp expanded, of which the top 64dp is the row that would hold a navigation icon —
 * and none of these screens have one, so it reads as a large empty gap above the title. These
 * screens are tab destinations with nowhere to navigate back to.
 *
 * [COLLAPSED_HEIGHT] is still tall enough for the Outfits "Create" action, which is the only
 * thing that ever occupies that row.
 */
@Composable
fun WornTopAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    MediumFlexibleTopAppBar(
        title = { Text(title, modifier = TITLE_GUTTER_NUDGE) },
        subtitle = subtitle?.let { { Text(it, modifier = TITLE_GUTTER_NUDGE) } },
        actions = actions,
        collapsedHeight = COLLAPSED_HEIGHT,
        expandedHeight = if (subtitle != null) EXPANDED_HEIGHT else EXPANDED_HEIGHT_NO_SUBTITLE,
        // Same colour for the container and its scrolled state: the screens are a single tinted
        // sheet, so a distinct bar surface would cut them in half, and M3's elevation tint on
        // scroll would introduce a colour the palette does not contain.
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            subtitleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        scrollBehavior = scrollBehavior,
        modifier = modifier,
    )
}

// The bar bottom-aligns its title block, so the space above the title is
// expandedHeight minus the text's own height. These are sized so that gap lands near the ~24dp
// the hand-built headers used to have, rather than the ~96dp the M3 large-bar defaults produce.
private val COLLAPSED_HEIGHT = 48.dp
private val EXPANDED_HEIGHT = 88.dp
private val EXPANDED_HEIGHT_NO_SUBTITLE = 68.dp

/**
 * Nudges the title out to the screens' 24dp content gutter.
 *
 * M3 indents it 16dp, which leaves it 8dp left of the chips and cards below. The bar exposes no
 * title-padding parameter, so the offset goes on the title content itself.
 */
private val TITLE_GUTTER_NUDGE = Modifier.padding(start = 8.dp)
