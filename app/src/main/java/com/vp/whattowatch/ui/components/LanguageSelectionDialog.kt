package com.vp.whattowatch.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vp.whattowatch.R
import com.vp.whattowatch.data.preference.LanguagePreferenceManager
import com.vp.whattowatch.ui.theme.CinemaBorder
import com.vp.whattowatch.ui.theme.CinemaCard
import com.vp.whattowatch.ui.theme.CinemaCardElevated
import com.vp.whattowatch.ui.theme.CinemaGold
import com.vp.whattowatch.ui.theme.CinemaTextPrimary
import com.vp.whattowatch.ui.theme.CinemaTextSecondary
import com.vp.whattowatch.ui.theme.Spacing

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_language_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CinemaTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                LanguagePreferenceManager.supportedLanguages.forEach { option ->
                    val isSelected = currentLanguage == option.tag
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) CinemaGold.copy(alpha = 0.12f) else CinemaCard,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) CinemaGold else CinemaBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLanguageSelected(option.tag)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onLanguageSelected(option.tag) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CinemaGold,
                                    unselectedColor = CinemaTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(Spacing.small))
                            Text(
                                text = stringResource(id = option.titleRes),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (isSelected) CinemaGold else CinemaTextPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, CinemaBorder),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = CinemaTextSecondary
                )
            }
        },
        containerColor = CinemaCardElevated,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
    )
}
