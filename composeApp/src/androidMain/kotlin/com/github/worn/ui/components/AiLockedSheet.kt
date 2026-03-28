package com.github.worn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.ui.theme.SheetPreview
import com.github.worn.ui.theme.WornColors

private val IndigoAccent = WornColors.AccentIndigo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiLockedSheet(onDismiss: () -> Unit, onGoToSettings: () -> Unit = {}) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WornColors.BgElevated,
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
        dragHandle = { SheetHandle() },
    ) {
        AiLockedContent(onGoToSettings = onGoToSettings, onDismiss = onDismiss)
    }
}

@Composable
private fun SheetHandle() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(WornColors.IconMuted),
        )
    }
}

@Composable
internal fun AiLockedContent(
    onGoToSettings: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .background(IndigoAccent, RoundedCornerShape(12.dp)),
        ) {
            Icon(
                Icons.Outlined.SmartToy,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        Text(
            text = "Unlock AI features",
            color = WornColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
        )

        Text(
            text = "Add your Claude API key in Settings to enable this.",
            color = WornColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
            modifier = Modifier.widthIn(max = 280.dp),
        )

        SettingsCta(onClick = {
            onGoToSettings()
            onDismiss()
        })
    }
}

@Composable
private fun SettingsCta(onClick: () -> Unit) {
    val gradient = Brush.verticalGradient(listOf(WornColors.AccentGreen, WornColors.AccentGreenDark))
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(gradient, RoundedCornerShape(16.dp))
                .padding(horizontal = 40.dp, vertical = 14.dp),
        ) {
            Text(
                text = "Go to Settings",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun AiLockedPhonePreview() {
    SheetPreview { AiLockedContent() }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun AiLockedTabletPreview() {
    SheetPreview { AiLockedContent() }
}
