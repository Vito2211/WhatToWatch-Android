package com.vp.whattowatch.ui.sharing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vp.whattowatch.R
import com.vp.whattowatch.data.sharing.ImportConflict
import com.vp.whattowatch.data.sharing.ImportConflictResolution
import com.vp.whattowatch.data.sharing.ShareContentItem
import com.vp.whattowatch.data.sharing.ShareType
import com.vp.whattowatch.domain.model.ContentType
import com.vp.whattowatch.domain.model.WatchStatus
import com.vp.whattowatch.ui.components.getContentTypeIcon
import com.vp.whattowatch.ui.components.getContentTypeLabel
import com.vp.whattowatch.ui.theme.CinemaBlack
import com.vp.whattowatch.ui.theme.CinemaBorder
import com.vp.whattowatch.ui.theme.CinemaCard
import com.vp.whattowatch.ui.theme.CinemaCardElevated
import com.vp.whattowatch.ui.theme.CinemaGold
import com.vp.whattowatch.ui.theme.CinemaTextPrimary
import com.vp.whattowatch.ui.theme.CinemaTextSecondary
import com.vp.whattowatch.ui.theme.Spacing
import com.vp.whattowatch.ui.theme.StatusNotWatchedColor
import com.vp.whattowatch.ui.theme.StatusWatchedColor
import com.vp.whattowatch.ui.theme.StatusWatchingColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(
    viewModel: ImportPreviewViewModel,
    onNavigateBack: () -> Unit,
    onImportSuccess: (count: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_import_preview),
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
        bottomBar = {
            if (uiState.preview != null && uiState.preview!!.isValid) {
                Surface(
                    color = CinemaCardElevated,
                    border = BorderStroke(1.dp, CinemaBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.regular, vertical = Spacing.small),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, CinemaBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.action_cancel_import),
                                color = CinemaTextSecondary
                            )
                        }

                        Button(
                            onClick = { viewModel.confirmImport(onImportSuccess) },
                            enabled = uiState.canImport,
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CinemaGold,
                                contentColor = Color(0xFF1E1300)
                            ),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            if (uiState.isImporting) {
                                CircularProgressIndicator(
                                    color = Color(0xFF1E1300),
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "${stringResource(R.string.action_confirm_import)} (${uiState.selectedCount})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        CircularProgressIndicator(color = CinemaGold)
                        Text(
                            text = stringResource(R.string.import_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CinemaTextSecondary
                        )
                    }
                }
            }

            uiState.errorMessage != null && uiState.preview == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(Spacing.regular),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.large),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = uiState.errorMessage ?: stringResource(R.string.import_invalid_link),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(
                                onClick = onNavigateBack,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onErrorContainer)
                            ) {
                                Text(text = stringResource(R.string.action_cancel_import))
                            }
                        }
                    }
                }
            }

            uiState.preview != null -> {
                val preview = uiState.preview!!
                val payload = preview.payload!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(Spacing.regular),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    // Header Card
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, CinemaBorder),
                            colors = CardDefaults.cardColors(containerColor = CinemaCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.regular),
                                verticalArrangement = Arrangement.spacedBy(Spacing.small)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                                ) {
                                    Surface(
                                        color = CinemaGold.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = if (payload.type == ShareType.CUSTOM_LIST) {
                                                stringResource(R.string.import_source_list)
                                            } else {
                                                stringResource(R.string.import_source_item)
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = CinemaGold,
                                            modifier = Modifier.padding(horizontal = Spacing.small, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = payload.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CinemaTextPrimary
                                )

                                Text(
                                    text = stringResource(R.string.items_count, payload.contents.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CinemaTextSecondary
                                )

                                if (payload.categories.isNotEmpty()) {
                                    Text(
                                        text = stringResource(R.string.import_categories_summary, payload.categories.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CinemaTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Conflict resolution card if conflicts detected
                    if (preview.conflicts.isNotEmpty()) {
                        item {
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.6f)),
                                colors = CardDefaults.cardColors(containerColor = CinemaCardElevated),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(Spacing.regular),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.xSmall)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.WarningAmber,
                                            contentDescription = null,
                                            tint = CinemaGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.import_conflicts_detected, preview.conflicts.size),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = CinemaGold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(Spacing.xxSmall))
                                    Text(
                                        text = stringResource(R.string.import_conflict_header),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = CinemaTextPrimary
                                    )

                                    // Resolution 1: Skip
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.onConflictResolutionChanged(ImportConflictResolution.SKIP_EXISTING) }
                                    ) {
                                        RadioButton(
                                            selected = uiState.conflictResolution == ImportConflictResolution.SKIP_EXISTING,
                                            onClick = { viewModel.onConflictResolutionChanged(ImportConflictResolution.SKIP_EXISTING) },
                                            colors = RadioButtonDefaults.colors(selectedColor = CinemaGold)
                                        )
                                        Text(
                                            text = stringResource(R.string.import_conflict_skip),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CinemaTextPrimary
                                        )
                                    }

                                    // Resolution 2: Keep Both (Copy)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.onConflictResolutionChanged(ImportConflictResolution.KEEP_BOTH_AS_COPY) }
                                    ) {
                                        RadioButton(
                                            selected = uiState.conflictResolution == ImportConflictResolution.KEEP_BOTH_AS_COPY,
                                            onClick = { viewModel.onConflictResolutionChanged(ImportConflictResolution.KEEP_BOTH_AS_COPY) },
                                            colors = RadioButtonDefaults.colors(selectedColor = CinemaGold)
                                        )
                                        Text(
                                            text = stringResource(R.string.import_conflict_copy),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CinemaTextPrimary
                                        )
                                    }

                                    // Resolution 3: Overwrite
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.onConflictResolutionChanged(ImportConflictResolution.OVERWRITE_EXISTING) }
                                    ) {
                                        RadioButton(
                                            selected = uiState.conflictResolution == ImportConflictResolution.OVERWRITE_EXISTING,
                                            onClick = { viewModel.onConflictResolutionChanged(ImportConflictResolution.OVERWRITE_EXISTING) },
                                            colors = RadioButtonDefaults.colors(selectedColor = CinemaGold)
                                        )
                                        Text(
                                            text = stringResource(R.string.import_conflict_overwrite),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CinemaTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Content selection header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.import_contents_header, payload.contents.size),
                                style = MaterialTheme.typography.titleSmall,
                                color = CinemaGold
                            )

                            TextButton(
                                onClick = {
                                    if (uiState.areAllSelected) {
                                        viewModel.onDeselectAll()
                                    } else {
                                        viewModel.onSelectAll()
                                    }
                                }
                            ) {
                                Text(
                                    text = if (uiState.areAllSelected) {
                                        stringResource(R.string.import_deselect_all)
                                    } else {
                                        stringResource(R.string.import_select_all)
                                    },
                                    color = CinemaGold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    // Content items list
                    items(
                        items = payload.contents,
                        key = { it.id }
                    ) { item ->
                        val isChecked = uiState.selectedItemIds.contains(item.id)
                        val conflict = preview.conflicts.find { it.incomingItem.id == item.id }
                        ImportContentItemRow(
                            item = item,
                            isChecked = isChecked,
                            conflict = conflict,
                            onToggle = { viewModel.onToggleItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportContentItemRow(
    item: ShareContentItem,
    isChecked: Boolean,
    conflict: ImportConflict?,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, if (isChecked) CinemaGold.copy(alpha = 0.5f) else CinemaBorder),
        colors = CardDefaults.cardColors(containerColor = CinemaCard),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = CinemaGold,
                    checkmarkColor = Color(0xFF1E1300),
                    uncheckedColor = CinemaTextSecondary
                )
            )

            Spacer(modifier = Modifier.width(Spacing.small))

            val contentType = ContentType.fromString(item.contentType)
            Icon(
                imageVector = getContentTypeIcon(contentType),
                contentDescription = null,
                tint = CinemaGold,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(Spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = CinemaTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getContentTypeLabel(contentType),
                        style = MaterialTheme.typography.bodySmall,
                        color = CinemaTextSecondary
                    )

                    val status = WatchStatus.fromString(item.watchStatus)
                    val statusColor = when (status) {
                        WatchStatus.WATCHED -> StatusWatchedColor
                        WatchStatus.WATCHING -> StatusWatchingColor
                        WatchStatus.NOT_WATCHED -> StatusNotWatchedColor
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(statusColor, CircleShape)
                    )
                }

                if (conflict != null) {
                    Text(
                        text = "Matches local item: \"${conflict.existingItemTitle}\"",
                        style = MaterialTheme.typography.labelSmall,
                        color = CinemaGold.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
