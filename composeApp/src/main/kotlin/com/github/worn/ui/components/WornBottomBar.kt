@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.github.worn.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.R

enum class Tab(
    @StringRes val labelRes: Int,
    val testTag: String,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null,
) {
    WARDROBE(R.string.tab_wardrobe, "tab_wardrobe", iconRes = R.drawable.ic_shirt),
    OUTFITS(R.string.tab_outfits, "tab_outfits", icon = Icons.Outlined.Layers),
    GAPS(R.string.tab_gaps, "tab_gaps", icon = Icons.Outlined.Extension),
    TRY_IT(R.string.tab_try_it, "tab_try_it", icon = Icons.Outlined.QrCodeScanner),
    SETTINGS(R.string.tab_settings, "tab_settings", icon = Icons.Outlined.Settings),
}

@Composable
fun WornBottomBar(
    activeTab: Tab,
    onTabSelected: (Tab) -> Unit,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (isCompact) 10.dp else 32.dp,
                end = if (isCompact) 10.dp else 32.dp,
                top = 12.dp,
                bottom = 21.dp,
            ),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraExtraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .then(
                    if (isCompact) Modifier.fillMaxWidth()
                    else Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                )
                .height(62.dp)
                .testTag("bottom_bar"),
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
            ) {
                Tab.entries.forEach { tab ->
                    TabItem(
                        tab = tab,
                        isActive = tab == activeTab,
                        onClick = { onTabSelected(tab) },
                    )
                }
            }
        }
    }
}

/**
 * One tab: the app's full-width pill, with the touch feedback and semantics it was missing.
 *
 * Deliberately *not* [NavigationBarItem]. That gives a ripple and an animated indicator for free,
 * but its indicator only ever wraps the icon — the label sits outside it. Worn's pill wraps icon
 * and label together, so the M3 item left the selected label stranded on the bar background in
 * `onPrimary`, which is dark-green-on-dark in the dark scheme and nearly unreadable.
 *
 * So the pill container stays hand-built, and the two things that actually needed fixing are
 * addressed directly: [selectable] supplies a ripple bounded to the pill plus proper
 * selected/Tab semantics for TalkBack, and the fill animates between tabs rather than snapping.
 *
 * An earlier comment here blamed the ripple for repainting the bar for ~1s after each tap and
 * removed indication entirely. The cost was actually the pager recomposing the destination page,
 * which `beyondViewportPageCount` and the snap-scroll in App.kt already address — suppressing
 * touch feedback only hid it.
 */
@Composable
private fun RowScope.TabItem(
    tab: Tab,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(tab.labelRes)
    val haptics = LocalHapticFeedback.current
    val containerColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "tabContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "tabContent",
    )

    Surface(
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(MaterialTheme.shapes.extraLargeIncreased)
            .selectable(
                selected = isActive,
                onClick = {
                    // SegmentTick, not LongPress: this is a discrete position change in a row of
                    // segments, which is exactly what that constant is for.
                    if (!isActive) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onClick()
                },
                role = Role.Tab,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
            )
            .testTag(tab.testTag),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight(),
        ) {
            // contentDescription is null: the label below already names the tab, and TalkBack
            // would otherwise announce it twice.
            if (tab.iconRes != null) {
                Icon(painterResource(id = tab.iconRes), null, Modifier.size(18.dp))
            } else if (tab.icon != null) {
                Icon(tab.icon, null, Modifier.size(18.dp))
            }
            Text(
                text = label,
                // labelSmall already carries the 10sp/SemiBold/+0.5sp tracking these labels used.
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}



