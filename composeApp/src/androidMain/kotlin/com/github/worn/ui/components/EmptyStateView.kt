package com.github.worn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.ui.theme.WornColors
import com.github.worn.ui.theme.WornTheme

@Composable
fun EmptyStateView(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(130.dp)
                .shadow(15.dp, CircleShape)
                .background(WornColors.BgCard, CircleShape)
                .border(1.dp, WornColors.BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(Modifier.height(24.dp))
        Text(
            title,
            color = WornColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            description,
            color = WornColors.TextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(24.dp))
            action()
        }
    }
}

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun EmptyStatePhonePreview() {
    WornTheme {
        EmptyStateView(
            icon = {
                Text("👕", fontSize = 42.sp)
            },
            title = "Your wardrobe is empty",
            description = "Add your first item to get started",
        )
    }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun EmptyStateTabletPreview() {
    WornTheme {
        EmptyStateView(
            icon = {
                Text("👕", fontSize = 42.sp)
            },
            title = "Your wardrobe is empty",
            description = "Add your first item to get started",
        )
    }
}
