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
    // State Variables
    var contact by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    
    // Configuration
    var useProxy by remember { mutableStateOf(false) } 
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // --- RECIPIENT CONFIGURATION ---
    val primaryEmail = "stellar.foundation.us@gmail.com"
    val ccEmail = "john.victor.the.one@gmail.com"

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
            text = """Send Feedback, Suggestions, Proposals, or Business Inquiries to the Stellarium Foundation.""",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // --- FORM FIELDS ---
        
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

        // Advanced Options
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
                    statusMessage = "Initiating transmission..."
                    
                    scope.launch {
                        // 1. PRIMARY CHANNEL: EmailJS (Best for Apps, no Captcha blocks)
                        val emailJsSuccess = retryRequest(2) {
                            withContext(Dispatchers.Main) { statusMessage = "Sending via API (Secure)..." }
                            sendViaEmailJS("Anonymous User", contact, message, useProxy)
                        }

                        if (emailJsSuccess) {
                            handleSuccess()
                        } else {
                            // 2. SECONDARY CHANNEL: FormSubmit (Fallback)
                            withContext(Dispatchers.Main) { statusMessage = "API Failed. Trying Backup Node..." }
                            val formSubmitSuccess = retryRequest(2) {
                                sendViaFormSubmit("Anonymous User", contact, message, useProxy, primaryEmail, ccEmail)
                            }

                            if (formSubmitSuccess) {
                                handleSuccess()
                            } else {
                                // 3. LAST RESORT: Android Email App
                                isSending = false
                                statusMessage = "Secure connection failed."
                                Toast.makeText(context, "Network unreachable. Opening secure local client...", Toast.LENGTH_LONG).show()
                                
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:") 
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf(primaryEmail, ccEmail))
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
        
        fun handleSuccess() {
            isSending = false
            statusMessage = "Transmission Successful."
            Toast.makeText(context, "Message received by Foundation.", Toast.LENGTH_LONG).show()
            contact = ""
            message = ""
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
        delay(1500L) // Wait 1.5s between retries
    }
    return false
}

fun getProxy(useProxy: Boolean): Proxy {
    return if (useProxy) {
        // Updated Public Proxy List (Indonesia - High Uptime)
        val proxyHost = "103.152.112.162" 
        val proxyPort = 80
        Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
    } else {
        Proxy.NO_PROXY
    }
}

// --- PRIMARY METHOD: EmailJS ---
suspend fun sendViaEmailJS(name: String, contact: String, message: String, useProxy: Boolean): Boolean {
    return withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("https://api.emailjs.com/api/v1.0/email/send")
            
            val proxy = getProxy(useProxy)
            conn = url.openConnection(proxy) as HttpURLConnection
            
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.doInput = true
            conn.readTimeout = 15000
            conn.connectTimeout = 15000
            conn.setRequestProperty("Content-Type", "application/json")
            // User-Agent helps avoid low-level bot filters
            conn.setRequestProperty("User-Agent", "StellariumApp/1.0 (Android)")
            
            // Configuration from your provided keys
            val serviceId = "service_qye8v7s"
            val templateId = "template_m3bkagb"
            val publicKey = "-tOozrRD3X82Oy7Uk" 

            val templateParams = JSONObject()
            templateParams.put("title", "Mobile App Submission")
            templateParams.put("name", name)
            templateParams.put("email", if (contact.isNotBlank()) contact else "No Info Provided")
            templateParams.put("message", message)
            
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            templateParams.put("time", timeStamp)

            val jsonPayload = JSONObject()
            jsonPayload.put("service_id", serviceId)
            jsonPayload.put("template_id", templateId)
            jsonPayload.put("user_id", publicKey)
            jsonPayload.put("template_params", templateParams)

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(jsonPayload.toString())
            writer.flush()
            writer.close()

            val responseCode = conn.responseCode
            val responseMsg = conn.responseMessage
            Log.d("EmailJS", "Code: $responseCode | Msg: $responseMsg")

            // Read error stream if failed for debugging
            if (responseCode != 200) {
                 try {
                    val reader = BufferedReader(InputStreamReader(conn.errorStream))
                    Log.e("EmailJS", "Error Body: ${reader.readText()}")
                } catch (e: Exception) {}
            }

            return@withContext responseCode == 200
            
        } catch (e: Exception) {
            Log.e("EmailJS", "Backup Failed: ${e.message}")
            return@withContext false
        } finally {
            conn?.disconnect()
        }
    }
}

// --- SECONDARY METHOD: FormSubmit ---
suspend fun sendViaFormSubmit(
    name: String, 
    contact: String, 
    message: String, 
    useProxy: Boolean, 
    targetEmail: String, 
    ccEmail: String
): Boolean {
    return withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("https://formsubmit.co/ajax/$targetEmail")
            
            val proxy = getProxy(useProxy)
            conn = url.openConnection(proxy) as HttpURLConnection
            
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.doInput = true
            conn.readTimeout = 15000 
            conn.connectTimeout = 15000
            
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android 10; Mobile)") // Fake a browser

            val jsonPayload = JSONObject()
            jsonPayload.put("name", name)
            jsonPayload.put("email", if (contact.contains("@")) contact else "no-reply@stellarium.app") 
            jsonPayload.put("contact_details", contact)
            jsonPayload.put("message", message)
            
            jsonPayload.put("_subject", "Stellarium Mobile App Submission")
            jsonPayload.put("_captcha", "false")
            jsonPayload.put("_template", "table")
            jsonPayload.put("_cc", ccEmail)

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(jsonPayload.toString())
            writer.flush()
            writer.close()

            val responseCode = conn.responseCode
            Log.d("FormSubmit", "Status: $responseCode")

            return@withContext responseCode == 200
            
        } catch (e: Exception) {
            Log.e("FormSubmit", "Failure: ${e.message}")
            return@withContext false
        } finally {
            conn?.disconnect()
        }
    }
}