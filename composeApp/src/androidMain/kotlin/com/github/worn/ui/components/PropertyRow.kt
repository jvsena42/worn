package com.github.worn.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.ui.theme.WornColors
import com.github.worn.ui.theme.WornTheme

@Composable
fun PropertyRow(
    label: String,
    value: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = WornColors.TextSecondary, fontSize = fontSize, fontWeight = FontWeight.Medium)
        Text(value, color = WornColors.TextPrimary, fontSize = fontSize, fontWeight = FontWeight.Medium)
    }
}

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun PropertyRowPhonePreview() {
    WornTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PropertyRow(label = "Season", value = "Summer", fontSize = 15.sp)
            PropertyRow(label = "Fit", value = "Regular", fontSize = 15.sp)
        }
    }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun PropertyRowTabletPreview() {
    WornTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PropertyRow(label = "Season", value = "Summer", fontSize = 15.sp)
            PropertyRow(label = "Fit", value = "Regular", fontSize = 15.sp)
        }
    }
}
