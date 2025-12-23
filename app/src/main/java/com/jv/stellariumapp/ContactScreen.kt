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
    // Removed Name state variable
    var contact by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var useProxy by remember { mutableStateOf(false) } 
    
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
        
        // --- NAME FIELD REMOVED ---
        
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

        // Proxy Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(checked = useProxy, onCheckedChange = { useProxy = it })
            Text(text = "Hide IP (Use Public Proxy)", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (message.isNotBlank()) {
                    isSending = true
                    scope.launch {
                        // 1. Try FormSubmit (Primary)
                        Log.d("Contact", "Attempting FormSubmit. Proxy: $useProxy")
                        // Hardcode "Anonymous" as the name since field is removed
                        var success = sendViaFormSubmit("Anonymous User", contact, message, useProxy)
                        
                        // 2. If Failed, Try EmailJS (Backup)
                        if (!success) {
                            Log.d("Contact", "FormSubmit failed. Attempting EmailJS...")
                            success = sendViaEmailJS("Anonymous User", contact, message, useProxy)
                        }

                        isSending = false
                        
                        if (success) {
                            Toast.makeText(context, "Message sent successfully!", Toast.LENGTH_LONG).show()
                            contact = ""
                            message = ""
                        } else {
                            // 3. Fallback to Android Intent (Last Resort)
                            val errorMsg = if (useProxy) "Proxy connection failed." else "Network failed."
                            Toast.makeText(context, "$errorMsg Opening Email...", Toast.LENGTH_SHORT).show()
                            
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:stellar.foundation.us@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Message from App (Anonymous)")
                                putExtra(Intent.EXTRA_TEXT, "Contact Info: $contact\n\nMessage:\n$message")
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
 * Returns a Proxy object based on user selection.
 */
fun getProxy(useProxy: Boolean): Proxy {
    return if (useProxy) {
        // --- CONFIGURE YOUR PUBLIC PROXY HERE ---
        // Find a working HTTPS/HTTP proxy from: https://spys.one/en/
        val proxyHost = "202.162.212.164" 
        val proxyPort = 80
        Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
    } else {
        Proxy.NO_PROXY
    }
}

/**
 * METHOD 1: FormSubmit.co
 */
suspend fun sendViaFormSubmit(name: String, contact: String, message: String, useProxy: Boolean): Boolean {
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
            conn.readTimeout = 15000 
            conn.connectTimeout = 15000
            
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")

            val jsonPayload = JSONObject()
            // Send hardcoded name or whatever is passed
            jsonPayload.put("name", name) 
            jsonPayload.put("email", if (contact.contains("@")) contact else "no-reply@stellarium.app") 
            jsonPayload.put("contact_details", contact)
            jsonPayload.put("message", message)
            jsonPayload.put("_subject", "Stellarium App Submission")
            jsonPayload.put("_captcha", "false")
            jsonPayload.put("_template", "table")

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(jsonPayload.toString())
            writer.flush()
            writer.close()

            val responseCode = conn.responseCode
            Log.d("FormSubmit", "Code: $responseCode")
            
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
 */
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
            
            val serviceId = "service_qye8v7s"
            val templateId = "template_m3bkagb"
            val publicKey = "-tOozrRD3X82Oy7Uk" 

            val templateParams = JSONObject()
            templateParams.put("title", "App Contact Form")
            templateParams.put("name", name) // Hardcoded "Anonymous User" passed from UI
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