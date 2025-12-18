package com.jv.stellariumapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Website Theme: Dark, Elegant, High Contrast
private val WebsiteDarkScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),     // White text/icons
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFFB0B0B0),   // Light Grey accents
    tertiary = Color(0xFFD0BCFF),    // Slight purple tint for interactive elements
    background = Color(0xFF000000),  // Pitch Black background like the hero image
    surface = Color(0xFF121212),     // Very dark grey cards
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

// We override Light Scheme to match Dark because the user wants "Website Theme" (which is dark)
private val WebsiteLightScheme = WebsiteDarkScheme

@Composable
fun StellariumAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We turn OFF dynamic color to force our custom website branding
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = WebsiteDarkScheme // Force the Dark/Website look

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false // White text on status bar
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}