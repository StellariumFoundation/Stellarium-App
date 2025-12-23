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

@Composable
fun ContactScreen() {
    var contact by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var useProxy by remember { mutableStateOf(false) } 
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun handleSuccess() {
        isSending = false
        statusMessage = "Transmission Successful."
        Toast.makeText(context, "Message received by Foundation.", Toast.LENGTH_LONG).show()
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
            Text(text = "Route via Anonymity Network (Auto-Retry)", style = MaterialTheme.typography.bodyMedium)
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
                    statusMessage = "Initiating secure transmission..."
                    
                    scope.launch {
                        // 1. PRIMARY: Formspree with Robust Proxy Rotation
                        val formspreeSuccess = sendWithProxyRotation(contact, message, useProxy, "formspree") { msg ->
                            withContext(Dispatchers.Main) { statusMessage = msg }
                        }

                        if (formspreeSuccess) {
                            withContext(Dispatchers.Main) { handleSuccess() }
                        } else {
                            // 2. BACKUP: FormSubmit with Proxy Rotation
                            withContext(Dispatchers.Main) { statusMessage = "Primary failed. Attempting Backup..." }
                            val formSubmitSuccess = sendWithProxyRotation(contact, message, useProxy, "formsubmit") { msg ->
                                withContext(Dispatchers.Main) { statusMessage = msg }
                            }

                            if (formSubmitSuccess) {
                                withContext(Dispatchers.Main) { handleSuccess() }
                            } else {
                                // 3. FALLBACK: Local Email App
                                withContext(Dispatchers.Main) {
                                    isSending = false
                                    statusMessage = "Secure connection failed."
                                    Toast.makeText(context, "Network unreachable. Opening secure local client...", Toast.LENGTH_LONG).show()
                                    
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:stellar.foundation.us@gmail.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "Secure Message (Anonymous)")
                                        putExtra(Intent.EXTRA_TEXT, "Contact: $contact\n\nMessage:\n$message")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No email client installed.", Toast.LENGTH_SHORT).show()
                                    }
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
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Transmitting...")
            } else {
                Text("Send Message")
            }
        }
    }
}

// --- PROXY MANAGEMENT ---

data class ProxyNode(val ip: String, val port: Int, val type: Proxy.Type)

val anonymousProxies = listOf(
    // HTTP Proxies (Usually more reliable for REST)
    ProxyNode("139.177.229.232", 8080, Proxy.Type.HTTP), 
    ProxyNode("139.177.229.211", 8080, Proxy.Type.HTTP), 
    ProxyNode("182.53.202.208", 8080, Proxy.Type.HTTP),  
    ProxyNode("139.177.229.127", 8080, Proxy.Type.HTTP), 
    ProxyNode("139.59.1.14", 8080, Proxy.Type.HTTP),     
    ProxyNode("167.71.182.192", 80, Proxy.Type.HTTP),    
    ProxyNode("134.209.29.120", 8080, Proxy.Type.HTTP),  
    ProxyNode("13.80.134.180", 80, Proxy.Type.HTTP),     
    ProxyNode("197.255.125.12", 80, Proxy.Type.HTTP),    

    // SOCKS5 Proxies (Only use if validated as SOCKS5)
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

// --- INTELLIGENT SENDING LOGIC ---

suspend fun sendWithProxyRotation(
    contact: String, 
    message: String, 
    useProxy: Boolean, 
    service: String,
    updateStatus: suspend (String) -> Unit
): Boolean {
    return withContext(Dispatchers.IO) {
        // If proxy is disabled, just try direct once
        if (!useProxy) {
            return@withContext if (service == "formspree") {
                sendViaFormspree(contact, message, Proxy.NO_PROXY)
            } else {
                sendViaFormSubmit(contact, message, Proxy.NO_PROXY)
            }
        }

        // If proxy enabled, shuffle and try ALL of them
        val shuffledProxies = anonymousProxies.shuffled()
        var attempt = 1
        
        for (node in shuffledProxies) {
            updateStatus("Routing via Proxy $attempt/${shuffledProxies.size} (${node.ip})...")
            
            val proxy = Proxy(node.type, InetSocketAddress(node.ip, node.port))
            val success = if (service == "formspree") {
                sendViaFormspree(contact, message, proxy)
            } else {
                sendViaFormSubmit(contact, message, proxy)
            }

            if (success) {
                Log.d("ProxyManager", "Success with ${node.ip}")
                return@withContext true
            }
            
            // Short delay before next attempt
            Log.e("ProxyManager", "Failed with ${node.ip}. Retrying...")
            attempt++
            // delay(200) // Optional: very short delay to not freeze UI
        }

        return@withContext false // All proxies failed
    }
}

// --- PRIMARY CHANNEL: Formspree ---
fun sendViaFormspree(contact: String, message: String, proxy: Proxy): Boolean {
    var conn: HttpURLConnection? = null
    try {
        val formId = "mzdpovoa" 
        val url = URL("https://formspree.io/f/$formId")
        
        conn = url.openConnection(proxy) as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.doInput = true
        conn.readTimeout = 8000 // Fast timeout for proxies
        conn.connectTimeout = 8000
        
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        
        val userAgents = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15"
        )
        conn.setRequestProperty("User-Agent", userAgents.random())

        val jsonPayload = JSONObject()
        jsonPayload.put("email", if (contact.contains("@")) contact else "no-reply@stellarium.app")
        jsonPayload.put("message", message)
        jsonPayload.put("contact_details", contact)

        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(jsonPayload.toString())
        writer.flush()
        writer.close()

        val responseCode = conn.responseCode
        // Read response to close stream cleanly
        try {
            BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        } catch (e: Exception) {}

        return responseCode == 200
        
    } catch (e: Exception) {
        // Log.e("Formspree", "Error: ${e.message}") // Optional logging
        return false
    } finally {
        conn?.disconnect()
    }
}

// --- BACKUP CHANNEL: FormSubmit ---
fun sendViaFormSubmit(contact: String, message: String, proxy: Proxy): Boolean {
    var conn: HttpURLConnection? = null
    try {
        val targetEmail = "stellar.foundation.us@gmail.com"
        val url = URL("https://formsubmit.co/ajax/$targetEmail")
        
        conn = url.openConnection(proxy) as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.doInput = true
        conn.readTimeout = 8000 
        conn.connectTimeout = 8000
        
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        
        val userAgents = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15"
        )
        conn.setRequestProperty("User-Agent", userAgents.random())

        val jsonPayload = JSONObject()
        jsonPayload.put("name", "Stellarium App User")
        jsonPayload.put("email", if (contact.contains("@")) contact else "no-reply@stellarium.app") 
        jsonPayload.put("contact_details", contact)
        jsonPayload.put("message", message)
        jsonPayload.put("_subject", "Stellarium Mobile App Submission")
        jsonPayload.put("_captcha", "false")
        jsonPayload.put("_template", "table")
        jsonPayload.put("_cc", "john.victor.the.one@gmail.com")

        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(jsonPayload.toString())
        writer.flush()
        writer.close()

        val responseCode = conn.responseCode
        return responseCode == 200
        
    } catch (e: Exception) {
        return false
    } finally {
        conn?.disconnect()
    }
}