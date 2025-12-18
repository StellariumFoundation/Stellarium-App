package com.jv.stellariumapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    var selectedTopic by remember { mutableStateOf<HomeTopic?>(null) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Title ---
        Text(
            text = "STELLARIUM FOUNDATION",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. Website Button (Subtitle) ---
        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.stellarium.ddns-ip.net/home"))
                context.startActivity(intent)
            }
        ) {
            Text("Visit Official Website")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // --- 3. Introduction ---
        Text(
            text = "An institution to propel global wealth creation and wellness.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Through high-profile advising, technology, wisdom, and innovative fortitude, we implement commoditizing solutions in business, policy, finance, personal wealth creation, relationships, and branding.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        // --- 4. Interaction Prompt ---
        Text(
            text = "How would you like to interact?",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // --- 5. Topics Grid ---
        
        // Row 1
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TopicCard(
                icon = Icons.Default.Public,
                title = "Policy",
                modifier = Modifier.weight(1f)
            ) {
                selectedTopic = HomeTopic.Policy
                showSheet = true
            }
            TopicCard(
                icon = Icons.Default.Lightbulb,
                title = "Philosophy",
                modifier = Modifier.weight(1f)
            ) {
                selectedTopic = HomeTopic.Philosophy
                showSheet = true
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Row 2
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TopicCard(
                icon = Icons.Default.Business,
                title = "Projects",
                modifier = Modifier.weight(1f)
            ) {
                selectedTopic = HomeTopic.Projects
                showSheet = true
            }
            TopicCard(
                icon = Icons.Default.Group,
                title = "Partner Up",
                modifier = Modifier.weight(1f)
            ) {
                selectedTopic = HomeTopic.Partner
                showSheet = true
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Row 3
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TopicCard(
                icon = Icons.Default.SelfImprovement,
                title = "Growth",
                modifier = Modifier.weight(1f)
            ) {
                selectedTopic = HomeTopic.Growth
                showSheet = true
            }
            TopicCard(
                icon = Icons.Default.Psychology,
                title = "Consciousness",
                modifier = Modifier.weight(1f)
            ) {
                selectedTopic = HomeTopic.Consciousness
                showSheet = true
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }

    // --- Bottom Sheet Logic ---
    if (showSheet && selectedTopic != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1E1E1E), // Solid dark background for readability
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = selectedTopic!!.icon, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = selectedTopic!!.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = selectedTopic!!.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun TopicCard(
    icon: ImageVector, 
    title: String, 
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

// --- Data for the Topics ---
enum class HomeTopic(val title: String, val description: String, val icon: ImageVector) {
    Policy(
        "Policy", 
        "Economy, politics, and society. We advocate for systems that prioritize sustainable wealth creation and societal stability through pragmatic frameworks.",
        Icons.Default.Public
    ),
    Philosophy(
        "The Philosophy", 
        "Non-religious, non-political, non-ideological. The Stellarium stands for universal values, practical wisdom, and their application to improve well-being without dogmatic constraints.",
        Icons.Default.Lightbulb
    ),
    Projects(
        "The Projects", 
        "Enterprise Housing Pledge, Subsidized Jobs, 'Water' Products, Finance, Policy, Business, Relationships Education, and Mastery.",
        Icons.Default.Business
    ),
    Partner(
        "How Can You Partner?", 
        "Specific ways a member can deal with the Stellarium to enrich themselves and others. Join us in joint ventures, affiliate marketing, and strategic alliances.",
        Icons.Default.Group
    ),
    Growth(
        "Personal Growth", 
        "Education, networking, and relationships. We provide the tools for you to master your internal world and your external connections.",
        Icons.Default.SelfImprovement
    ),
    Consciousness(
        "Shared Consciousness", 
        "How the shared consciousness allows people to cast as if it were a collective dream. Be an actor, a supporter, and the reflection of the community. Do your own branding. You just have to interact with Radiohead.",
        Icons.Default.Psychology
    )
}