package com.github.worn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.R
import com.github.worn.ui.theme.WornTheme

@Composable
fun ErrorContentView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryButtonColor: Color = MaterialTheme.colorScheme.primary,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Surface(
            onClick = onRetry,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Text(
                text = stringResource(R.string.common_retry),
                color = retryButtonColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun ErrorContentViewPhonePreview() {
    WornTheme {
        ErrorContentView(
            message = "Invalid API key. Check your key in Settings.",
            onRetry = {},
            modifier = Modifier.padding(vertical = 60.dp),
        )
    }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun ErrorContentViewTabletPreview() {
    WornTheme {
        ErrorContentView(
            message = "Something went wrong. Please try again.",
            onRetry = {},
            modifier = Modifier.padding(vertical = 60.dp),
            retryButtonColor = MaterialTheme.colorScheme.secondary,
        )
    }
}
