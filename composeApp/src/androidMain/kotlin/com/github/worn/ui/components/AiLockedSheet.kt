package com.github.worn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.ui.theme.SheetPreview
import com.github.worn.ui.theme.WornColors

private val IndigoAccent = Color(0xFF6B7B8E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiLockedSheet(onDismiss: () -> Unit, onGoToSettings: () -> Unit = {}) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WornColors.BgElevated,
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
        dragHandle = null,
    ) {
        AiLockedContent(onDismiss = onDismiss, onGoToSettings = onGoToSettings)
    }
}

@Composable
internal fun AiLockedContent(
    onDismiss: () -> Unit = {},
    onGoToSettings: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 24.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Dismiss",
                    tint = WornColors.IconMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

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
    val gradient = Brush.verticalGradient(listOf(Color(0xFF7A9468), Color(0xFF5C6E50)))
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
