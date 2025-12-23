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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // --- HELPER FUNCTION ---
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
            Text(text = "Route via Proxy (Anonymity Layer)", style = MaterialTheme.typography.bodyMedium)
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
                        // 1. PRIMARY: Formspree (High Reliability)
                        val formspreeSuccess = retryRequest(3) {
                            withContext(Dispatchers.Main) { statusMessage = "Routing via Formspree..." }
                            sendViaFormspree(contact, message, useProxy)
                        }

                        if (formspreeSuccess) {
                            withContext(Dispatchers.Main) { handleSuccess() }
                        } else {
                            // 2. BACKUP: FormSubmit
                            withContext(Dispatchers.Main) { statusMessage = "Primary failed. Rerouting..." }
                            val formSubmitSuccess = retryRequest(2) {
                                sendViaFormSubmit(contact, message, useProxy)
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

// --- UTILITIES ---

suspend fun retryRequest(times: Int, block: suspend () -> Boolean): Boolean {
    var currentAttempt = 0
    while (currentAttempt < times) {
        try {
            if (block()) return true
        } catch (e: Exception) {
            Log.e("EnterpriseRetry", "Attempt $currentAttempt failed: ${e.message}")
        }
        currentAttempt++
        delay(1500L)
    }
    return false
}

fun getProxy(useProxy: Boolean): Proxy {
    return if (useProxy) {
        // High-Availability Public Proxy
        val proxyHost = "103.152.112.162" 
        val proxyPort = 80
        Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
    } else {
        Proxy.NO_PROXY
    }
}

// --- PRIMARY CHANNEL: Formspree ---
suspend fun sendViaFormspree(contact: String, message: String, useProxy: Boolean): Boolean {
    return withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            // REPLACE THIS ID WITH YOUR ACTUAL FORMSPREE FORM ID
            // Go to https://formspree.io/ -> Create Form -> Copy ID (e.g. "mzdpovoa")
            val formId = "mzdpovoa" 
            val url = URL("https://formspree.io/f/$formId")
            
            val proxy = getProxy(useProxy)
            conn = url.openConnection(proxy) as HttpURLConnection
            
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.doInput = true
            conn.readTimeout = 15000
            conn.connectTimeout = 15000
            
            // Standard JSON headers
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            
            // Anti-Bot: Fake User Agent
            val userAgents = listOf(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15",
                "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
            )
            conn.setRequestProperty("User-Agent", userAgents.random())

            val jsonPayload = JSONObject()
            jsonPayload.put("email", if (contact.contains("@")) contact else "no-reply@stellarium.app")
            jsonPayload.put("message", message)
            // Add custom field for non-email contact info
            jsonPayload.put("contact_details", contact)

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(jsonPayload.toString())
            writer.flush()
            writer.close()

            val responseCode = conn.responseCode
            Log.d("Formspree", "Status: $responseCode")

            // Read response
            try {
                BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            } catch (e: Exception) {}

            return@withContext responseCode == 200
            
        } catch (e: Exception) {
            Log.e("Formspree", "Error: ${e.message}")
            return@withContext false
        } finally {
            conn?.disconnect()
        }
    }
}

// --- BACKUP CHANNEL: FormSubmit ---
suspend fun sendViaFormSubmit(contact: String, message: String, useProxy: Boolean): Boolean {
    return withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val targetEmail = "stellar.foundation.us@gmail.com"
            val url = URL("https://formsubmit.co/ajax/$targetEmail")
            
            val proxy = getProxy(useProxy)
            conn = url.openConnection(proxy) as HttpURLConnection
            
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.doInput = true
            conn.readTimeout = 10000 
            conn.connectTimeout = 10000
            
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
            
            // CC Logic
            jsonPayload.put("_cc", "john.victor.the.one@gmail.com")

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(jsonPayload.toString())
            writer.flush()
            writer.close()

            val responseCode = conn.responseCode
            return@withContext responseCode == 200
            
        } catch (e: Exception) {
            Log.e("FormSubmit", "Error: ${e.message}")
            return@withContext false
        } finally {
            conn?.disconnect()
        }
    }
}