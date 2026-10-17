package com.vp.whattowatch.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vp.whattowatch.R
import com.vp.whattowatch.ui.theme.CinemaBorder
import com.vp.whattowatch.ui.theme.CinemaCardElevated
import com.vp.whattowatch.ui.theme.CinemaRed
import com.vp.whattowatch.ui.theme.CinemaTextPrimary
import com.vp.whattowatch.ui.theme.CinemaTextSecondary

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String = stringResource(R.string.action_delete),
    cancelLabel: String = stringResource(R.string.action_cancel),
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CinemaTextPrimary
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = CinemaTextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) CinemaRed else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = confirmLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, CinemaBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CinemaTextSecondary
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Text(text = cancelLabel)
            }
        },
        containerColor = CinemaCardElevated,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
    )
}
