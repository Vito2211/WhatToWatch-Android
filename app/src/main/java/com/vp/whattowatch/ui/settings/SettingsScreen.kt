package com.vp.whattowatch.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vp.whattowatch.R
import com.vp.whattowatch.data.backup.ConflictResolutionStrategy
import com.vp.whattowatch.data.backup.RestoreMode
import com.vp.whattowatch.data.backup.RestorePreview
import com.vp.whattowatch.data.preference.LanguagePreferenceManager
import com.vp.whattowatch.ui.components.LanguageSelectionDialog
import com.vp.whattowatch.ui.theme.CinemaBlack
import com.vp.whattowatch.ui.theme.CinemaBorder
import com.vp.whattowatch.ui.theme.CinemaCard
import com.vp.whattowatch.ui.theme.CinemaCardElevated
import com.vp.whattowatch.ui.theme.CinemaGold
import com.vp.whattowatch.ui.theme.CinemaTextPrimary
import com.vp.whattowatch.ui.theme.CinemaTextSecondary
import com.vp.whattowatch.ui.theme.Spacing
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onLanguageChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // SAF Create Document launcher for ZIP export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackup(uri)
        }
    }

    // SAF Open Document launcher for ZIP restore
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.inspectBackup(uri)
        }
    }

    // Handle snackbar notifications
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.cd_settings),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CinemaTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = CinemaTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CinemaBlack)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.regular),
            verticalArrangement = Arrangement.spacedBy(Spacing.large)
        ) {
            // Busy progress indicator banner
            if (uiState.isBusy) {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, CinemaGold),
                    colors = CardDefaults.cardColors(containerColor = CinemaCardElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.regular),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.regular)
                    ) {
                        CircularProgressIndicator(
                            color = CinemaGold,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        val text = when {
                            uiState.isExporting -> stringResource(R.string.exporting_backup)
                            uiState.isInspecting -> stringResource(R.string.inspecting_backup)
                            uiState.isRestoring -> stringResource(R.string.restoring_backup)
                            else -> ""
                        }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CinemaTextPrimary
                        )
                    }
                }
            }

            // Section 1: Appearance & Language
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xSmall)) {
                Text(
                    text = stringResource(R.string.settings_appearance),
                    style = MaterialTheme.typography.titleSmall,
                    color = CinemaGold
                )

                Surface(
                    onClick = { viewModel.onOpenLanguageDialog() },
                    shape = MaterialTheme.shapes.medium,
                    color = CinemaCard,
                    border = BorderStroke(1.dp, CinemaBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.regular),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = CinemaGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.regular))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_language),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = CinemaTextPrimary
                            )
                            val currentOption = LanguagePreferenceManager.supportedLanguages.find {
                                it.tag == uiState.currentLanguage
                            } ?: LanguagePreferenceManager.supportedLanguages.first()
                            Text(
                                text = stringResource(currentOption.titleRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = CinemaTextSecondary
                            )
                        }
                    }
                }
            }

            // Section 2: Library Backup & Restore
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xSmall)) {
                Text(
                    text = stringResource(R.string.settings_backup_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = CinemaGold
                )

                Card(
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, CinemaBorder),
                    colors = CardDefaults.cardColors(containerColor = CinemaCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.regular),
                        verticalArrangement = Arrangement.spacedBy(Spacing.regular)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_backup_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = CinemaTextSecondary
                        )

                        // Export Button
                        Button(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                exportLauncher.launch("whattowatch_backup_$timestamp.zip")
                            },
                            enabled = !uiState.isBusy,
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CinemaGold,
                                contentColor = Color(0xFF1E1300)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Archive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.small))
                            Text(
                                text = stringResource(R.string.action_export_backup),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // Restore Button
                        OutlinedButton(
                            onClick = {
                                restoreLauncher.launch(
                                    arrayOf(
                                        "application/zip",
                                        "application/x-zip-compressed",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            },
                            enabled = !uiState.isBusy,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, CinemaGold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CinemaGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SettingsBackupRestore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.small))
                            Text(
                                text = stringResource(R.string.action_restore_backup),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }

    // In-App Language Selection Dialog
    if (uiState.showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = uiState.currentLanguage,
            onLanguageSelected = { tag ->
                viewModel.onLanguageSelected(tag)
                onLanguageChanged()
            },
            onDismiss = { viewModel.onDismissLanguageDialog() }
        )
    }

    // Restore Preview & Confirmation Dialog
    uiState.restorePreview?.let { preview ->
        RestorePreviewDialog(
            preview = preview,
            selectedMode = uiState.selectedRestoreMode,
            selectedStrategy = uiState.selectedConflictStrategy,
            onModeSelected = { viewModel.onRestoreModeSelected(it) },
            onStrategySelected = { viewModel.onConflictStrategySelected(it) },
            onConfirm = { viewModel.confirmRestore() },
            onDismiss = { viewModel.dismissRestorePreview() }
        )
    }
}

