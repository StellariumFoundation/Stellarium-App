package com.jv.stellariumapp

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.*

// --- DATA STRUCTURES ---
data class ProxyNode(val ip: String, val port: Int, val type: Proxy.Type)

@Composable
fun ContactScreen() {
    // UI State
    var contact by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var useProxy by remember { mutableStateOf(false) } // Default false -> Direct Send
    
    // Dialog States
    var showPrivacyDialog by remember { mutableStateOf(false) } // Privacy education dialog
    var showOrbotMissingDialog by remember { mutableStateOf(false) } // Orbot missing dialog

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // --- HELPER: START DIRECT SEND (NO PROXY) ---
    fun startDirectSequence() {
        if (message.isBlank()) {
            Toast.makeText(context, "Message content is required.", Toast.LENGTH_SHORT).show()
            return
        }
        isSending = true
        statusMessage = "Transmitting (Standard Channel)..."
        scope.launch(Dispatchers.IO) {
            val success = sendSecureRequest(contact, message, Proxy.NO_PROXY, "formspree")
            
            withContext(Dispatchers.Main) {
                isSending = false
                if (success) {
                    statusMessage = "Message Sent Successfully."
                    Toast.makeText(context, "Message Sent", Toast.LENGTH_SHORT).show()
                    contact = ""
                    message = ""
                } else {
                    statusMessage = "Transmission Failed. Check Internet."
                    Toast.makeText(context, "Failed to send message.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- HELPER: START PUBLIC PROXY SEQUENCE ---
    fun startPublicProxySequence() {
        if (message.isBlank()) {
            Toast.makeText(context, "Message content is required.", Toast.LENGTH_SHORT).show()
            return
        }
        isSending = true
        statusMessage = "Initializing Public Proxy Protocol..."
        scope.launch {
            val success = sendWithPublicProxies(contact, message, "formspree") { status ->
                withContext(Dispatchers.Main) { statusMessage = status }
            }

            withContext(Dispatchers.Main) {
                isSending = false
                if (success) {
                    statusMessage = "Transmission Successful via Proxy."
                    Toast.makeText(context, "Secure Message Sent.", Toast.LENGTH_LONG).show()
                    contact = ""
                    message = ""
                } else {
                    statusMessage = "All Secure Nodes Failed."
                    Toast.makeText(context, "Network Unreachable.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- HELPER: START ORBOT SEQUENCE ---
    fun startOrbotSequence() {
        if (message.isBlank()) {
            Toast.makeText(context, "Message content is required.", Toast.LENGTH_SHORT).show()
            return
        }
        isSending = true
        statusMessage = "Connecting to Tor Network..."
        scope.launch {
            val success = sendViaTor(contact, message, "formspree")
            
            withContext(Dispatchers.Main) {
                isSending = false
                if (success) {
                    statusMessage = "Transmission Successful via Tor."
                    Toast.makeText(context, "Anonymized Message Sent.", Toast.LENGTH_LONG).show()
                    contact = ""
                    message = ""
                } else {
                    statusMessage = "Tor Circuit Failed."
                    Toast.makeText(context, "Check Orbot Connection.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- UI CONTENT ---
    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Contact Us", 
            style = MaterialTheme.typography.headlineMedium, 
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Send Feedback, Suggestions, Proposals, or Business Inquiries to the Stellarium Foundation.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = contact,
            onValueChange = { contact = it },
            label = { Text("Your Contact (Email/Social)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
             colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Your Message") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            minLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(checked = useProxy, onCheckedChange = { useProxy = it })
            Column {
                Text(text = "Enable Anonymity Mode", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Routes via Tor or Proxy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusMessage,
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (message.isNotBlank()) {
                    if (!useProxy) {
                        // 1. Checkbox UNCHECKED -> Show Privacy Education Dialog
                        showPrivacyDialog = true
                    } else {
                        // 2. Checkbox CHECKED -> Check for Orbot
                        scope.launch(Dispatchers.IO) {
                            val orbotReady = isOrbotRunning()
                            withContext(Dispatchers.Main) {
                                if (orbotReady) {
                                    startOrbotSequence()
                                } else {
                                    // Trigger Orbot Missing Dialog
                                    showOrbotMissingDialog = true
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(context, "Message content is required.", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isSending,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Transmitting...")
            } else {
                Text("Send Message")
            }
        }
    }

    // --- DIALOG 1: PRIVACY EDUCATION (When sending without proxy checked) ---
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(text = "Enhance Privacy?") },
            text = { 
                Text("Sending directly exposes your IP address. For anonymity, we recommend using a proxy or Tor.\n\nWould you like to enable Anonymity Mode?") 
            },
            confirmButton = {
                // Button 1: Install Orbot (Sets proxy true)
                Button(
                    onClick = {
                        useProxy = true
                        showPrivacyDialog = false
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://orbot.app/"))
                        context.startActivity(intent)
                    }
                ) {
                    Text("Install Orbot (Tor)")
                }
            },
            dismissButton = {
                Row {
                    // Button 2: Use Public Proxies (Sets proxy true)
                    TextButton(
                        onClick = {
                            useProxy = true
                            showPrivacyDialog = false
                            // Note: User just enabled proxy, they must click Send again to trigger the proxy logic
                            Toast.makeText(context, "Anonymity Mode Enabled. Press Send again.", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Use Public Proxies")
                    }
                    // Button 3: Send Directly (Leaves proxy false, sends immediately)
                    TextButton(
                        onClick = {
                            showPrivacyDialog = false
                            startDirectSequence()
                        }
                    ) {
                        Text("Send Directly")
                    }
                }
            }
        )
    }

    // --- DIALOG 2: ORBOT MISSING (When proxy checked but Orbot not found) ---
    if (showOrbotMissingDialog) {
        AlertDialog(
            onDismissRequest = { showOrbotMissingDialog = false },
            title = { Text(text = "Orbot Not Detected") },
            text = { 
                Text("For complete military-grade anonymity, we recommend using the Tor Network via Orbot.\n\nWithout Orbot, we can route your message through secure public proxies.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://orbot.app/"))
                        context.startActivity(intent)
                        showOrbotMissingDialog = false
                    }
                ) {
                    Text("Get Orbot")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOrbotMissingDialog = false
                        // User chose to continue without Orbot -> Use Public Rotating Proxies
                        startPublicProxySequence()
                    }
                ) {
                    Text("Use Public Proxies")
                }
            }
        )
    }
}

// =========================================================================
// ====================     NETWORKING LOGIC     ===========================
// =========================================================================

const val TOR_HOST = "127.0.0.1"
const val TOR_PORT = 9050

// --- 1. TOR DETECTION ---
fun isOrbotRunning(): Boolean {
    return try {
        val socket = Socket()
        // Try to connect to local Tor SOCKS port with a short timeout
        socket.connect(InetSocketAddress(TOR_HOST, TOR_PORT), 500)
        socket.close()
        true
    } catch (e: Exception) {
        false
    }
}

// --- 2. SEND VIA TOR ---
suspend fun sendViaTor(contact: String, message: String, service: String): Boolean {
    return withContext(Dispatchers.IO) {
        // Create a SOCKS proxy pointing to localhost:9050
        val torProxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(TOR_HOST, TOR_PORT))
        return@withContext sendSecureRequest(contact, message, torProxy, service)
    }
}

// --- 3. SEND VIA PUBLIC PROXY ROTATION ---
suspend fun sendWithPublicProxies(
    contact: String, 
    message: String, 
    service: String,
    updateStatus: suspend (String) -> Unit
): Boolean {
    return withContext(Dispatchers.IO) {
        
        // 1. Shuffle Once
        val shuffledProxies = anonymousProxies.shuffled()
        var attempt = 1
        val total = shuffledProxies.size
        
        // 2. Iterate All
        for (node in shuffledProxies) {
            updateStatus("Routing via Secure Node $attempt/$total (${node.ip})...")
            
            // SECURITY: Use 'createUnresolved'. 
            // This prevents the Android OS from resolving the DNS. 
            // We pass the hostname to the proxy, and the PROXY resolves it.
            val socketAddress = InetSocketAddress.createUnresolved(node.ip, node.port)
            val proxy = Proxy(node.type, socketAddress)
            
            val success = sendSecureRequest(contact, message, proxy, service)

            if (success) {
                Log.d("ProxyManager", "Secure Tunnel Established: ${node.ip}")
                return@withContext true
            }
            
            Log.e("ProxyManager", "Node Dead/Blocked: ${node.ip}. Rotating...")
            attempt++
        }

        return@withContext false
    }
}

// --- 4. UNIFIED SECURE REQUEST (HARDENED) ---
/**
 * Unified request handler. 
 * Pass Proxy.NO_PROXY for direct connection.
 */
fun sendSecureRequest(contact: String, message: String, proxy: Proxy, service: String): Boolean {
    var conn: HttpURLConnection? = null
    try {
        val targetUrl = if (service == "formspree") 
            "https://formspree.io/f/mzdpovoa" 
        else 
            "https://formsubmit.co/ajax/stellar.foundation.us@gmail.com"

        val url = URL(targetUrl)
        
        // This is where the magic happens. 
        // If proxy is Proxy.NO_PROXY, it sends directly.
        // If proxy is an HTTP/SOCKS object, it tunnels.
        conn = url.openConnection(proxy) as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.doInput = true
        
        // TIMEOUTS: 
        // If using a proxy, we want to fail fast (3s to connect).
        // If NO_PROXY (Direct), standard mobile networks might need a bit more time (e.g. 10s).
        if (proxy == Proxy.NO_PROXY) {
            conn.connectTimeout = 10000 
            conn.readTimeout = 10000
        } else {
            conn.connectTimeout = 3000
            conn.readTimeout = 5000 
        }
        
        // --- ANTI-FINGERPRINTING HEADERS ---
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("Connection", "close") // Don't allow keep-alive tracking
        
        // Spoofer: Make it look like a generic Linux or Windows Desktop
        val userAgents = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/115.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:109.0) Gecko/20100101 Firefox/115.0"
        )
        conn.setRequestProperty("User-Agent", userAgents.random())

        val jsonPayload = JSONObject()
        // Sanitize contact info if empty
        val safeContact = if (contact.isNotBlank()) contact else "anonymous@stellarium.app"
        
        if (service == "formspree") {
            jsonPayload.put("email", if (safeContact.contains("@")) safeContact else "no-reply@stellarium.app")
            jsonPayload.put("message", message)
            jsonPayload.put("contact_details", safeContact)
        } else {
            jsonPayload.put("name", "Stellarium Anon")
            jsonPayload.put("email", if (safeContact.contains("@")) safeContact else "no-reply@stellarium.app")
            jsonPayload.put("message", message)
            jsonPayload.put("contact_details", safeContact)
            jsonPayload.put("_subject", "Encrypted Transmission")
            jsonPayload.put("_captcha", "false")
        }

        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(jsonPayload.toString())
        writer.flush()
        writer.close()

        val responseCode = conn.responseCode
        // Drain stream
        try { BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() } } catch (e: Exception) {}

        // Success codes
        return responseCode in 200..299
        
    } catch (e: Exception) {
        Log.e("Network", "Send failed: ${e.message}")
        return false
    } finally {
        conn?.disconnect()
    }
}

// --- PROXY LIST ---
// (HTTP and SOCKS5 only. SOCKS4 removed for security.)
val anonymousProxies = listOf(
    // HTTP
    ProxyNode("89.43.133.145", 8080, Proxy.Type.HTTP),
    ProxyNode("4.213.98.253", 80, Proxy.Type.HTTP),
    ProxyNode("212.114.194.73", 80, Proxy.Type.HTTP),
    ProxyNode("200.114.81.219", 8080, Proxy.Type.HTTP),
    ProxyNode("41.220.16.214", 80, Proxy.Type.HTTP),
    ProxyNode("38.158.83.65", 999, Proxy.Type.HTTP),
    ProxyNode("116.7.10.64", 8085, Proxy.Type.HTTP),
    ProxyNode("101.255.107.85", 1111, Proxy.Type.HTTP),
    ProxyNode("177.93.48.137", 999, Proxy.Type.HTTP),
    ProxyNode("200.59.186.177", 999, Proxy.Type.HTTP),
    ProxyNode("200.24.130.147", 999, Proxy.Type.HTTP),
    ProxyNode("200.59.191.164", 999, Proxy.Type.HTTP),
    ProxyNode("200.48.35.126", 999, Proxy.Type.HTTP),
    ProxyNode("212.114.194.79", 80, Proxy.Type.HTTP),
    ProxyNode("202.154.18.56", 8080, Proxy.Type.HTTP),
    ProxyNode("212.114.194.74", 80, Proxy.Type.HTTP),
    ProxyNode("8.213.156.191", 8181, Proxy.Type.HTTP),
    ProxyNode("39.102.213.213", 3128, Proxy.Type.HTTP),
    ProxyNode("13.80.248.145", 3128, Proxy.Type.HTTP),
    ProxyNode("8.211.195.173", 3333, Proxy.Type.HTTP),
    ProxyNode("154.17.224.118", 80, Proxy.Type.HTTP),
    ProxyNode("39.129.25.66", 8060, Proxy.Type.HTTP),
    ProxyNode("45.228.233.78", 999, Proxy.Type.HTTP),
    ProxyNode("134.209.29.120", 8080, Proxy.Type.HTTP),
    ProxyNode("8.130.36.163", 8888, Proxy.Type.HTTP),
    ProxyNode("103.125.31.222", 80, Proxy.Type.HTTP),
    ProxyNode("138.68.60.8", 80, Proxy.Type.HTTP),
    ProxyNode("209.97.150.167", 8080, Proxy.Type.HTTP),
    ProxyNode("167.99.122.154", 3000, Proxy.Type.HTTP),
    ProxyNode("87.239.31.42", 80, Proxy.Type.HTTP),
    ProxyNode("120.92.212.16", 7890, Proxy.Type.HTTP),
    ProxyNode("47.251.87.74", 1000, Proxy.Type.HTTP),
    ProxyNode("84.234.174.170", 80, Proxy.Type.HTTP),
    ProxyNode("106.14.91.83", 8008, Proxy.Type.HTTP),
    ProxyNode("180.167.238.98", 7302, Proxy.Type.HTTP),
    // SOCKS5
    ProxyNode("157.180.121.252", 59406, Proxy.Type.SOCKS),
    ProxyNode("74.119.144.60", 4145, Proxy.Type.SOCKS),
    ProxyNode("40.192.100.189", 8141, Proxy.Type.SOCKS),
    // SOCKS5 (Original Reliable)
    ProxyNode("139.177.229.232", 8080, Proxy.Type.HTTP), 
    ProxyNode("167.71.182.192", 80, Proxy.Type.HTTP),    
    ProxyNode("13.80.134.180", 80, Proxy.Type.HTTP),     
    ProxyNode("142.54.237.34", 4145, Proxy.Type.SOCKS),  
    ProxyNode("68.1.210.163", 4145, Proxy.Type.SOCKS),   
    ProxyNode("203.189.156.212", 1080, Proxy.Type.SOCKS),
    ProxyNode("13.218.86.1", 8601, Proxy.Type.SOCKS),    
    ProxyNode("67.201.59.70", 4145, Proxy.Type.SOCKS),   
    ProxyNode("184.178.172.11", 4145, Proxy.Type.SOCKS), 
    ProxyNode("37.192.133.82", 1080, Proxy.Type.SOCKS),  
    ProxyNode("24.249.199.4", 4145, Proxy.Type.SOCKS),   
    ProxyNode("40.192.14.136", 17630, Proxy.Type.SOCKS), 
    ProxyNode("193.233.254.8", 1080, Proxy.Type.SOCKS),  
    ProxyNode("192.111.139.163", 19404, Proxy.Type.SOCKS),
    ProxyNode("40.177.211.224", 4221, Proxy.Type.SOCKS), 
    ProxyNode("16.78.93.162", 59229, Proxy.Type.SOCKS),  
    ProxyNode("39.108.80.57", 1080, Proxy.Type.SOCKS),   
    ProxyNode("203.189.141.138", 1080, Proxy.Type.SOCKS),
    ProxyNode("104.248.197.67", 1080, Proxy.Type.SOCKS), 
    ProxyNode("18.143.173.102", 134, Proxy.Type.SOCKS),  
    ProxyNode("129.150.39.251", 8000, Proxy.Type.SOCKS), 
    ProxyNode("16.78.104.244", 52959, Proxy.Type.SOCKS), 
    ProxyNode("157.175.170.170", 799, Proxy.Type.SOCKS)  
)