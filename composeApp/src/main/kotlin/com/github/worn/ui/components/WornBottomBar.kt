package com.github.worn.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
            shape = RoundedCornerShape(36.dp),
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
                    val isActive = tab == activeTab
                    TabItem(
                        tab = tab,
                        isActive = isActive,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: Tab,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        // No ripple: it repaints the bar for ~1s after each tap, which reads as a slow page
        // switch. The active tab's fill already signals selection.
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .testTag(tab.testTag),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight(),
        ) {
            val tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            val label = stringResource(tab.labelRes)
            if (tab.iconRes != null) {
                Icon(
                    painter = painterResource(id = tab.iconRes),
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
            } else if (tab.icon != null) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = label,
                style = TextStyle(
                    color = if (isActive) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
