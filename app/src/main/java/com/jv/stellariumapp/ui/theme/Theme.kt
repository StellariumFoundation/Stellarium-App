package com.jv.stellariumapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// The Website Vibe: Dark Background with Neon/Rainbow highlights
private val DarkRainbowScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),      // White Text
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFFE0B0FF),    // Light Mauve for accents
    tertiary = Color(0xFF00FFFF),     // Cyan for buttons/interactions
    background = Color.Transparent,   // IMPORTANT: Transparent so our Gradient shows through
    surface = Color(0x33000000),      // Semi-transparent black for Cards
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFEEEEEE)
)

// Define the "Semi Rainbow" Gradient Brush
val StellariumGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF220000), // Deep Dark Red (Top)
        Color(0xFF1A0033), // Deep Dark Purple
        Color(0xFF001133), // Deep Dark Blue
        Color(0xFF002200), // Deep Dark Green
        Color(0xFF000000)  // Black (Bottom)
    )
)

@Composable
fun StellariumAppTheme(
    darkTheme: Boolean = true, // Force Dark Theme
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Black.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkRainbowScheme,
        typography = Typography, // Ensure you have the Typography from the previous step
        content = content
    )
}