@Composable
private fun RestorePreviewDialog(
    preview: RestorePreview,
    selectedMode: RestoreMode,
    selectedStrategy: ConflictResolutionStrategy,
    onModeSelected: (RestoreMode) -> Unit,
    onStrategySelected: (ConflictResolutionStrategy) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.restore_preview_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CinemaTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                if (!preview.isValid) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(modifier = Modifier.padding(Spacing.medium)) {
                            preview.validationErrors.forEach { errorMsg ->
                                Text(
                                    text = "• $errorMsg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                } else {
                    // Archive Metadata Details
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = CinemaCard,
                        border = BorderStroke(1.dp, CinemaBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.medium),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xxSmall)
                        ) {
                            val manifest = preview.manifest
                            if (manifest != null) {
                                val formattedDate = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                    .format(Date(manifest.createdAt))
                                Text(
                                    text = stringResource(R.string.restore_preview_date, formattedDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CinemaTextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.restore_preview_version, manifest.version),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CinemaTextSecondary
                                )
                                Text(
                                    text = stringResource(
                                        R.string.restore_preview_summary,
                                        manifest.itemCount,
                                        manifest.listCount,
                                        manifest.categoryCount,
                                        preview.imageFileCount
                                    ),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = CinemaGold
                                )
                            }
                        }
                    }

                    // Restore Mode Selection
                    Text(
                        text = stringResource(R.string.restore_mode_title),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = CinemaTextPrimary
                    )

                    // Mode: Merge
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (selectedMode == RestoreMode.MERGE) CinemaGold.copy(alpha = 0.12f) else CinemaCard,
                        border = BorderStroke(1.dp, if (selectedMode == RestoreMode.MERGE) CinemaGold else CinemaBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelected(RestoreMode.MERGE) }
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == RestoreMode.MERGE,
                                onClick = { onModeSelected(RestoreMode.MERGE) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CinemaGold,
                                    unselectedColor = CinemaTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(Spacing.xSmall))
                            Column {
                                Text(
                                    text = stringResource(R.string.restore_mode_merge),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = CinemaTextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.restore_mode_merge_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CinemaTextSecondary
                                )
                            }
                        }
                    }

                    // Mode: Replace
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (selectedMode == RestoreMode.REPLACE) CinemaGold.copy(alpha = 0.12f) else CinemaCard,
                        border = BorderStroke(1.dp, if (selectedMode == RestoreMode.REPLACE) CinemaGold else CinemaBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelected(RestoreMode.REPLACE) }
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == RestoreMode.REPLACE,
                                onClick = { onModeSelected(RestoreMode.REPLACE) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CinemaGold,
                                    unselectedColor = CinemaTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(Spacing.xSmall))
                            Column {
                                Text(
                                    text = stringResource(R.string.restore_mode_replace),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = CinemaTextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.restore_mode_replace_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CinemaTextSecondary
                                )
                            }
                        }
                    }

                    // Warning if Replace mode selected
                    if (selectedMode == RestoreMode.REPLACE) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.small),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = stringResource(R.string.restore_replace_warning),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Conflicts & Strategy if Merge mode selected and conflicts exist
                    if (selectedMode == RestoreMode.MERGE && preview.conflicts.isNotEmpty()) {
                        Card(
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, CinemaBorder),
                            colors = CardDefaults.cardColors(containerColor = CinemaCardElevated)
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.small),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xSmall)
                            ) {
                                Text(
                                    text = stringResource(R.string.restore_conflicts_title, preview.conflicts.size),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = CinemaGold
                                )
                                Text(
                                    text = stringResource(R.string.restore_conflicts_desc, preview.conflicts.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CinemaTextSecondary
                                )
                                Spacer(modifier = Modifier.height(Spacing.xxSmall))
                                Text(
                                    text = stringResource(R.string.restore_conflict_strategy),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = CinemaTextPrimary
                                )

                                // Strategy 1: Keep Both
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onStrategySelected(ConflictResolutionStrategy.KEEP_BOTH_IMPORT_AS_COPY) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedStrategy == ConflictResolutionStrategy.KEEP_BOTH_IMPORT_AS_COPY,
                                        onClick = { onStrategySelected(ConflictResolutionStrategy.KEEP_BOTH_IMPORT_AS_COPY) },
                                        colors = RadioButtonDefaults.colors(selectedColor = CinemaGold)
                                    )
                                    Text(
                                        text = stringResource(R.string.conflict_keep_both),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CinemaTextPrimary
                                    )
                                }

                                // Strategy 2: Overwrite
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onStrategySelected(ConflictResolutionStrategy.OVERWRITE_EXISTING) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedStrategy == ConflictResolutionStrategy.OVERWRITE_EXISTING,
                                        onClick = { onStrategySelected(ConflictResolutionStrategy.OVERWRITE_EXISTING) },
                                        colors = RadioButtonDefaults.colors(selectedColor = CinemaGold)
                                    )
                                    Text(
                                        text = stringResource(R.string.conflict_overwrite),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CinemaTextPrimary
                                    )
                                }

                                // Strategy 3: Skip
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onStrategySelected(ConflictResolutionStrategy.SKIP_EXISTING) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedStrategy == ConflictResolutionStrategy.SKIP_EXISTING,
                                        onClick = { onStrategySelected(ConflictResolutionStrategy.SKIP_EXISTING) },
                                        colors = RadioButtonDefaults.colors(selectedColor = CinemaGold)
                                    )
                                    Text(
                                        text = stringResource(R.string.conflict_skip),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CinemaTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (preview.isValid) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMode == RestoreMode.REPLACE) MaterialTheme.colorScheme.error else CinemaGold,
                        contentColor = if (selectedMode == RestoreMode.REPLACE) MaterialTheme.colorScheme.onError else Color(0xFF1E1300)
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = stringResource(R.string.action_confirm_restore),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, CinemaBorder)
            ) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = CinemaTextSecondary
                )
            }
        },
        containerColor = CinemaCardElevated
    )
}
