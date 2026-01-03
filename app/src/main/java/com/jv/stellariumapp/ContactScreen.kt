package com.jv.stellariumapp

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
import android.content.pm.PackageManager
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.util.Log
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
import java.net.URL
import java.net.Socket
import java.nio.charset.StandardCharsets

@Composable
fun ContactScreen() {
    var contact by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    
    var showTorDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val packageManager = context.packageManager

    val torProxy = ProxyNode("127.0.0.1", 9050, Proxy.Type.SOCKS)

    fun handleSuccess(channel: String) {
        isSending = false
        statusMessage = "Success via $channel."
        Toast.makeText(context, "Message Sent ($channel)", Toast.LENGTH_LONG).show()
        contact = ""
        message = ""
    }

    // Helper to check if Orbot is installed
    fun isOrbotInstalled(): Boolean {
        return try {
            packageManager.getApplicationInfo("org.torproject.android", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    // Helper to test if Tor SOCKS proxy is reachable (Orbot running & connected)
    suspend fun isTorAvailable(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                val address = InetSocketAddress.createUnresolved(torProxy.ip, torProxy.port)
                socket.connect(address, 3000) // 3-second timeout
                socket.close()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    if (showTorDialog) {
        AlertDialog(
            onDismissRequest = { showTorDialog = false },
            title = { Text("Tor (Orbot) Not Available") },
            text = {
                Text("For maximum anonymity, Orbot (Tor) is recommended.\n\nYou can download it now or proceed with public proxy rotation.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showTorDialog = false
                    // Proceed with public proxies
                    scope.launch {
                        statusMessage = "Routing via public proxy rotation..."
                        val shimmerSuccess = retryWithProxies(contact, message, "shimmer") { msg ->
                            withContext(Dispatchers.Main) { statusMessage = msg }
                        }
                        val formspreeSuccess = retryWithProxies(contact, message, "formspree") { msg ->
                            withContext(Dispatchers.Main) { statusMessage = msg }
                        }

                        if (formspreeSuccess || shimmerSuccess) {
                            val channel = if (formspreeSuccess) "Secure Email" else "Shimmer Tangle"
                            withContext(Dispatchers.Main) { handleSuccess(channel) }
                        } else {
                            val formSubmitSuccess = retryWithProxies(contact, message, "formsubmit") { msg ->
                                withContext(Dispatchers.Main) { statusMessage = msg }
                            }
                            if (formSubmitSuccess) {
                                withContext(Dispatchers.Main) { handleSuccess("Backup Email") }
                            } else {
                                withContext(Dispatchers.Main) {
                                    isSending = false
                                    statusMessage = "All channels unreachable."
                                    // Add offline email fallback if desired
                                }
                            }
                        }
                    }
                }) {
                    Text("Proceed with Proxies")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTorDialog = false
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://play.google.com/store/apps/details?id=org.torproject.android")
                    }
                    context.startActivity(intent)
                }) {
                    Text("Download Orbot")
                }
            }
        )
    }

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
            text = "Send Intelligence, Proposals, or Directives to the Stellarium Foundation.",
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

        Text(
            text = "Maximum anonymity via Tor (Orbot) is prioritized. Public proxies used only if Tor unavailable.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = {
                if (isOrbotInstalled()) {
                    val launchIntent = packageManager.getLaunchIntentForPackage("org.torproject.android")
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    } else {
                        Toast.makeText(context, "Orbot installed but cannot launch.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://play.google.com/store/apps/details?id=org.torproject.android")
                    }
                    context.startActivity(intent)
                }
            }
        ) {
            Text(if (isOrbotInstalled()) "Open Orbot (Start Tor)" else "Download Orbot (Recommended)")
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
                    isSending = true
                    statusMessage = "Initiating Sequence..."
                    
                    scope.launch {
                        val torAvailable = isTorAvailable()

                        if (torAvailable) {
                            statusMessage = "Tor (Orbot) detected – routing via Tor..."
                            val success = retryWithSingleProxy(torProxy, contact, message) { msg ->
                                withContext(Dispatchers.Main) { statusMessage = msg }
                            }
                            if (success) {
                                withContext(Dispatchers.Main) { handleSuccess("Tor (Orbot)") }
                            } else {
                                withContext(Dispatchers.Main) {
                                    isSending = false
                                    statusMessage = "Tor channels failed. Check Orbot connection."
                                }
                            }
                        } else {
                            isSending = false
                            showTorDialog = true
                        }
                    }
                } else {
                    Toast.makeText(context, "Intel required.", Toast.LENGTH_SHORT).show()
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
                Text("Transmitting...")
            } else {
                Text("Broadcast Message")
            }
        }
    }
}

// --- PROXY DATA ---
data class ProxyNode(val ip: String, val port: Int, val type: Proxy.Type)

