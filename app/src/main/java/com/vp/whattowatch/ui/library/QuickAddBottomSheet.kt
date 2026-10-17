package com.vp.whattowatch.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.vp.whattowatch.R
import com.vp.whattowatch.ui.theme.CinemaBorder
import com.vp.whattowatch.ui.theme.CinemaCardElevated
import com.vp.whattowatch.ui.theme.CinemaGold
import com.vp.whattowatch.ui.theme.CinemaTextPrimary
import com.vp.whattowatch.ui.theme.CinemaTextSecondary
import com.vp.whattowatch.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    isSaving: Boolean,
    onSave: (title: String) -> Unit,
    onOpenFullEditor: (title: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CinemaCardElevated,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large)
                .padding(bottom = Spacing.xxLarge)
        ) {
            Text(
                text = stringResource(R.string.title_quick_add),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = CinemaGold
            )

            Spacer(modifier = Modifier.height(Spacing.regular))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) showError = false
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.field_title_hint),
                        color = CinemaTextSecondary
                    )
                },
                isError = showError,
                supportingText = if (showError) {
                    { Text(text = stringResource(R.string.field_title_required)) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (title.trim().isNotBlank()) {
                            onSave(title.trim())
                        } else {
                            showError = true
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CinemaGold,
                    unfocusedBorderColor = CinemaBorder,
                    focusedTextColor = CinemaTextPrimary,
                    unfocusedTextColor = CinemaTextPrimary,
                    cursorColor = CinemaGold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(Spacing.large))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                OutlinedButton(
                    onClick = {
                        onOpenFullEditor(title.trim())
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CinemaTextPrimary
                    ),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.action_more_details))
                }

                Button(
                    onClick = {
                        if (title.trim().isNotBlank()) {
                            onSave(title.trim())
                        } else {
                            showError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CinemaGold,
                        contentColor = Color(0xFF1E1300)
                    ),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
