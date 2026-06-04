package com.mohannic.taskarma.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary              = BrandPrimary,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFF2A2654),
    onPrimaryContainer   = BrandPrimaryDim,
    secondary            = BrandSecondary,
    onSecondary          = Color(0xFF003327),
    secondaryContainer   = Color(0xFF003D30),
    onSecondaryContainer = BrandSecondary,
    tertiary             = BrandAccent,
    background           = DarkBackground,
    onBackground         = DarkOnBackground,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = DarkOnSurfaceVar,
    outline              = DarkOutline,
    error                = DarkError,
    onError              = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary              = BrandPrimary,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFE8E6FF),
    onPrimaryContainer   = Color(0xFF3730A3),
    secondary            = Color(0xFF00A37A),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFB3F0DE),
    onSecondaryContainer = Color(0xFF003D30),
    tertiary             = Color(0xFFD93025),
    background           = LightBackground,
    onBackground         = LightOnBackground,
    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = LightOnSurfaceVar,
    outline              = LightOutline,
    error                = LightError,
    onError              = Color.White,
)

@Composable
fun TaskarmaTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
