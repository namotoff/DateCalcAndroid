package com.datecalc.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BlueAccent,
    secondary = OrangeAccent,
    background = LightSystemGroupedBg,
    surface = LightSecondarySystemBg,
    surfaceVariant = LightTertiarySystemBg,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightCardStroke,
    error = Color(0xFFDC2626)
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueAccent,
    secondary = OrangeAccent,
    background = DarkSystemGroupedBg,
    surface = DarkSecondarySystemBg,
    surfaceVariant = DarkTertiarySystemBg,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkCardStroke,
    error = Color(0xFFEF4444)
)

@Composable
fun DateCalcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(darkTheme) {
            val window = (view.context as Activity).window
            val bgColor = colorScheme.background.toArgb()
            window.statusBarColor = bgColor
            window.navigationBarColor = bgColor
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DateCalcTypography,
        content = content
    )
}
