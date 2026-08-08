package com.github.worn.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.worn.ui.theme.PhonePreview
import com.github.worn.ui.theme.TabletPreview
import com.github.worn.ui.theme.WornTheme

@Composable
fun SelectionIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    iconSize: Dp = 16.dp,
) {
    Surface(
        shape = RoundedCornerShape(size / 2),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
        border = if (isSelected) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.size(size),
    ) {
        if (isSelected) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}

@PhonePreview
@Composable
private fun SelectionIndicatorPhonePreview() {
    WornTheme {
        Row(modifier = Modifier.size(200.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(16.dp))
            SelectionIndicator(isSelected = false)
            Spacer(Modifier.width(16.dp))
            SelectionIndicator(isSelected = true)
            Spacer(Modifier.width(16.dp))
            SelectionIndicator(isSelected = false, size = 20.dp, iconSize = 12.dp)
            Spacer(Modifier.width(16.dp))
            SelectionIndicator(isSelected = true, size = 20.dp, iconSize = 12.dp)
        }
    }
}

@TabletPreview
@Composable
private fun SelectionIndicatorTabletPreview() {
    WornTheme {
        Row(modifier = Modifier.size(200.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(16.dp))
            SelectionIndicator(isSelected = false)
            Spacer(Modifier.width(16.dp))
            SelectionIndicator(isSelected = true)
        }
    }
}

