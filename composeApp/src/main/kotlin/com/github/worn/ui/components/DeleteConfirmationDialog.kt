package com.github.worn.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.R
import com.github.worn.ui.theme.WornColors
import com.github.worn.ui.theme.WornTheme

@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            )
        },
        text = {
            Text(
                message,
                color = WornColors.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = WornColors.DeleteRed),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(
                    text = if (isDeleting) {
                        stringResource(R.string.common_deleting)
                    } else {
                        stringResource(R.string.common_delete)
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun DeleteConfirmationDialogPhonePreview() {
    WornTheme {
        DeleteConfirmationDialog(
            title = "Delete 3 items?",
            message = "This action cannot be undone.",
            isDeleting = false,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showSystemUi = true, device = "spec:width=800dp,height=1280dp,dpi=240")
@Composable
private fun DeleteConfirmationDialogTabletPreview() {
    WornTheme {
        DeleteConfirmationDialog(
            title = "Delete 3 items?",
            message = "This action cannot be undone.",
            isDeleting = false,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
