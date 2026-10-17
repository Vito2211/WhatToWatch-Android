package com.vp.whattowatch.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FilterListOff
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vp.whattowatch.R
import com.vp.whattowatch.data.preference.LanguagePreferenceManager
import com.vp.whattowatch.domain.model.WatchStatus
import com.vp.whattowatch.ui.components.CinematicSearchBar
import com.vp.whattowatch.ui.components.EmptyStateView
import com.vp.whattowatch.ui.components.LanguageSelectionDialog
import com.vp.whattowatch.ui.components.PosterCard
import com.vp.whattowatch.ui.theme.CinemaBorder
import com.vp.whattowatch.ui.theme.CinemaCard
import com.vp.whattowatch.ui.theme.CinemaGold
import com.vp.whattowatch.ui.theme.CinemaTextPrimary
import com.vp.whattowatch.ui.theme.CinemaTextSecondary
import com.vp.whattowatch.ui.theme.Spacing
import com.vp.whattowatch.ui.theme.StatusNotWatchedColor
import com.vp.whattowatch.ui.theme.StatusWatchedColor
import com.vp.whattowatch.ui.theme.StatusWatchingColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onNavigateToDetail: (contentId: String) -> Unit,
    onNavigateToFullEditor: (initialTitle: String?) -> Unit,
    onNavigateToLists: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onLanguageChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onOpenQuickAdd() },
                containerColor = CinemaGold,
                contentColor = Color(0xFF1E1300),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.cd_add_content),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.regular, vertical = Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = CinemaGold
                        )
                    )
                    Text(
                        text = stringResource(R.string.tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = CinemaTextSecondary
                    )
                }

                // In-App Language Selector Button
                IconButton(onClick = { showLanguageDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = stringResource(R.string.cd_language_settings),
                        tint = CinemaGold
                    )
                }

                // Quick access to Custom Lists
                IconButton(onClick = onNavigateToLists) {
                    Icon(
                        imageVector = Icons.Rounded.VideoLibrary,
                        contentDescription = stringResource(R.string.cd_custom_lists),
                        tint = CinemaGold
                    )
                }

                // Settings & Backup
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = CinemaGold
                    )
                }
            }

            // Search bar
            Box(modifier = Modifier.padding(horizontal = Spacing.regular, vertical = Spacing.small)) {
                CinematicSearchBar(
                    query = uiState.query,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) }
                )
            }

            // Status Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.regular, vertical = Spacing.xSmall),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                // All
                StatusFilterChip(
                    label = stringResource(R.string.filter_all),
                    isSelected = uiState.selectedStatus == null,
                    activeColor = CinemaGold,
                    onClick = { viewModel.onStatusFilterSelected(null) }
                )

                // Watching
                StatusFilterChip(
                    label = stringResource(R.string.status_watching),
                    isSelected = uiState.selectedStatus == WatchStatus.WATCHING,
                    activeColor = StatusWatchingColor,
                    onClick = { viewModel.onStatusFilterSelected(WatchStatus.WATCHING) }
                )

                // Watched
                StatusFilterChip(
                    label = stringResource(R.string.status_watched),
                    isSelected = uiState.selectedStatus == WatchStatus.WATCHED,
                    activeColor = StatusWatchedColor,
                    onClick = { viewModel.onStatusFilterSelected(WatchStatus.WATCHED) }
                )

                // Not Watched
                StatusFilterChip(
                    label = stringResource(R.string.status_not_watched),
                    isSelected = uiState.selectedStatus == WatchStatus.NOT_WATCHED,
                    activeColor = StatusNotWatchedColor,
                    onClick = { viewModel.onStatusFilterSelected(WatchStatus.NOT_WATCHED) }
                )
            }

            // Optional category & list filters with smooth expand/collapse motion
            AnimatedVisibility(
                visible = uiState.availableCategories.isNotEmpty() || uiState.availableLists.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.regular, vertical = Spacing.xxSmall),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    // Category chips
                    uiState.availableCategories.forEach { category ->
                        val isSelected = uiState.selectedCategoryId == category.id
                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onCategoryFilterSelected(category.id) },
                            label = { Text(text = category.name) },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                containerColor = CinemaCard,
                                labelColor = CinemaTextSecondary,
                                selectedContainerColor = CinemaGold.copy(alpha = 0.2f),
                                selectedLabelColor = CinemaGold
                            ),
                            border = BorderStroke(1.dp, if (isSelected) CinemaGold else CinemaBorder),
                            shape = MaterialTheme.shapes.small
                        )
                    }

                    // List chips
                    uiState.availableLists.forEach { customList ->
                        val isSelected = uiState.selectedListId == customList.id
                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onListFilterSelected(customList.id) },
                            label = { Text(text = customList.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = if (isSelected) CinemaGold else CinemaTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                containerColor = CinemaCard,
                                labelColor = CinemaTextSecondary,
                                selectedContainerColor = CinemaGold.copy(alpha = 0.2f),
                                selectedLabelColor = CinemaGold
                            ),
                            border = BorderStroke(1.dp, if (isSelected) CinemaGold else CinemaBorder),
                            shape = MaterialTheme.shapes.small
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xSmall))

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            color = CinemaGold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    uiState.isLibraryEmpty -> {
                        EmptyStateView(
                            icon = Icons.Rounded.Movie,
                            title = stringResource(R.string.empty_library_title),
                            subtitle = stringResource(R.string.empty_library_subtitle),
                            actionLabel = stringResource(R.string.title_add_content),
                            onActionClick = { viewModel.onOpenQuickAdd() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    uiState.items.isEmpty() && uiState.isFilterActive -> {
                        EmptyStateView(
                            icon = Icons.Rounded.FilterListOff,
                            title = stringResource(R.string.empty_search_title),
                            subtitle = stringResource(R.string.empty_search_subtitle),
                            actionLabel = stringResource(R.string.action_clear_filters),
                            onActionClick = { viewModel.onClearFilters() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        // Responsive adaptive grid (min 145dp adapts to 2, 3, 4, 6 columns across devices)
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 145.dp),
                            contentPadding = PaddingValues(Spacing.regular),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = uiState.items,
                                key = { it.id }
                            ) { item ->
                                PosterCard(
                                    item = item,
                                    onClick = { onNavigateToDetail(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Add Modal Bottom Sheet
    if (uiState.showQuickAddSheet) {
        QuickAddBottomSheet(
            isSaving = uiState.isSavingQuickAdd,
            onSave = { title ->
                viewModel.onSaveQuickAdd(title)
            },
            onOpenFullEditor = { prefilledTitle ->
                viewModel.onDismissQuickAdd()
                onNavigateToFullEditor(prefilledTitle.ifBlank { null })
            },
            onDismiss = {
                viewModel.onDismissQuickAdd()
            }
        )
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = LanguagePreferenceManager.getStoredLanguage(context),
            onLanguageSelected = { selectedTag ->
                LanguagePreferenceManager.setLanguage(context, selectedTag)
                showLanguageDialog = false
                onLanguageChanged()
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
fun StatusFilterChip(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    ElevatedFilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
            )
        },
        colors = FilterChipDefaults.elevatedFilterChipColors(
            containerColor = CinemaCard,
            labelColor = CinemaTextSecondary,
            selectedContainerColor = activeColor.copy(alpha = 0.2f),
            selectedLabelColor = activeColor
        ),
        border = BorderStroke(1.dp, if (isSelected) activeColor else CinemaBorder),
        shape = MaterialTheme.shapes.small
    )
}
