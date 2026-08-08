package com.github.worn.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.worn.ui.theme.PhonePreview
import com.github.worn.ui.theme.TabletPreview
import com.github.worn.ui.theme.WornTheme

@Composable
fun PropertyRow(
    label: String,
    value: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = textStyle,
            fontWeight = FontWeight.Medium,
        )
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurface,
            style = textStyle,
            fontWeight = FontWeight.Medium,
        )
    }
}

@PhonePreview
@Composable
private fun PropertyRowPhonePreview() {
    WornTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PropertyRow(label = "Season", value = "Summer", textStyle = MaterialTheme.typography.bodyMedium)
            PropertyRow(label = "Fit", value = "Regular", textStyle = MaterialTheme.typography.bodyMedium)
        }
    }
}

@TabletPreview
@Composable
private fun PropertyRowTabletPreview() {
    WornTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PropertyRow(label = "Season", value = "Summer", textStyle = MaterialTheme.typography.bodyMedium)
            PropertyRow(label = "Fit", value = "Regular", textStyle = MaterialTheme.typography.bodyMedium)
        }
    }
}

