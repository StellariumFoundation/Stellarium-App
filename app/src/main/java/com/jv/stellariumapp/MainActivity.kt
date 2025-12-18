package com.jv.stellariumapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.jv.stellariumapp.ui.theme.StellariumAppTheme
import com.jv.stellariumapp.ui.theme.StellariumGradient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StellariumAppTheme { 
                StellariumApp()
            }
        }
    }
}

@Composable
fun StellariumApp() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Home,
        Screen.Books,
        Screen.Quiz,
        Screen.Sponsor,
        Screen.Contact
    )

    // Global Gradient Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StellariumGradient) // <--- THE RAINBOW GRADIENT
    ) {
        Scaffold(
            // Make Scaffold Transparent so gradient shows
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xCC000000) // Semi-transparent black bottom bar
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) { HomeScreen() }
                composable(Screen.Books.route) { BookScreen() }
                composable(Screen.Quiz.route) { QuizScreen() }
                composable(Screen.Sponsor.route) { SponsorScreen() }
                composable(Screen.Contact.route) { ContactScreen() }
            }
        }
    }
}