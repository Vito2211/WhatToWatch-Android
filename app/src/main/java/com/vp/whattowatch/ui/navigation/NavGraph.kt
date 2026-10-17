package com.vp.whattowatch.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vp.whattowatch.WhatToWatchApp
import com.vp.whattowatch.ui.detail.ContentDetailScreen
import com.vp.whattowatch.ui.detail.ContentDetailViewModel
import com.vp.whattowatch.ui.editor.ContentEditorScreen
import com.vp.whattowatch.ui.editor.ContentEditorViewModel
import com.vp.whattowatch.ui.library.LibraryScreen
import com.vp.whattowatch.ui.library.LibraryViewModel
import com.vp.whattowatch.ui.lists.ListDetailScreen
import com.vp.whattowatch.ui.lists.ListDetailViewModel
import com.vp.whattowatch.ui.lists.ListsScreen
import com.vp.whattowatch.ui.lists.ListsViewModel
import androidx.compose.runtime.LaunchedEffect
import com.vp.whattowatch.ui.settings.SettingsScreen
import com.vp.whattowatch.ui.settings.SettingsViewModel
import com.vp.whattowatch.ui.sharing.ImportPreviewScreen
import com.vp.whattowatch.ui.sharing.ImportPreviewViewModel

@Composable
fun WhatToWatchNavGraph(
    navController: NavHostController,
    pendingImportRoute: String? = null,
    onLanguageChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as WhatToWatchApp
    val container = app.container

    LaunchedEffect(pendingImportRoute) {
        if (!pendingImportRoute.isNullOrBlank()) {
            navController.navigate(pendingImportRoute)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Library.route,
        enterTransition = {
            fadeIn(animationSpec = tween(280)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(280)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(280))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(280))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(280)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(280)
            )
        },
        modifier = modifier
    ) {
        // Library Screen (Main)
        composable(route = Screen.Library.route) {
            val viewModel: LibraryViewModel = viewModel(
                factory = LibraryViewModel.provideFactory(
                    contentRepository = container.contentRepository,
                    categoryRepository = container.categoryRepository,
                    customListRepository = container.customListRepository
                )
            )
            LibraryScreen(
                viewModel = viewModel,
                onNavigateToDetail = { contentId ->
                    navController.navigate(Screen.ContentDetail.createRoute(contentId))
                },
                onNavigateToFullEditor = { initialTitle ->
                    navController.navigate(Screen.ContentEditor.createRoute(initialTitle = initialTitle))
                },
                onNavigateToLists = {
                    navController.navigate(Screen.Lists.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onLanguageChanged = onLanguageChanged
            )
        }

        // Custom Lists Overview
        composable(route = Screen.Lists.route) {
            val viewModel: ListsViewModel = viewModel(
                factory = ListsViewModel.provideFactory(
                    customListRepository = container.customListRepository
                )
            )
            ListsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToListDetail = { listId ->
                    navController.navigate(Screen.ListDetail.createRoute(listId))
                }
            )
        }

        // Content Detail Screen
        composable(
            route = Screen.ContentDetail.route,
            arguments = listOf(
                navArgument(Screen.ContentDetail.ARG_CONTENT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val contentId = backStackEntry.arguments?.getString(Screen.ContentDetail.ARG_CONTENT_ID) ?: ""
            val viewModel: ContentDetailViewModel = viewModel(
                key = contentId,
                factory = ContentDetailViewModel.provideFactory(
                    contentId = contentId,
                    contentRepository = container.contentRepository,
                    customListRepository = container.customListRepository
                )
            )
            ContentDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.ContentEditor.createRoute(contentId = id))
                }
            )
        }

        // Content Editor Screen (Add / Edit)
        composable(
            route = Screen.ContentEditor.route,
            arguments = listOf(
                navArgument(Screen.ContentEditor.ARG_CONTENT_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Screen.ContentEditor.ARG_INITIAL_TITLE) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val contentId = backStackEntry.arguments?.getString(Screen.ContentEditor.ARG_CONTENT_ID)
            val initialTitle = backStackEntry.arguments?.getString(Screen.ContentEditor.ARG_INITIAL_TITLE)
            val viewModel: ContentEditorViewModel = viewModel(
                key = contentId ?: initialTitle ?: "new",
                factory = ContentEditorViewModel.provideFactory(
                    contentId = contentId,
                    initialTitle = initialTitle,
                    contentRepository = container.contentRepository,
                    categoryRepository = container.categoryRepository,
                    customListRepository = container.customListRepository,
                    imageStorageManager = container.imageStorageManager
                )
            )
            ContentEditorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // List Detail Screen
        composable(
            route = Screen.ListDetail.route,
            arguments = listOf(
                navArgument(Screen.ListDetail.ARG_LIST_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString(Screen.ListDetail.ARG_LIST_ID) ?: ""
            val viewModel: ListDetailViewModel = viewModel(
                key = listId,
                factory = ListDetailViewModel.provideFactory(
                    listId = listId,
                    customListRepository = container.customListRepository
                )
            )
            ListDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { contentId ->
                    navController.navigate(Screen.ContentDetail.createRoute(contentId))
                }
            )
        }

        // Settings Screen (Language & Backup / Restore)
        composable(route = Screen.Settings.route) {
            val app = LocalContext.current.applicationContext as WhatToWatchApp
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.provideFactory(
                    application = app,
                    backupService = container.backupService
                )
            )
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onLanguageChanged = onLanguageChanged
            )
        }

        // Import Preview Screen
        composable(
            route = Screen.ImportPreview.route,
            arguments = listOf(
                navArgument(Screen.ImportPreview.ARG_DEEP_LINK) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Screen.ImportPreview.ARG_FILE_URI) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Screen.ImportPreview.ARG_RAW_PAYLOAD) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val deepLink = backStackEntry.arguments?.getString(Screen.ImportPreview.ARG_DEEP_LINK)
            val fileUri = backStackEntry.arguments?.getString(Screen.ImportPreview.ARG_FILE_URI)
            val rawPayload = backStackEntry.arguments?.getString(Screen.ImportPreview.ARG_RAW_PAYLOAD)

            val app = LocalContext.current.applicationContext as WhatToWatchApp
            val viewModel: ImportPreviewViewModel = viewModel(
                key = deepLink ?: fileUri ?: rawPayload ?: "import",
                factory = ImportPreviewViewModel.provideFactory(
                    application = app,
                    sharingService = container.sharingService,
                    deepLink = deepLink,
                    fileUri = fileUri,
                    rawPayload = rawPayload
                )
            )
            ImportPreviewScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onImportSuccess = { _ ->
                    navController.popBackStack(Screen.Library.route, inclusive = false)
                }
            )
        }
    }
}
