package com.jv.stellariumapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.widget.Toast

@Composable
fun SponsorScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Support the Mission", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Join us in creating global prosperity.", 
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SponsorOption("Partner & Prosper", "Affiliate Marketing: Earn 40% commission on funds raised.\nJoint Ventures: Co-launch products.")
        SponsorOption("Donation - Monero (XMR)", "44u8KhinKQ4SgpxwS5jq3cJBMWVsWnMHaGMqYp8abTw3iAJW5izBm9V7uoNVcXAeWS6UqUzVdrn2qAtH4Epd5RkoDJxtRaL")
        SponsorOption("Banking / PIX", "stellar.foundation.us@gmail.com")
        SponsorOption("Patreon", "Search for 'Stellarium Foundation' on Patreon.")
    }
}

@Composable
fun SponsorOption(title: String, details: String) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    Card(
        onClick = {
            clipboardManager.setText(AnnotatedString(details))
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "(Tap to Copy)", 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}