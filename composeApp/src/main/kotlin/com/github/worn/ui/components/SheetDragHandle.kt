package com.github.worn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.worn.ui.theme.PhonePreview
import com.github.worn.ui.theme.TabletPreview
import com.github.worn.ui.theme.WornTheme
import com.github.worn.ui.theme.wornExtras

@Composable
fun SheetDragHandle(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.wornExtras.iconMuted,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
    }
}

@PhonePreview
@Composable
private fun SheetDragHandlePhonePreview() {
    WornTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            SheetDragHandle()
        }
    }
}

@TabletPreview
@Composable
private fun SheetDragHandleTabletPreview() {
    WornTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            SheetDragHandle()
        }
    }
}

