package com.jv.stellariumapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "STELLARIUM FOUNDATION",
            style = MaterialTheme.typography.displayMedium, // Using smaller display style
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Do Good. Make Money. Have Fun.", 
            style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Our Mission", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "We are dedicated to driving economic prosperity and social progress. We empower individuals through innovative solutions and strategic partnerships.",
                    style = MaterialTheme.typography.bodyMedium, // Smaller font
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "THE LAW", 
            style = MaterialTheme.typography.headlineMedium, 
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LawItem("Make Money", "Create value, do what you love, build thriving businesses.")
        LawItem("Have Fun", "Celebrate life, make friends, find happiness and self-fulfillment.")
        LawItem("Do Good", "Be benevolent, stand up, be a hero, and shine your light.")
        
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW, 
                    android.net.Uri.parse("https://www.stellarium.ddns-ip.net/home")
                )
                context.startActivity(intent)
            }
        ) {
            Text("Visit Official Website")
        }
    }
}

@Composable
fun LawItem(title: String, desc: String) {
    Column(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "∆ $title", 
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = desc, 
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}