package com.vp.whattowatch.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vp.whattowatch.R
import com.vp.whattowatch.WhatToWatchApp
import com.vp.whattowatch.ui.components.EmptyStateView
import com.vp.whattowatch.ui.components.PosterCard
import com.vp.whattowatch.ui.sharing.SharingHelper
import com.vp.whattowatch.ui.theme.CinemaBlack
import com.vp.whattowatch.ui.theme.CinemaGold
import com.vp.whattowatch.ui.theme.CinemaTextPrimary
import com.vp.whattowatch.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    viewModel: ListDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (contentId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.customList?.name ?: stringResource(R.string.title_list_default),
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
                    val list = uiState.customList
                    if (list != null) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                val app = context.applicationContext as? WhatToWatchApp
                                val sharingService = app?.container?.sharingService
                                if (sharingService != null) {
                                    SharingHelper.shareCustomList(
                                        context = context,
                                        sharingService = sharingService,
                                        listId = list.id,
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

            uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    EmptyStateView(
                        icon = Icons.Rounded.Movie,
                        title = stringResource(R.string.empty_list_items_title),
                        subtitle = stringResource(R.string.empty_list_items_subtitle),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 145.dp),
                    contentPadding = PaddingValues(Spacing.regular),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
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