val publicProxies = listOf(
    // HTTP Proxies
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
    ProxyNode("183.215.23.242", 9091, Proxy.Type.HTTP),

    // SOCKS5 Proxies
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

// --- SINGLE PROXY (TOR) RETRY ---
suspend fun retryWithSingleProxy(
    proxyNode: ProxyNode,
    contact: String,
    message: String,
    updateStatus: suspend (String) -> Unit
): Boolean {
    return withContext(Dispatchers.IO) {
        updateStatus("Routing via Tor (${proxyNode.ip}:${proxyNode.port})...")
        val socketAddress = InetSocketAddress.createUnresolved(proxyNode.ip, proxyNode.port)
        val proxy = Proxy(proxyNode.type, socketAddress)

        // Try services in prioritized order
        if (executeService("shimmer", contact, message, proxy)) return@withContext true
        if (executeService("formspree", contact, message, proxy)) return@withContext true
        if (executeService("formsubmit", contact, message, proxy)) return@withContext true

        false
    }
}

// --- PUBLIC PROXY ROTATOR (fallback) ---
suspend fun retryWithProxies(
    contact: String, 
    message: String, 
    service: String,
    updateStatus: suspend (String) -> Unit
): Boolean {
    return withContext(Dispatchers.IO) {
        val shuffledProxies = publicProxies.shuffled()
        var attempt = 1
        
        for (node in shuffledProxies) {
            updateStatus("Routing $service via Node $attempt/${shuffledProxies.size} (${node.ip})...")
            
            val socketAddress = InetSocketAddress.createUnresolved(node.ip, node.port)
            val proxy = Proxy(node.type, socketAddress)
            
            if (executeService(service, contact, message, proxy)) {
                Log.d("ProxyManager", "Tunnel Established: ${node.ip}")
                return@withContext true
            }
            
            attempt++
        }

        false
    }
}

// --- SERVICE DISPATCHER ---
suspend fun executeService(service: String, contact: String, message: String, proxy: Proxy): Boolean {
    return when (service) {
        "shimmer" -> sendViaShimmer(contact, message, proxy)
        "formspree" -> sendViaFormspree(contact, message, proxy)
        "formsubmit" -> sendViaFormSubmit(contact, message, proxy)
        else -> false
    }
}

// --- 1. SHIMMER TANGLE (Blockchain) ---
suspend fun sendViaShimmer(contact: String, message: String, proxy: Proxy): Boolean {
    var conn: HttpURLConnection? = null
    try {
        val nodeUrl = "https://api.shimmer.network/api/core/v2/blocks"
        val url = URL(nodeUrl)
        
        conn = url.openConnection(proxy) as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.doInput = true
        conn.readTimeout = 15000
        conn.connectTimeout = 15000
        conn.setRequestProperty("Content-Type", "application/json")

        val fullPayload = "CONTACT: $contact\n\nMESSAGE: $message"
        val hexData = fullPayload.toByteArray(StandardCharsets.UTF_8).joinToString("") { "%02x".format(it) }
        val hexTag = "STELLARIUM_INTEL_VAULT".toByteArray(StandardCharsets.UTF_8).joinToString("") { "%02x".format(it) }

        val jsonPayload = JSONObject()
        jsonPayload.put("protocolVersion", 2)
        
        val payloadObj = JSONObject()
        payloadObj.put("type", 5) // Tagged Data
        payloadObj.put("tag", "0x$hexTag")
        payloadObj.put("data", "0x$hexData")
        
        jsonPayload.put("payload", payloadObj)
        jsonPayload.put("nonce", "0") 

        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(jsonPayload.toString())
        writer.flush()
        writer.close()

        val responseCode = conn.responseCode
        if (responseCode in 200..299) {
            val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val json = JSONObject(response)
            val blockId = json.getString("blockId")
            Log.d("Shimmer", "Block ID: $blockId")

            delay(5000)

            var verifyConn: HttpURLConnection? = null
            try {
                val verifyUrl = URL("$nodeUrl/$blockId")
                verifyConn = verifyUrl.openConnection(proxy) as HttpURLConnection
                verifyConn.requestMethod = "GET"
                verifyConn.readTimeout = 15000
                verifyConn.connectTimeout = 15000

                return verifyConn.responseCode == 200
            } catch (e: Exception) {
                Log.e("Shimmer", "Verification failed: ${e.message}")
                return false
            } finally {
                verifyConn?.disconnect()
            }
        } else {
            return false
        }
    } catch (e: Exception) {
        Log.e("Shimmer", "Error: ${e.message}")
        return false
    } finally {
        conn?.disconnect()
    }
}

// --- 2. FORMSPREE (Primary Email) ---
suspend fun sendViaFormspree(contact: String, message: String, proxy: Proxy): Boolean {
    var conn: HttpURLConnection? = null
    try {
        val formId = "mzdpovoa" 
        val url = URL("https://formspree.io/f/$formId")
        
        conn = url.openConnection(proxy) as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.readTimeout = 10000
        conn.connectTimeout = 10000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

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
        Log.e("Formspree", "Error: ${e.message}")
        return false
    } finally {
        conn?.disconnect()
    }
}

// --- 3. FORMSUBMIT (Backup Email) ---
suspend fun sendViaFormSubmit(contact: String, message: String, proxy: Proxy): Boolean {
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
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

        val jsonPayload = JSONObject()
        jsonPayload.put("name", "Stellarium App User")
        jsonPayload.put("email", if (contact.contains("@")) contact else "no-reply@stellarium.app") 
        jsonPayload.put("contact_details", contact)
        jsonPayload.put("message", message)
        jsonPayload.put("_captcha", "false")
        jsonPayload.put("_cc", "john.victor.the.one@gmail.com")

        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(jsonPayload.toString())
        writer.flush()
        writer.close()

        return conn.responseCode == 200
    } catch (e: Exception) {
        Log.e("FormSubmit", "Error: ${e.message}")
        return false
    } finally {
        conn?.disconnect()
    }
}