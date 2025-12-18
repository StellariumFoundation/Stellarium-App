package com.jv.stellariumapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun BookScreen() {
    val chapters = listOf(
        Chapter("Principle 1", "Each individual possesses the innate ability to thrive on their own interests."),
        Chapter("Principle 2", "Wealth is the biggest and most important metric. Generate wealth."),
        Chapter("Principle 3", "The duty of everyone is to be a peacemaker. Peace and wealth walk in one accord."),
        Chapter("Principle 4", "War is anti-wealth. Conflict is anti-peace."),
        Chapter("The Goal", "To create a world where prosperity and opportunity are accessible to all through the 'Water' suite of products and robotics.")
    )

    Column(
        modifier = Modifier.padding(16.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "The Stellarium Book", 
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(chapters) { chapter ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = chapter.title, 
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = chapter.content, 
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}