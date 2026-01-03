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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.URL
import java.nio.charset.StandardCharsets

// --- DATA STRUCTURES ---
data class ProxyNode(val ip: String, val port: Int, val type: Proxy.Type)

@Composable
fun ContactScreen() {
    // UI State
    var contact by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    
    // Dialog State
    var showOrbotDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // --- LOGIC: START TRANSMISSION ---
    fun startTransmission(useTor: Boolean) {
        isSending = true
        showOrbotDialog = false
        
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { 
                statusMessage = if (useTor) "Routing via Tor Network..." else "Routing via Public Proxy Chain..." 
            }

            // 1. Configure the Proxy Strategy
            val proxyStrategy = if (useTor) {
                // Fixed Tor Proxy
                listOf(ProxyNode("127.0.0.1", 9050, Proxy.Type.SOCKS))
            } else {
                // Public Proxy Rotation (Shuffle for anonymity)
                publicProxies.shuffled()
            }

            // 2. Broadcast to Shimmer (IOTA Blockchain)
            var shimmerSuccess = false
            var shimmerBlockId = ""
            
            // Try to broadcast using the proxy strategy
            for (node in proxyStrategy) {
                val proxy = Proxy(node.type, InetSocketAddress.createUnresolved(node.ip, node.port))
                val result = sendViaShimmer(contact, message, proxy)
                if (result != null) {
                    shimmerSuccess = true
                    shimmerBlockId = result
                    break // Stop rotating once successful
                }
            }

            withContext(Dispatchers.Main) {
                if (shimmerSuccess) statusMessage = "Blockchain Confirmed. Sending Email..."
            }

            // 3. Send Email (Formspree -> FormSubmit Fallback)
            var emailSuccess = false
            
            // Try Formspree first with rotation
            for (node in proxyStrategy) {
                val proxy = Proxy(node.type, InetSocketAddress.createUnresolved(node.ip, node.port))
                if (sendViaFormspree(contact, message, proxy)) {
                    emailSuccess = true
                    break
                }
            }

            // If Formspree failed, try FormSubmit
            if (!emailSuccess) {
                for (node in proxyStrategy) {
                    val proxy = Proxy(node.type, InetSocketAddress.createUnresolved(node.ip, node.port))
                    if (sendViaFormSubmit(contact, message, proxy)) {
                        emailSuccess = true
                        break
                    }
                }
            }

            // 4. Final Status Update
            withContext(Dispatchers.Main) {
                isSending = false
                if (shimmerSuccess || emailSuccess) {
                    val status = StringBuilder("Transmission Complete.")
                    if (shimmerSuccess) status.append("\nBlock ID: $shimmerBlockId")
                    if (emailSuccess) status.append("\nEmail Sent.")
                    
                    statusMessage = status.toString()
                    Toast.makeText(context, "Secure Transmission Complete", Toast.LENGTH_LONG).show()
                    contact = ""
                    message = ""
                } else {
                    statusMessage = "All secure channels failed."
                    Toast.makeText(context, "Connection Failed. Try installing Orbot.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- UI LAYOUT ---
    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Secure Comms", 
            style = MaterialTheme.typography.headlineMedium, 
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Send Intelligence or Directives to the Stellarium Foundation.\n(Routes via Shimmer Blockchain & Encrypted Email)",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = contact,
            onValueChange = { contact = it },
            label = { Text("Contact (Optional)") },
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
            label = { Text("Intel / Message") },
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

        if (statusMessage.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusMessage,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                if (message.isNotBlank()) {
                    // Step 1: Check if Orbot (Tor) is running
                    scope.launch(Dispatchers.IO) {
                        val isTorAvailable = checkOrbotConnection()
                        withContext(Dispatchers.Main) {
                            if (isTorAvailable) {
                                // Tor is running, start immediately
                                startTransmission(useTor = true)
                            } else {
                                // Tor not found, show dialog
                                showOrbotDialog = true
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
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Anonymizing...")
            } else {
                Text("Broadcast Securely")
            }
        }
    }

    // --- DIALOG: TOR MISSING ---
    if (showOrbotDialog) {
        AlertDialog(
            onDismissRequest = { showOrbotDialog = false },
            title = { Text("Tor Network Not Detected") },
            text = { 
                Text("For maximum security, we recommend installing Orbot to route traffic through the Tor network.\n\nWithout Orbot, we will use public proxies, which are less secure.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://orbot.app/"))
                        context.startActivity(intent)
                        showOrbotDialog = false
                    }
                ) {
                    Text("Install Orbot")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // User chose to fallback to public proxies
                        startTransmission(useTor = false)
                    }
                ) {
                    Text("Use Public Proxies")
                }
            }
        )
    }
}

// =========================================================================
// ====================     NETWORKING LAYER     ===========================
// =========================================================================

// --- 1. TOR CHECKER ---
fun checkOrbotConnection(): Boolean {
    return try {
        val socket = Socket()
        socket.connect(InetSocketAddress("127.0.0.1", 9050), 500)
        socket.close()
        true
    } catch (e: Exception) {
        false
    }
}

// --- 2. SHIMMER BLOCKCHAIN (IOTA) ---
// Returns Block ID String if success, null otherwise
fun sendViaShimmer(contact: String, message: String, proxy: Proxy): String? {
    var conn: HttpURLConnection? = null
    try {
        // Shimmer Mainnet Node
        val nodeUrl = "https://api.shimmer.network/api/core/v2/blocks"
        val url = URL(nodeUrl)
        
        conn = url.openConnection(proxy) as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.doInput = true
        conn.readTimeout = 15000
        conn.connectTimeout = 15000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("User-Agent", "StellariumApp/1.0")

        // 1. Prepare Payload (Hex Encoded)
        val fullPayload = "CONTACT: $contact\n\nMESSAGE: $message"
        
        // Convert to Hex
        val hexData = fullPayload.toByteArray(StandardCharsets.UTF_8).joinToString("") { "%02x".format(it) }
        val hexTag = "STELLARIUM_INTEL_VAULT".toByteArray(StandardCharsets.UTF_8).joinToString("") { "%02x".format(it) }

        // 2. Build JSON
        val jsonPayload = JSONObject()
        jsonPayload.put("protocolVersion", 2)
        
        val payloadObj = JSONObject()
        payloadObj.put("type", 5) // Tagged Data
        payloadObj.put("tag", "0x$hexTag")
        payloadObj.put("data", "0x$hexData")
        
        jsonPayload.put("payload", payloadObj)
        jsonPayload.put("nonce", "0") // Rely on node for PoW or 0-value data

        // 3. Send
        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(jsonPayload.toString())
        writer.flush()
        writer.close()

        val responseCode = conn.responseCode
        if (responseCode in 200..299) {
            val responseBody = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val responseJson = JSONObject(responseBody)
            return responseJson.optString("blockId", "Unknown ID")
        }
        return null
    } catch (e: Exception) {
        // Log.e("Shimmer", "Fail: ${e.message}")
        return null
    } finally {
        conn?.disconnect()
    }
}

// --- 3. FORMSPREE ---
fun sendViaFormspree(contact: String, message: String, proxy: Proxy): Boolean {
    var conn: HttpURLConnection? = null
    try {
        val formId = "mzdpovoa" 
        val url = URL("https://formspree.io/f/$formId")
        
        conn = url.openConnection(proxy) as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.doInput = true
        conn.readTimeout = 10000
        conn.connectTimeout = 10000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")

        val jsonPayload = JSONObject()
        jsonPayload.put("email", if (contact.contains("@")) contact else "no-reply@stellarium.app")
        jsonPayload.put("message", message)
        jsonPayload.put("contact_details", contact)

        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(jsonPayload.toString())
        writer.flush()
        writer.close()

        return conn.responseCode == 200
    } catch (e: Exception) {
        return false
    } finally {
        conn?.disconnect()
    }
}

// --- 4. FORMSUBMIT (Backup) ---
fun sendViaFormSubmit(contact: String, message: String, proxy: Proxy): Boolean {
    var conn: HttpURLConnection? = null
    try {
        val url = URL("https://formsubmit.co/ajax/stellar.foundation.us@gmail.com")
        
        conn = url.openConnection(proxy) as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.readTimeout = 10000
        conn.connectTimeout = 10000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")

        val jsonPayload = JSONObject()
        jsonPayload.put("name", "Stellarium App User")
        jsonPayload.put("email", if (contact.contains("@")) contact else "no-reply@stellarium.app") 
        jsonPayload.put("contact_details", contact)
        jsonPayload.put("message", message)
        jsonPayload.put("_captcha", "false")

        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(jsonPayload.toString())
        writer.flush()
        writer.close()

        return conn.responseCode == 200
    } catch (e: Exception) {
        return false
    } finally {
        conn?.disconnect()
    }
}

// --- PROXY LIST (Mixed HTTP & SOCKS5) ---
val publicProxies = listOf(
    // HTTP
    ProxyNode("139.177.229.232", 8080, Proxy.Type.HTTP),
    ProxyNode("139.177.229.211", 8080, Proxy.Type.HTTP),
    ProxyNode("182.53.202.208", 8080, Proxy.Type.HTTP),
    ProxyNode("139.177.229.127", 8080, Proxy.Type.HTTP),
    ProxyNode("139.59.1.14", 8080, Proxy.Type.HTTP),
    ProxyNode("167.71.182.192", 80, Proxy.Type.HTTP),
    ProxyNode("39.102.211.64", 80, Proxy.Type.HTTP),
    ProxyNode("121.43.146.222", 8081, Proxy.Type.HTTP),
    ProxyNode("47.119.22.156", 9098, Proxy.Type.HTTP),
    ProxyNode("134.209.29.120", 8080, Proxy.Type.HTTP),
    ProxyNode("47.254.36.213", 50, Proxy.Type.HTTP),
    ProxyNode("139.177.229.31", 8080, Proxy.Type.HTTP),
    ProxyNode("13.80.134.180", 80, Proxy.Type.HTTP),
    ProxyNode("197.255.125.12", 80, Proxy.Type.HTTP),
    
    // SOCKS5 (Valid for Java)
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
    ProxyNode("104.248.197.67", 1080, Proxy.Type.SOCKS)
)