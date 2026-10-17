package com.vp.whattowatch.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.vp.whattowatch.data.local.image.ImageStorageManager
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vp.whattowatch.R
import com.vp.whattowatch.domain.model.WatchStatus
import com.vp.whattowatch.ui.components.ConfirmationDialog
import com.vp.whattowatch.ui.components.getContentTypeIcon
import com.vp.whattowatch.ui.components.getContentTypeLabel
import com.vp.whattowatch.ui.theme.CinemaBlack
import com.vp.whattowatch.ui.theme.CinemaBorder
import com.vp.whattowatch.ui.theme.CinemaCard
import com.vp.whattowatch.ui.theme.CinemaCardElevated
import com.vp.whattowatch.ui.theme.CinemaGold
import com.vp.whattowatch.ui.theme.CinemaRed
import com.vp.whattowatch.ui.theme.CinemaTextPrimary
import com.vp.whattowatch.ui.theme.CinemaTextSecondary
import com.vp.whattowatch.ui.theme.Spacing
import com.vp.whattowatch.ui.theme.StatusNotWatchedColor
import com.vp.whattowatch.ui.theme.StatusWatchedColor
import com.vp.whattowatch.ui.theme.StatusWatchingColor
import com.vp.whattowatch.WhatToWatchApp
import com.vp.whattowatch.ui.sharing.SharingHelper
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContentDetailScreen(
    viewModel: ContentDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (contentId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val item = uiState.contentItem

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_content_detail),
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
                actions = {
                    if (item != null) {
                        // Share button
                        IconButton(onClick = {
                            coroutineScope.launch {
                                val app = context.applicationContext as? WhatToWatchApp
                                val sharingService = app?.container?.sharingService
                                if (sharingService != null) {
                                    SharingHelper.shareContentItem(
                                        context = context,
                                        sharingService = sharingService,
                                        contentId = item.id,
                                        onNotice = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                    )
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.cd_share),
                                tint = CinemaGold
                            )
                        }

                        // Edit button
                        IconButton(onClick = { onNavigateToEdit(item.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.action_edit),
                                tint = CinemaGold
                            )
                        }

                        // Delete button
                        IconButton(onClick = { viewModel.onOpenDeleteDialog() }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = CinemaRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CinemaBlack)
            )
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
                    CircularProgressIndicator(color = CinemaGold)
                }
            }

            item == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.content_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = CinemaTextSecondary
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.regular)
                ) {
                    // Hero Poster (2:3 Aspect Ratio)
                    val posterFile = remember(item.posterPath) {
                        item.posterPath?.let { ImageStorageManager.getPosterFile(context, it) }
                    }
                    val hasValidPoster = posterFile != null && posterFile.exists() && posterFile.length() > 0

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .border(BorderStroke(1.dp, CinemaBorder), MaterialTheme.shapes.large)
                            .clip(MaterialTheme.shapes.large)
                            .background(
                                Brush.verticalGradient(listOf(Color(0xFF20232E), CinemaCardElevated, CinemaBlack))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasValidPoster) {
                            AsyncImage(
                                model = posterFile,
                                contentDescription = stringResource(R.string.cd_poster_placeholder),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = getContentTypeIcon(item.contentType),
                                contentDescription = stringResource(R.string.cd_poster_placeholder),
                                tint = CinemaGold.copy(alpha = 0.5f),
                                modifier = Modifier.size(72.dp)
                            )
                        }

                        // Content Type Badge
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = CinemaBlack.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(Spacing.medium)
                        ) {
                            Text(
                                text = getContentTypeLabel(item.contentType).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CinemaGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Bottom subtle gradient scrim
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, CinemaBlack.copy(alpha = 0.7f))
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.large))

                    // Title
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = CinemaTextPrimary
                    )

                    // Added timestamp
                    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    val formattedDate = dateFormat.format(Date(item.createdAt))
                    Text(
                        text = stringResource(R.string.added_on, formattedDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = CinemaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(Spacing.large))

                    // Status Switcher Row
                    Text(
                        text = stringResource(R.string.action_change_status),
                        style = MaterialTheme.typography.titleSmall,
                        color = CinemaGold
                    )
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        val statuses = listOf(
                            Triple(WatchStatus.NOT_WATCHED, R.string.status_not_watched, StatusNotWatchedColor),
                            Triple(WatchStatus.WATCHING, R.string.status_watching, StatusWatchingColor),
                            Triple(WatchStatus.WATCHED, R.string.status_watched, StatusWatchedColor)
                        )

                        statuses.forEach { (status, labelRes, color) ->
                            val isSelected = item.watchStatus == status
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onUpdateWatchStatus(status) },
                                label = { Text(text = stringResource(labelRes)) },
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    containerColor = CinemaCard,
                                    labelColor = CinemaTextSecondary,
                                    selectedContainerColor = color.copy(alpha = 0.25f),
                                    selectedLabelColor = color
                                ),
                                border = BorderStroke(1.dp, if (isSelected) color else CinemaBorder),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // External URL Button
                    if (!item.externalUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(Spacing.large))
                        Button(
                            onClick = {
                                try {
                                    val uri = Uri.parse(item.externalUrl)
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CinemaCardElevated,
                                contentColor = CinemaGold
                            ),
                            border = BorderStroke(1.dp, CinemaBorder),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.small))
                            Text(
                                text = item.externalUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Description / Personal Notes
                    if (!item.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(Spacing.large))
                        Text(
                            text = stringResource(R.string.field_description),
                            style = MaterialTheme.typography.titleSmall,
                            color = CinemaGold
                        )
                        Spacer(modifier = Modifier.height(Spacing.small))
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = CinemaCard,
                            border = BorderStroke(1.dp, CinemaBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CinemaTextPrimary,
                                modifier = Modifier.padding(Spacing.medium)
                            )
                        }
                    }

                    // Categories
                    if (item.categories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Spacing.large))
                        Text(
                            text = stringResource(R.string.field_categories),
                            style = MaterialTheme.typography.titleSmall,
                            color = CinemaGold
                        )
                        Spacer(modifier = Modifier.height(Spacing.small))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xSmall)
                        ) {
                            item.categories.forEach { category ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = CinemaCard,
                                    border = BorderStroke(1.dp, CinemaBorder)
                                ) {
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CinemaTextSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Custom Lists Section
                    Spacer(modifier = Modifier.height(Spacing.large))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.title_custom_lists),
                            style = MaterialTheme.typography.titleSmall,
                            color = CinemaGold,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { viewModel.onOpenManageListsDialog() },
                            border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.5f)),
                            shape = MaterialTheme.shapes.small,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                contentDescription = null,
                                tint = CinemaGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.action_manage_lists),
                                style = MaterialTheme.typography.labelSmall,
                                color = CinemaGold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.small))

                    val memberLists = uiState.allCustomLists.filter { item.listIds.contains(it.id) }
                    if (memberLists.isEmpty()) {
                        Text(
                            text = stringResource(R.string.not_assigned_to_lists),
                            style = MaterialTheme.typography.bodySmall,
                            color = CinemaTextSecondary
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xSmall)
                        ) {
                            memberLists.forEach { customList ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = CinemaGold.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Folder,
                                            contentDescription = null,
                                            tint = CinemaGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = customList.name,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                            color = CinemaGold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.xxLarge))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (uiState.showDeleteDialog && item != null) {
        ConfirmationDialog(
            title = stringResource(R.string.dialog_delete_content_title),
            message = stringResource(R.string.dialog_delete_content_message, item.title),
            onConfirm = {
                viewModel.onDeleteContent(onSuccess = onNavigateBack)
            },
            onDismiss = {
                viewModel.onDismissDeleteDialog()
            }
        )
    }

    // Manage Lists Dialog
    if (uiState.showManageListsDialog && item != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissManageListsDialog() },
            title = {
                Text(
                    text = stringResource(R.string.action_manage_lists),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CinemaTextPrimary
                )
            },
            text = {
                if (uiState.allCustomLists.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_lists_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CinemaTextSecondary
                    )
                } else {
                    Column {
                        uiState.allCustomLists.forEach { customList ->
                            val isMember = item.listIds.contains(customList.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onToggleListMembership(customList.id) }
                                    .padding(vertical = Spacing.small),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isMember,
                                    onCheckedChange = { viewModel.onToggleListMembership(customList.id) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = CinemaGold,
                                        checkmarkColor = Color(0xFF1E1300),
                                        uncheckedColor = CinemaTextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Text(
                                    text = customList.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CinemaTextPrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onDismissManageListsDialog() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CinemaGold,
                        contentColor = Color(0xFF1E1300)
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(text = stringResource(R.string.action_done))
                }
            },
            containerColor = CinemaCardElevated,
            shape = MaterialTheme.shapes.large
        )
    }
}
