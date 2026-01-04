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
import java.nio.charset.StandardCharsets

@Composable
fun ContactScreen() {
    var contact by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    
    // Toggles
    var useProxy by remember { mutableStateOf(true) } 
    var sendToNostr by remember { mutableStateOf(true) } // Nostr instead of IOTA
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun handleSuccess(channel: String) {
        isSending = false
        statusMessage = "Success via $channel."
        Toast.makeText(context, "Message Sent ($channel)", Toast.LENGTH_LONG).show()
        contact = ""
        message = ""
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
            text = "Send Intelligence. Routes via Nostr Relays (Uncensorable) & Encrypted Email.",
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

        // Options Row
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useProxy, onCheckedChange = { useProxy = it })
                Text("Hide IP (Proxy Rotation)", style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = sendToNostr, onCheckedChange = { sendToNostr = it })
                Text("Broadcast to Nostr Network", style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusMessage,
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (message.isNotBlank()) {
                    isSending = true
                    statusMessage = "Initiating Sequence..."
                    
                    scope.launch {
                        // 1. NOSTR BROADCAST (Priority)
                        if (sendToNostr) {
                            val nostrSuccess = retryWithProxies(contact, message, useProxy, "nostr") { msg ->
                                withContext(Dispatchers.Main) { statusMessage = msg }
                            }
                            if (nostrSuccess) {
                                withContext(Dispatchers.Main) { handleSuccess("Nostr Network") }
                                // We continue to email for redundancy
                            }
                        }

                        // 2. EMAIL CHANNEL (Formspree)
                        val formspreeSuccess = retryWithProxies(contact, message, useProxy, "formspree") { msg ->
                            withContext(Dispatchers.Main) { statusMessage = msg }
                        }

                        if (formspreeSuccess) {
                            withContext(Dispatchers.Main) { handleSuccess("Secure Email") }
                        } else {
                            // 3. FALLBACK (FormSubmit)
                            val formSubmitSuccess = retryWithProxies(contact, message, useProxy, "formsubmit") { msg ->
                                withContext(Dispatchers.Main) { statusMessage = msg }
                            }
                            
                            if (formSubmitSuccess) {
                                withContext(Dispatchers.Main) { handleSuccess("Backup Email") }
                            } else {
                                // 4. OFFLINE FALLBACK
                                withContext(Dispatchers.Main) {
                                    isSending = false
                                    statusMessage = "Network Unreachable."
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:stellar.foundation.us@gmail.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "Stellarium Intel")
                                        putExtra(Intent.EXTRA_TEXT, "Contact: $contact\n\n$message")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No email client found.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
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
                Text("Broadcast")
            }
        }
    }
}

// --- PROXY DATA ---
data class ProxyNode(val ip: String, val port: Int, val type: Proxy.Type)

val anonymousProxies = listOf(
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

// --- PROXY ROTATOR ENGINE ---

suspend fun retryWithProxies(
    contact: String, 
    message: String, 
    useProxy: Boolean, 
    service: String,
    updateStatus: suspend (String) -> Unit
): Boolean {
    return withContext(Dispatchers.IO) {
        if (!useProxy) return@withContext executeService(service, contact, message, Proxy.NO_PROXY)

        val shuffledProxies = anonymousProxies.shuffled()
        var attempt = 1
        
        for (node in shuffledProxies) {
            updateStatus("Routing via Node $attempt/${shuffledProxies.size} (${node.ip})...")
            try {
                val socketAddress = InetSocketAddress.createUnresolved(node.ip, node.port)
                val proxy = Proxy(node.type, socketAddress)
                
                // For Nostr, we must pass the proxy to the NostrService.publishMessage
                // For simplicity, we use a global variable or modify the service
                // But here, we handle it inside executeService by passing the proxy object
                if (executeService(service, contact, message, proxy)) return@withContext true
            } catch(e: Exception) {}
            attempt++
        }
        return@withContext false
    }
}

fun executeService(service: String, contact: String, message: String, proxy: Proxy): Boolean {
    return when (service) {
        "nostr" -> sendViaNostrProtocol(contact, message, proxy) // NEW FUNCTION
        "formspree" -> sendViaFormspree(contact, message, proxy)
        "formsubmit" -> sendViaFormSubmit(contact, message, proxy)
        else -> false
    }
}

// --- 1. NOSTR PROTOCOL (With Proxy Support) ---
fun sendViaNostrProtocol(contact: String, message: String, proxy: Proxy): Boolean {
    // We call the NostrService here, but we need to tell it to use the proxy
    // Since NostrService is an Object, we can add a method to it that accepts a proxy
    // OR we can define the broadcasting logic right here for simplicity within the rotation loop.
    
    // See implementation below in NostrService.kt update
    return NostrService.publishMessageWithProxy(contact, message, proxy)
}

// --- 2. FORMSPREE ---
fun sendViaFormspree(contact: String, message: String, proxy: Proxy): Boolean {
    var conn: HttpURLConnection? = null
    try {
        val formId = "mzdpovoa" 
        val url = URL("https://formspree.io/f/$formId")
        
        conn = url.openConnection(proxy) as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.readTimeout = 8000
        conn.connectTimeout = 8000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        
        val jsonPayload = JSONObject()
        jsonPayload.put("email", if (contact.contains("@")) contact else "anon@stellarium.app")
        jsonPayload.put("message", message)
        jsonPayload.put("contact_details", contact)

        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(jsonPayload.toString())
        writer.flush()
        writer.close()

        return conn.responseCode in 200..299
    } catch (e: Exception) {
        return false
    } finally {
        conn?.disconnect()
    }
}

// --- 3. FORMSUBMIT ---
fun sendViaFormSubmit(contact: String, message: String, proxy: Proxy): Boolean {
    var conn: HttpURLConnection? = null
    try {
        val url = URL("https://formsubmit.co/ajax/stellar.foundation.us@gmail.com")
        conn = url.openConnection(proxy) as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.readTimeout = 8000
        conn.connectTimeout = 8000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")

        val jsonPayload = JSONObject()
        jsonPayload.put("name", "Stellarium App User")
        jsonPayload.put("email", if (contact.contains("@")) contact else "no-reply@stellarium.app") 
        jsonPayload.put("message", message)
        jsonPayload.put("_captcha", "false")
        jsonPayload.put("_cc", "john.victor.the.one@gmail.com")

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