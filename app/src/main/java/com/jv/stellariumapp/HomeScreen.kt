package com.jv.stellariumapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "STELLARIUM FOUNDATION",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Do Good. Make Money. Have Fun.", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Our Mission", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "We are dedicated to driving economic prosperity and social progress. We empower individuals through innovative solutions and strategic partnerships.")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "THE LAW", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        LawItem("Make Money", "Create value, do what you love, build thriving businesses.")
        LawItem("Have Fun", "Celebrate life, make friends, find happiness and self-fulfillment.")
        LawItem("Do Good", "Be benevolent, stand up, be a hero, and shine your light.")
    }
}

@Composable
fun LawItem(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = "∆ $title", fontWeight = FontWeight.Bold)
        Text(text = desc, style = MaterialTheme.typography.bodyMedium)
    }
}