package com.jv.stellariumapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.jv.stellariumapp.R

// Using the provided font, but falling back to Serif for Titles to match the website
val Frutiger = FontFamily(
    Font(R.font.neue_frutiger_world_regular, FontWeight.Normal),
    Font(R.font.neue_frutiger_world_regular, FontWeight.Medium),
    Font(R.font.neue_frutiger_world_regular, FontWeight.Bold)
)

val Typography = Typography(
    // Big Titles (e.g. "STELLARIUM FOUNDATION") - Serif to match website
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp, // Reduced from 57
        textAlign = TextAlign.Center
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp, // Reduced from 45
        textAlign = TextAlign.Center
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp, // Reduced from 32
        textAlign = TextAlign.Center
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp, // Reduced from 28
        textAlign = TextAlign.Center
    ),
    // Subtitles
    titleLarge = TextStyle(
        fontFamily = Frutiger,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp, // Reduced from 22
        textAlign = TextAlign.Center
    ),
    titleMedium = TextStyle(
        fontFamily = Frutiger,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        textAlign = TextAlign.Center
    ),
    // Body Text
    bodyLarge = TextStyle(
        fontFamily = Frutiger,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp, // Reduced from 16
        textAlign = TextAlign.Center
    ),
    bodyMedium = TextStyle(
        fontFamily = Frutiger,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp, // Reduced from 14
        textAlign = TextAlign.Center
    ),
    labelLarge = TextStyle(
        fontFamily = Frutiger,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        textAlign = TextAlign.Center
    )
)