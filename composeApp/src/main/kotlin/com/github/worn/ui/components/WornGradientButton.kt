package com.github.worn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.ui.theme.WornColors
import com.github.worn.ui.theme.WornTheme

object WornGradients {
    val Save = listOf(WornColors.SaveGradientStart, WornColors.SaveGradientEnd)
    val Green = listOf(WornColors.AccentGreen, WornColors.AccentGreenDark)
    val GreenCta = listOf(WornColors.AccentGreen, WornColors.AccentGreenEnd)
    val Indigo = listOf(WornColors.AccentIndigo, Color(0xFF556070))
    val Disabled = listOf(WornColors.TextMuted, WornColors.IconMuted)
}

@Composable
fun WornGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradientColors: List<Color> = WornGradients.Save,
    disabledGradientColors: List<Color> = WornGradients.Disabled,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: Dp = 0.dp,
    icon: (@Composable () -> Unit)? = null,
    fillMaxWidth: Boolean = true,
    fixedHeight: Dp? = 52.dp,
    contentPadding: PaddingValues? = null,
) {
    val gradient = Brush.verticalGradient(if (enabled) gradientColors else disabledGradientColors)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        contentPadding = PaddingValues(),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = elevation),
        modifier = modifier.then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .then(if (fixedHeight != null) Modifier.height(fixedHeight) else Modifier),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .then(if (fillMaxWidth) Modifier.fillMaxSize() else Modifier)
                .background(gradient, shape)
                .then(
                    if (contentPadding != null) {
                        Modifier.padding(contentPadding)
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (icon != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    icon()
                    Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun WornGradientButtonPhonePreview() {
    WornTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            WornGradientButton(text = "Save to Wardrobe", onClick = {})
        }
    }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun WornGradientButtonTabletPreview() {
    WornTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            WornGradientButton(text = "Save to Wardrobe", onClick = {})
        }
    }
}
