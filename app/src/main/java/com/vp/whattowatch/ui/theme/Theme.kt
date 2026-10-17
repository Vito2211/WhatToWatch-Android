package com.vp.whattowatch.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CinematicDarkColorScheme = darkColorScheme(
    primary = CinemaGold,
    onPrimary = Color(0xFF1E1300),
    primaryContainer = CinemaGoldContainer,
    onPrimaryContainer = CinemaGoldOnContainer,
    secondary = CinemaSilver,
    onSecondary = Color(0xFF0F141C),
    secondaryContainer = CinemaSilverContainer,
    onSecondaryContainer = CinemaSilverOnContainer,
    tertiary = CinemaRed,
    onTertiary = Color.White,
    background = CinemaBlack,
    onBackground = CinemaTextPrimary,
    surface = CinemaCard,
    onSurface = CinemaTextPrimary,
    surfaceVariant = CinemaCardElevated,
    onSurfaceVariant = CinemaTextSecondary,
    outline = CinemaBorder,
    outlineVariant = CinemaDivider,
    error = CinemaError,
    onError = Color(0xFF1E0004)
)

private val CinematicLightColorScheme = lightColorScheme(
    primary = Color(0xFF8A5A00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDF9E),
    onPrimaryContainer = Color(0xFF2B1700),
    secondary = Color(0xFF565F71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDAE2F9),
    onSecondaryContainer = Color(0xFF131C2B),
    tertiary = CinemaRed,
    onTertiary = Color.White,
    background = Color(0xFFF9F9FC),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE2E4EC),
    onSurfaceVariant = Color(0xFF45474F),
    outline = Color(0xFF757780),
    outlineVariant = Color(0xFFC5C6D0),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun WHATTOWATCHTheme(
    darkTheme: Boolean = true, // Default to dark cinematic experience
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CinematicDarkColorScheme else CinematicLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}