package com.vp.whattowatch.ui.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vp.whattowatch.R
import com.vp.whattowatch.domain.model.CustomList
import com.vp.whattowatch.ui.components.ConfirmationDialog
import com.vp.whattowatch.ui.components.EmptyStateView
import com.vp.whattowatch.ui.editor.editorTextFieldColors
import com.vp.whattowatch.ui.theme.CinemaBlack
import com.vp.whattowatch.ui.theme.CinemaBorder
import com.vp.whattowatch.ui.theme.CinemaCard
import com.vp.whattowatch.ui.theme.CinemaCardElevated
import com.vp.whattowatch.ui.theme.CinemaGold
import com.vp.whattowatch.ui.theme.CinemaRed
import com.vp.whattowatch.ui.theme.CinemaTextPrimary
import com.vp.whattowatch.ui.theme.CinemaTextSecondary
import com.vp.whattowatch.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    viewModel: ListsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToListDetail: (listId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_custom_lists),
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onOpenCreateDialog() },
                containerColor = CinemaGold,
                contentColor = Color(0xFF1E1300),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.title_create_list),
                    modifier = Modifier.size(28.dp)
                )
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
                    CircularProgressIndicator(color = CinemaGold)
                }
            }

            uiState.isEmpty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    EmptyStateView(
                        icon = Icons.Rounded.VideoLibrary,
                        title = stringResource(R.string.empty_lists_title),
                        subtitle = stringResource(R.string.empty_lists_subtitle),
                        actionLabel = stringResource(R.string.title_create_list),
                        onActionClick = { viewModel.onOpenCreateDialog() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(Spacing.regular),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    items(
                        items = uiState.lists,
                        key = { it.id }
                    ) { list ->
                        CustomListCard(
                            list = list,
                            onClick = { onNavigateToListDetail(list.id) },
                            onRename = { viewModel.onOpenRenameDialog(list) },
                            onDelete = { viewModel.onOpenDeleteDialog(list) }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Create New List
    if (uiState.showCreateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissCreateDialog() },
            title = {
                Text(
                    text = stringResource(R.string.title_create_list),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CinemaTextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = uiState.newListName,
                    onValueChange = { viewModel.onNewListNameChanged(it) },
                    placeholder = { Text(text = stringResource(R.string.list_name_hint), color = CinemaTextSecondary) },
                    singleLine = true,
                    colors = editorTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onCreateList() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CinemaGold,
                        contentColor = Color(0xFF1E1300)
                    ),
                    shape = MaterialTheme.shapes.small,
                    enabled = uiState.newListName.trim().isNotBlank() && !uiState.isProcessing
                ) {
                    Text(text = stringResource(R.string.action_create))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.onDismissCreateDialog() },
                    border = BorderStroke(1.dp, CinemaBorder),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(text = stringResource(R.string.action_cancel), color = CinemaTextSecondary)
                }
            },
            containerColor = CinemaCardElevated,
            shape = MaterialTheme.shapes.large
        )
    }

    // Dialog: Rename List
    val listToRename = uiState.listToRename
    if (listToRename != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissRenameDialog() },
            title = {
                Text(
                    text = stringResource(R.string.title_rename_list),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CinemaTextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = uiState.renameListName,
                    onValueChange = { viewModel.onRenameListNameChanged(it) },
                    placeholder = { Text(text = stringResource(R.string.list_name_hint), color = CinemaTextSecondary) },
                    singleLine = true,
                    colors = editorTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onRenameList() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CinemaGold,
                        contentColor = Color(0xFF1E1300)
                    ),
                    shape = MaterialTheme.shapes.small,
                    enabled = uiState.renameListName.trim().isNotBlank() && !uiState.isProcessing
                ) {
                    Text(text = stringResource(R.string.action_rename))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.onDismissRenameDialog() },
                    border = BorderStroke(1.dp, CinemaBorder),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(text = stringResource(R.string.action_cancel), color = CinemaTextSecondary)
                }
            },
            containerColor = CinemaCardElevated,
            shape = MaterialTheme.shapes.large
        )
    }

    // Dialog: Delete List Confirmation
    val listToDelete = uiState.listToDelete
    if (listToDelete != null) {
        ConfirmationDialog(
            title = stringResource(R.string.dialog_delete_list_title),
            message = stringResource(R.string.dialog_delete_list_message, listToDelete.name),
            onConfirm = { viewModel.onDeleteList() },
            onDismiss = { viewModel.onDismissDeleteDialog() }
        )
    }
}

@Composable
fun CustomListCard(
    list: CustomList,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, CinemaBorder), MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = CinemaCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder Icon
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                tint = CinemaGold,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(Spacing.medium))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = CinemaTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                val countText = if (list.itemCount == 1) {
                    stringResource(R.string.item_count_single)
                } else {
                    stringResource(R.string.items_count, list.itemCount)
                }
                Text(
                    text = countText,
                    style = MaterialTheme.typography.bodySmall,
                    color = CinemaTextSecondary
                )
            }

            // Rename button
            IconButton(onClick = onRename) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.cd_rename_list),
                    tint = CinemaTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.cd_delete_list),
                    tint = CinemaRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Navigate arrow
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = CinemaBorder,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
