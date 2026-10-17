package com.vp.whattowatch.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vp.whattowatch.R
import com.vp.whattowatch.data.local.image.ImageStorageManager
import com.vp.whattowatch.domain.model.ContentType
import com.vp.whattowatch.domain.model.WatchStatus
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContentEditorScreen(
    viewModel: ContentEditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onPosterSelected(uri)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) {
                            stringResource(R.string.title_edit_content)
                        } else {
                            stringResource(R.string.title_add_content)
                        },
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
                    Button(
                        onClick = {
                            viewModel.saveContent(onSuccess = onNavigateBack)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CinemaGold,
                            contentColor = Color(0xFF1E1300)
                        ),
                        shape = MaterialTheme.shapes.small,
                        enabled = !uiState.isSaving && uiState.isValid,
                        modifier = Modifier.padding(end = Spacing.small)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                color = Color(0xFF1E1300),
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.action_save),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CinemaBlack)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CinemaGold)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.regular)
            ) {
                // Section: Poster Artwork
                Text(
                    text = stringResource(R.string.poster_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = CinemaGold
                )
                Spacer(modifier = Modifier.height(Spacing.xSmall))

                val posterFile = remember(uiState.posterPath) {
                    uiState.posterPath?.let { ImageStorageManager.getPosterFile(context, it) }
                }
                val hasPoster = posterFile != null && posterFile.exists() && posterFile.length() > 0

                if (hasPoster) {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, CinemaBorder),
                        colors = CardDefaults.cardColors(containerColor = CinemaCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.regular),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .width(84.dp)
                                    .aspectRatio(Spacing.POSTER_ASPECT_RATIO)
                            ) {
                                AsyncImage(
                                    model = posterFile,
                                    contentDescription = stringResource(R.string.cd_poster_image),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(Spacing.regular))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(Spacing.small)
                            ) {
                                if (uiState.isProcessingPoster) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                                    ) {
                                        CircularProgressIndicator(
                                            color = CinemaGold,
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = stringResource(R.string.poster_formats_hint),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CinemaTextSecondary
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(1.dp, CinemaGold),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CinemaGold),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.xSmall))
                                        Text(
                                            text = stringResource(R.string.action_change_poster),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.onRemovePoster() },
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.xSmall))
                                        Text(
                                            text = stringResource(R.string.action_remove_poster),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        onClick = {
                            if (!uiState.isProcessingPoster) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        color = CinemaCard,
                        border = BorderStroke(1.dp, CinemaBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isProcessingPoster) {
                                CircularProgressIndicator(color = CinemaGold, modifier = Modifier.size(28.dp))
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(Spacing.xxSmall)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Image,
                                        contentDescription = null,
                                        tint = CinemaGold,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.action_add_poster),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = CinemaGold
                                    )
                                    Text(
                                        text = stringResource(R.string.poster_formats_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CinemaTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.large))

                // Section: Title
                Text(
                    text = stringResource(R.string.field_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = CinemaGold
                )
                Spacer(modifier = Modifier.height(Spacing.xSmall))
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.onTitleChanged(it) },
                    placeholder = { Text(text = stringResource(R.string.field_title_hint), color = CinemaTextSecondary) },
                    isError = uiState.titleErrorRes != null,
                    supportingText = uiState.titleErrorRes?.let { errRes ->
                        { Text(text = stringResource(errRes)) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = editorTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.large))

                // Section: Content Type
                Text(
                    text = stringResource(R.string.field_type),
                    style = MaterialTheme.typography.titleSmall,
                    color = CinemaGold
                )
                Spacer(modifier = Modifier.height(Spacing.xSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    ContentType.entries.forEach { type ->
                        val isSelected = uiState.contentType == type
                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onContentTypeSelected(type) },
                            label = { Text(text = getContentTypeLabel(type)) },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                containerColor = CinemaCard,
                                labelColor = CinemaTextSecondary,
                                selectedContainerColor = CinemaGold.copy(alpha = 0.25f),
                                selectedLabelColor = CinemaGold
                            ),
                            border = BorderStroke(1.dp, if (isSelected) CinemaGold else CinemaBorder),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.large))

                // Section: Viewing Status
                Text(
                    text = stringResource(R.string.field_status),
                    style = MaterialTheme.typography.titleSmall,
                    color = CinemaGold
                )
                Spacer(modifier = Modifier.height(Spacing.xSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    val statusList = listOf(
                        Triple(WatchStatus.NOT_WATCHED, R.string.status_not_watched, StatusNotWatchedColor),
                        Triple(WatchStatus.WATCHING, R.string.status_watching, StatusWatchingColor),
                        Triple(WatchStatus.WATCHED, R.string.status_watched, StatusWatchedColor)
                    )

                    statusList.forEach { (status, labelRes, color) ->
                        val isSelected = uiState.watchStatus == status
                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onWatchStatusSelected(status) },
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

                Spacer(modifier = Modifier.height(Spacing.large))

                // Section: External URL
                Text(
                    text = stringResource(R.string.field_url),
                    style = MaterialTheme.typography.titleSmall,
                    color = CinemaGold
                )
                Spacer(modifier = Modifier.height(Spacing.xSmall))
                OutlinedTextField(
                    value = uiState.externalUrl,
                    onValueChange = { viewModel.onExternalUrlChanged(it) },
                    placeholder = { Text(text = stringResource(R.string.field_url_hint), color = CinemaTextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = editorTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.large))

                // Section: Description / Notes
                Text(
                    text = stringResource(R.string.field_description),
                    style = MaterialTheme.typography.titleSmall,
                    color = CinemaGold
                )
                Spacer(modifier = Modifier.height(Spacing.xSmall))
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.onDescriptionChanged(it) },
                    placeholder = { Text(text = stringResource(R.string.field_description_hint), color = CinemaTextSecondary) },
                    minLines = 3,
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = editorTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.large))

                // Section: Categories
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.field_categories),
                        style = MaterialTheme.typography.titleSmall,
                        color = CinemaGold,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { viewModel.onOpenNewCategoryDialog() },
                        border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.5f)),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = CinemaGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.action_add_category),
                            style = MaterialTheme.typography.labelSmall,
                            color = CinemaGold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xSmall))

                if (uiState.availableCategories.isEmpty()) {
                    Text(
                        text = stringResource(R.string.empty_categories_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = CinemaTextSecondary
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xSmall)
                    ) {
                        uiState.availableCategories.forEach { category ->
                            val isSelected = uiState.selectedCategoryIds.contains(category.id)
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onToggleCategory(category.id) },
                                label = { Text(text = category.name) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = CinemaGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    containerColor = CinemaCard,
                                    labelColor = CinemaTextSecondary,
                                    selectedContainerColor = CinemaGold.copy(alpha = 0.25f),
                                    selectedLabelColor = CinemaGold
                                ),
                                border = BorderStroke(1.dp, if (isSelected) CinemaGold else CinemaBorder),
                                shape = MaterialTheme.shapes.small
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.large))

                // Section: Custom Lists
                Text(
                    text = stringResource(R.string.field_lists),
                    style = MaterialTheme.typography.titleSmall,
                    color = CinemaGold
                )
                Spacer(modifier = Modifier.height(Spacing.xSmall))

                if (uiState.availableLists.isEmpty()) {
                    Text(
                        text = stringResource(R.string.empty_editor_lists_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = CinemaTextSecondary
                    )
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = CinemaCard,
                        border = BorderStroke(1.dp, CinemaBorder)
                    ) {
                        Column {
                            uiState.availableLists.forEach { customList ->
                                val isChecked = uiState.selectedListIds.contains(customList.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.onToggleList(customList.id) }
                                        .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { viewModel.onToggleList(customList.id) },
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
                }

                Spacer(modifier = Modifier.height(Spacing.xxLarge))
            }
        }
    }

    // Dialog: Create Category
    if (uiState.showNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissNewCategoryDialog() },
            title = {
                Text(
                    text = stringResource(R.string.title_new_category),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CinemaTextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = uiState.newCategoryName,
                    onValueChange = { viewModel.onNewCategoryNameChanged(it) },
                    placeholder = { Text(text = stringResource(R.string.category_name_hint), color = CinemaTextSecondary) },
                    singleLine = true,
                    colors = editorTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onCreateCategory() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CinemaGold,
                        contentColor = Color(0xFF1E1300)
                    ),
                    shape = MaterialTheme.shapes.small,
                    enabled = uiState.newCategoryName.trim().isNotBlank()
                ) {
                    Text(text = stringResource(R.string.action_create))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.onDismissNewCategoryDialog() },
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
}

@Composable
fun editorTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CinemaGold,
    unfocusedBorderColor = CinemaBorder,
    focusedTextColor = CinemaTextPrimary,
    unfocusedTextColor = CinemaTextPrimary,
    cursorColor = CinemaGold
)
