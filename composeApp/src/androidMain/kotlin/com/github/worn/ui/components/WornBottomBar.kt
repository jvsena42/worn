package com.github.worn.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.R
import com.github.worn.ui.theme.WornColors

enum class Tab(
    val label: String,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null,
) {
    WARDROBE("WARDROBE", iconRes = R.drawable.ic_shirt),
    OUTFITS("OUTFITS", icon = Icons.Outlined.Layers),
    GAPS("GAPS", icon = Icons.Outlined.Extension),
    TRY_IT("TRY IT", icon = Icons.Outlined.QrCodeScanner),
    SETTINGS("SETTINGS", icon = Icons.Outlined.Settings),
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
                start = if (isCompact) 21.dp else 32.dp,
                end = if (isCompact) 21.dp else 32.dp,
                top = 12.dp,
                bottom = 21.dp,
            ),
    ) {
        Surface(
            shape = RoundedCornerShape(36.dp),
            color = WornColors.BgElevated,
            border = BorderStroke(1.dp, WornColors.BorderSubtle),
            modifier = Modifier
                .then(
                    if (isCompact) Modifier.fillMaxWidth()
                    else Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                )
                .height(62.dp),
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
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = if (isActive) WornColors.AccentGreen else WornColors.BgElevated,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight(),
        ) {
            val tint = if (isActive) WornColors.TextOnColor else WornColors.TextSecondary
            if (tab.iconRes != null) {
                Icon(
                    painter = painterResource(id = tab.iconRes),
                    contentDescription = tab.label,
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
            } else if (tab.icon != null) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = tab.label,
                color = if (isActive) WornColors.TextOnColor else WornColors.TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
        }
    }
}
