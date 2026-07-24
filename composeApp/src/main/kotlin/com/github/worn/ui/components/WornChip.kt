package com.github.worn.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.ui.theme.WornColors
import com.github.worn.ui.theme.WornTheme

private val chipShape = RoundedCornerShape(20.dp)

@Composable
fun WornChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = chipShape,
        color = if (isActive) WornColors.AccentGreen else WornColors.BgCard,
        border = if (isActive) null else BorderStroke(1.dp, WornColors.BorderSubtle),
        modifier = modifier,
    ) {
        Text(
            text = label,
            color = if (isActive) WornColors.TextOnColor else WornColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Preview(showSystemUi = true, device = "id:pixel_8")
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

@Preview(showSystemUi = true, device = "id:pixel_tablet")
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
