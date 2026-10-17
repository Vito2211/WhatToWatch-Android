package com.vp.whattowatch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.rememberNavController
import com.vp.whattowatch.data.preference.LanguagePreferenceManager
import com.vp.whattowatch.ui.navigation.Screen
import com.vp.whattowatch.ui.navigation.WhatToWatchNavGraph
import com.vp.whattowatch.ui.theme.WHATTOWATCHTheme

class MainActivity : ComponentActivity() {

    private val pendingImportRoute = mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguagePreferenceManager.applyLocaleToContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        setContent {
            WHATTOWATCHTheme {
                val navController = rememberNavController()
                WhatToWatchNavGraph(
                    navController = navController,
                    pendingImportRoute = pendingImportRoute.value,
                    onLanguageChanged = {
                        recreate()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        val data = intent.data
        val action = intent.action

        when {
            // Case 1: Deep link: whattowatch://share/...
            data != null && data.scheme == "whattowatch" && data.host == "share" -> {
                pendingImportRoute.value = Screen.ImportPreview.createRoute(deepLink = data.toString())
            }

            // Case 2: Opening a file: content:// or file://
            action == Intent.ACTION_VIEW && data != null -> {
                pendingImportRoute.value = Screen.ImportPreview.createRoute(fileUri = data.toString())
            }

            // Case 3: Sharesheet incoming text or stream
            action == Intent.ACTION_SEND -> {
                val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }
                val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)

                when {
                    streamUri != null -> {
                        pendingImportRoute.value = Screen.ImportPreview.createRoute(fileUri = streamUri.toString())
                    }
                    !extraText.isNullOrBlank() -> {
                        val trimmed = extraText.trim()
                        if (trimmed.startsWith("whattowatch://")) {
                            pendingImportRoute.value = Screen.ImportPreview.createRoute(deepLink = trimmed)
                        } else {
                            pendingImportRoute.value = Screen.ImportPreview.createRoute(rawPayload = trimmed)
                        }
                    }
                }
            }
        }
    }
}