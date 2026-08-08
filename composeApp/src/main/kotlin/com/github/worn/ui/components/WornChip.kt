@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.github.worn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.ui.theme.PhonePreview
import com.github.worn.ui.theme.TabletPreview
import com.github.worn.ui.theme.WornTheme

private val chipShape: Shape
    @Composable @ReadOnlyComposable get() = MaterialTheme.shapes.largeIncreased

@Composable
fun WornChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    // Filtering swaps the whole grid underneath, so easing the chip's own fill gives the eye
    // something continuous to hold on to; snapping both at once reads as a flash.
    val containerColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        label = "chipContainer",
    )
    val labelColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "chipLabel",
    )

    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onClick()
        },
        shape = chipShape,
        color = containerColor,
        border = if (isActive) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Text(
            text = label,
            color = labelColor,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@PhonePreview
@Composable
private fun WornChipPhonePreview() {
    WornTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            WornChip(label = "Summer", isActive = false, onClick = {})
            Spacer(Modifier.width(8.dp))
            WornChip(label = "Winter", isActive = true, onClick = {})
        }
    }
}

@TabletPreview
@Composable
private fun WornChipTabletPreview() {
    WornTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            WornChip(label = "Summer", isActive = false, onClick = {})
            Spacer(Modifier.width(8.dp))
            WornChip(label = "Winter", isActive = true, onClick = {})
        }
    }
}


