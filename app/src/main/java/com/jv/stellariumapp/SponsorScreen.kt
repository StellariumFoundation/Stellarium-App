package com.jv.stellariumapp

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorScreen(navController: NavController) {
    var showSheet by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Header ---
        Text(
            text = "Support the Mission",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Corporate Sponsorship Card ---
        OutlinedCard(
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Become a Partner",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Gain a platform of global cultural relevance to advertise your brand. Support the Stellarium Foundation and align your business with prosperity and peace.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.navigate(Screen.Contact.route) }) {
                    Text("Contact Us for a Deal")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- Payment Grid Header ---
        Text(
            text = "How do you want to make your payment?",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Payment Grid (2x2) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PaymentGridItem(
                icon = Icons.Default.AccountBalance,
                label = "Bank Deposit",
                modifier = Modifier.weight(1f)
            ) {
                selectedMethod = PaymentMethod.Bank
                showSheet = true
            }
            PaymentGridItem(
                icon = Icons.Default.CurrencyBitcoin,
                label = "Crypto",
                modifier = Modifier.weight(1f)
            ) {
                selectedMethod = PaymentMethod.Crypto
                showSheet = true
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PaymentGridItem(
                icon = Icons.Default.AttachMoney, // Placeholder for Patreon
                label = "Patreon",
                modifier = Modifier.weight(1f)
            ) {
                selectedMethod = PaymentMethod.Patreon
                showSheet = true
            }
            PaymentGridItem(
                icon = Icons.Default.Payment,
                label = "PayPal",
                modifier = Modifier.weight(1f)
            ) {
                selectedMethod = PaymentMethod.PayPal
                showSheet = true
            }
        }
    }

    // --- Bottom Sheet for Details ---
    if (showSheet && selectedMethod != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            // Content inside the sheet
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()) // Allow scrolling for long bank lists
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (selectedMethod) {
                    PaymentMethod.Bank -> BankDetails()
                    PaymentMethod.Crypto -> CryptoDetails()
                    PaymentMethod.Patreon -> PatreonDetails()
                    PaymentMethod.PayPal -> PayPalDetails()
                    else -> {}
                }
                Spacer(modifier = Modifier.height(48.dp)) // Bottom padding
            }
        }
    }
}

// --- Enum for Selection ---
enum class PaymentMethod { Bank, Crypto, Patreon, PayPal }

// --- Sub-Composables ---

@Composable
fun PaymentGridItem(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun BankDetails() {
    Text("Bank Transfer Details", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(16.dp))
    Text("Please choose the account matching your currency.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
    Spacer(modifier = Modifier.height(24.dp))

    // Cayman / USD
    BankSection("Cayman Islands (USD)", "BANCO C6 S.A. CAYMAN BRANCH", 
        "Account: 1009519676\nSwift: CSIXKYKY\nIntermediary: JP Morgan Chase (CHASUS33)")

    // Brazil
    BankSection("Brazil (BRL)", "PIX Key", "stellar.foundation.us@gmail.com")

    // Euro
    BankSection("Euro (Local/SEPA)", "Revolut Bank UAB", "IBAN: LT93 3250 0324 1949 5535\nBIC: REVOLT21")

    // Global Revolut
    BankSection("Global (GBP, HKD, AED, ILS, JPY, PLN, CHF)", "Revolut Technologies Singapore", 
        "Account: 6120621849\nBIC/SWIFT: REVOSGS2")
}

@Composable
fun CryptoDetails() {
    val context = LocalContext.current
    Text("Cryptocurrency", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(24.dp))

    // Monero
    Text("Monero (XMR) - Anonymous", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
    Spacer(modifier = Modifier.height(8.dp))
    CopyableText(
        label = "Address",
        text = "44u8KhinKQ4SgpxwS5jq3cJBMWVsWnMHaGMqYp8abTw3iAJW5izBm9V7uoNVcXAeWS6UqUzVdrn2qAtH4Epd5RkoDJxtRaL"
    )

    Spacer(modifier = Modifier.height(32.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(32.dp))

    // Any Crypto
    Text("Any Other Crypto", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
    Spacer(modifier = Modifier.height(8.dp))
    Text("Use Trocador to pay with Bitcoin, Ethereum, etc.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://trocador.app/en/anonpay/?ticker_to=xmr&network_to=Mainnet&address=44u8KhinKQ4SgpxwS5jq3cJBMWVsWnMHaGMqYp8abTw3iAJW5izBm9V7uoNVcXAeWS6UqUzVdrn2qAtH4Epd5RkoDJxtRaL&donation=True&amount=600.0&name=Stellarium+Foundation+&description=Donation+Checkout&email=stellar.foundation.us@gmail.com&ticker_from=usdc&network_from=ERC20&bgcolor="))
        context.startActivity(intent)
    }) {
        Icon(Icons.Default.OpenInNew, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Open Trocador App")
    }
}

@Composable
fun PatreonDetails() {
    val context = LocalContext.current
    Text("Patreon", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(16.dp))
    Text("Join our exclusive community on Patreon.", textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.patreon.com/join/StellariumFoundation"))
        context.startActivity(intent)
    }) {
        Text("Visit Patreon Page")
    }
}

@Composable
fun PayPalDetails() {
    val context = LocalContext.current
    Text("PayPal", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(24.dp))
    
    CopyableText(label = "PayPal Email", text = "stellar.foundation.us@gmail.com")
    
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = {
        try {
            // Try to open PayPal App
            val intent = context.packageManager.getLaunchIntentForPackage("com.paypal.android.p2pmobile")
            if (intent != null) {
                context.startActivity(intent)
            } else {
                // Fallback to web
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com"))
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com"))
            context.startActivity(webIntent)
        }
    }) {
        Text("Open PayPal App")
    }
}

@Composable
fun BankSection(title: String, bankName: String, details: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = bankName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = details, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                CopyButton(details)
            }
        }
    }
}

@Composable
fun CopyableText(label: String, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text, 
                style = MaterialTheme.typography.bodyMedium, 
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            CopyButton(text)
        }
    }
}

@Composable
fun CopyButton(text: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    IconButton(onClick = {
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
    }) {
        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
    }
}