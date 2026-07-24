package com.github.worn.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.R
import com.github.worn.ui.theme.WornColors
import com.github.worn.ui.theme.WornTheme

@Composable
fun SelectionHeader(
    count: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pluralStringResource(R.plurals.selected_count, count, count),
                color = WornColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.8).sp,
            )
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = WornColors.DeleteRed),
                shape = RoundedCornerShape(22.dp),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.common_delete),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.common_cancel),
            color = WornColors.TextSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onCancel),
        )
    }
}

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun SelectionHeaderPhonePreview() {
    WornTheme {
        SelectionHeader(count = 3, onCancel = {}, onDelete = {})
    }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun SelectionHeaderTabletPreview() {
    WornTheme {
        SelectionHeader(count = 5, onCancel = {}, onDelete = {})
    }
}
