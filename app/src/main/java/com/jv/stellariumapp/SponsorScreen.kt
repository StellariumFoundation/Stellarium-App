
package com.jv.stellariumapp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SponsorScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "Support the Mission", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Join us in creating global prosperity.", style = MaterialTheme.typography.bodyLarge)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SponsorOption("Partner & Prosper", "Affiliate Marketing: Earn 40% commission on funds raised.\nJoint Ventures: Co-launch products.")
        SponsorOption("Donation - Monero (XMR)", "Address: 44u8KhinKQ4SgpxwS5jq3cJBMWVsWnMHaGMqYp8abTw3iAJW5izBm9V7uoNVcXAeWS6UqUzVdrn2qAtH4Epd5RkoDJxtRaL")
        SponsorOption("Banking / PIX", "PIX Key: stellar.foundation.us@gmail.com")
        SponsorOption("Patreon", "Search for 'Stellarium Foundation' on Patreon.")
    }
}

@Composable
fun SponsorOption(title: String, details: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = details)
        }
    }
}