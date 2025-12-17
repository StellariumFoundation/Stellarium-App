package com.jv.stellariumapp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Books : Screen("books", "Library", Icons.Default.Book)
    object Quiz : Screen("quiz", "Quiz", Icons.Default.Quiz)
    object Sponsor : Screen("sponsor", "Sponsor", Icons.Default.MonetizationOn)
    object Contact : Screen("contact", "Message", Icons.Default.Mail)
}