package com.vp.whattowatch.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object Lists : Screen("lists")

    data object ContentDetail : Screen("content/{contentId}") {
        const val ARG_CONTENT_ID = "contentId"
        fun createRoute(contentId: String): String = "content/$contentId"
    }

    data object ContentEditor : Screen("editor?contentId={contentId}&initialTitle={initialTitle}") {
        const val ARG_CONTENT_ID = "contentId"
        const val ARG_INITIAL_TITLE = "initialTitle"

        fun createRoute(contentId: String? = null, initialTitle: String? = null): String {
            val encodedTitle = initialTitle?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) }
            return when {
                contentId != null && encodedTitle != null -> "editor?contentId=$contentId&initialTitle=$encodedTitle"
                contentId != null -> "editor?contentId=$contentId"
                encodedTitle != null -> "editor?initialTitle=$encodedTitle"
                else -> "editor"
            }
        }
    }

    data object ListDetail : Screen("list/{listId}") {
        const val ARG_LIST_ID = "listId"
        fun createRoute(listId: String): String = "list/$listId"
    }

    data object Settings : Screen("settings")

    data object ImportPreview : Screen("import_preview?deepLink={deepLink}&fileUri={fileUri}&rawPayload={rawPayload}") {
        const val ARG_DEEP_LINK = "deepLink"
        const val ARG_FILE_URI = "fileUri"
        const val ARG_RAW_PAYLOAD = "rawPayload"

        fun createRoute(deepLink: String? = null, fileUri: String? = null, rawPayload: String? = null): String {
            val encodedDeepLink = deepLink?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) }
            val encodedFileUri = fileUri?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) }
            val encodedRawPayload = rawPayload?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) }

            val params = mutableListOf<String>()
            if (encodedDeepLink != null) params.add("$ARG_DEEP_LINK=$encodedDeepLink")
            if (encodedFileUri != null) params.add("$ARG_FILE_URI=$encodedFileUri")
            if (encodedRawPayload != null) params.add("$ARG_RAW_PAYLOAD=$encodedRawPayload")

            return if (params.isNotEmpty()) "import_preview?${params.joinToString("&")}" else "import_preview"
        }
    }
}
