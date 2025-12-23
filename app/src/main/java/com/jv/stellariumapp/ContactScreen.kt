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
import android.net.Proxy
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ContactScreen() {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var useTor by remember { mutableStateOf(false) } // Toggle for Tor
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

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
            text = """Send Feedback, Suggestions, Compliments, Proposals, Business Partnerships, Customer Support Queries, etc...
to the Stellarium Foundation.""",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name or Institution (Optional)") },
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
            value = contact,
            onValueChange = { contact = it },
            label = { Text("Contact Back (Email/Social) (Optional)") },
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
            label = { Text("Write Your Message") },
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

        // Tor Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(checked = useTor, onCheckedChange = { useTor = it })
            Text(text = "Send Anonymously via Tor (Requires Orbot)", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (message.isNotBlank()) {
                    isSending = true
                    scope.launch {
                        // 1. Try FormSubmit
                        Log.d("Contact", "Attempting FormSubmit via Tor: $useTor")
                        var success = sendViaFormSubmit(name, contact, message, useTor)
                        
                        // 2. If Failed, Try EmailJS
                        if (!success) {
                            Log.d("Contact", "FormSubmit failed. Attempting EmailJS...")
                            success = sendViaEmailJS(name, contact, message, useTor)
                        }

                        isSending = false
                        
                        if (success) {
                            Toast.makeText(context, "Message sent successfully!", Toast.LENGTH_LONG).show()
                            name = ""
                            contact = ""
                            message = ""
                        } else {
                            // 3. Fallback to Android Intent
                            val errorMsg = if (useTor) "Tor failed. Is Orbot running?" else "Network failed."
                            Toast.makeText(context, "$errorMsg Opening Email...", Toast.LENGTH_SHORT).show()
                            
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:stellar.foundation.us@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Message from App: $name")
                                putExtra(Intent.EXTRA_TEXT, "Contact: $contact\n\nMessage:\n$message")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email client found.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(context, "Please write a message", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isSending,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Send Message")
            }
        }
    }
}

/**
 * Gets a Proxy object.
 * If useTor is true, returns a SOCKS proxy for Orbot (localhost:9050).
 * Otherwise returns Proxy.NO_PROXY.
 */
fun getProxy(useTor: Boolean): Proxy {
    return if (useTor) {
        // Standard Orbot SOCKS proxy port is 9050
        Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 9050))
    } else {
        Proxy.NO_PROXY
    }
}

/**
 * METHOD 1: FormSubmit.co
 * Robust, no API keys needed on client side.
 */
suspend fun sendViaFormSubmit(name: String, contact: String, message: String, useTor: Boolean): Boolean {
    return withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val targetEmail = "stellar.foundation.us@gmail.com"
            val url = URL("https://formsubmit.co/ajax/$targetEmail")
            
            // Open connection using the appropriate proxy
            val proxy = getProxy(useTor)
            conn = url.openConnection(proxy) as HttpURLConnection
            
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.doInput = true
            conn.readTimeout = 20000 // Longer timeout for Tor
            conn.connectTimeout = 20000
            
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")

            val jsonPayload = JSONObject()
            jsonPayload.put("name", if (name.isBlank()) "Anonymous" else name)
            // FormSubmit requires a valid-looking email field to prevent spam
            jsonPayload.put("email", if (contact.contains("@")) contact else "no-reply@stellarium.app") 
            jsonPayload.put("contact_details", contact)
            jsonPayload.put("message", message)
            jsonPayload.put("_subject", "Stellarium App Message")
            jsonPayload.put("_captcha", "false") // Disable captcha
            jsonPayload.put("_template", "table")

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(jsonPayload.toString())
            writer.flush()
            writer.close()

            val responseCode = conn.responseCode
            Log.d("FormSubmit", "Code: $responseCode")
            
            // Read response to ensure connection closes cleanly
            try {
                BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            } catch (e: Exception) { }

            return@withContext responseCode == 200
            
        } catch (e: Exception) {
            Log.e("FormSubmit", "Error: ${e.message}")
            return@withContext false
        } finally {
            conn?.disconnect()
        }
    }
}

/**
 * METHOD 2: EmailJS
 * Uses specific keys provided.
 */
suspend fun sendViaEmailJS(name: String, contact: String, message: String, useTor: Boolean): Boolean {
    return withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("https://api.emailjs.com/api/v1.0/email/send")
            
            // Open connection using the appropriate proxy
            val proxy = getProxy(useTor)
            conn = url.openConnection(proxy) as HttpURLConnection
            
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.doInput = true
            conn.readTimeout = 20000
            conn.connectTimeout = 20000
            conn.setRequestProperty("Content-Type", "application/json")
            
            // Keys provided by user
            val serviceId = "service_qye8v7s"
            val templateId = "template_m3bkagb"
            val publicKey = "-tOozrRD3X82Oy7Uk" 

            // Params matching the screenshot
            val templateParams = JSONObject()
            templateParams.put("title", "App Contact Form")
            templateParams.put("name", if (name.isNotBlank()) name else "Anonymous")
            templateParams.put("email", if (contact.isNotBlank()) contact else "No info")
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
            Log.d("EmailJS", "Code: $responseCode")

            return@withContext responseCode == 200
            
        } catch (e: Exception) {
            Log.e("EmailJS", "Exception: ${e.message}")
            return@withContext false
        } finally {
            conn?.disconnect()
        }
    }
}