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
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun ContactScreen() {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    
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
to the Stellarium Foundation and Radiohead.""",
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (message.isNotBlank()) {
                    isSending = true
                    scope.launch {
                        val success = sendToGoogleForm(name, contact, message)
                        isSending = false
                        if (success) {
                            Toast.makeText(context, "Message sent successfully!", Toast.LENGTH_LONG).show()
                            name = ""
                            contact = ""
                            message = ""
                        } else {
                            // FALLBACK: If API fails, let user open browser
                            Toast.makeText(context, "Opening Browser to Send...", Toast.LENGTH_SHORT).show()
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/forms/d/e/1FAIpQLSfIy7Wu4pZM-JsW146zYD1W_Y_tUE5EU4iVjkOu1Es2lvrDmQ/viewform"))
                            context.startActivity(browserIntent)
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

suspend fun sendToGoogleForm(name: String, contact: String, message: String): Boolean {
    return withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val formUrl = "https://docs.google.com/forms/d/e/1FAIpQLSfIy7Wu4pZM-JsW146zYD1W_Y_tUE5EU4iVjkOu1Es2lvrDmQ/formResponse"
            val viewUrl = "https://docs.google.com/forms/d/e/1FAIpQLSfIy7Wu4pZM-JsW146zYD1W_Y_tUE5EU4iVjkOu1Es2lvrDmQ/viewform"
            
            val finalMessage = StringBuilder()
            if (name.isNotBlank()) finalMessage.append("Name/Institution: $name\n")
            if (contact.isNotBlank()) finalMessage.append("Contact Back: $contact\n")
            finalMessage.append("\nMessage:\n$message")
            
            // Google needs a valid looking email if the field is "Email"
            val emailToSend = if (contact.contains("@") && contact.contains(".")) contact else "anonymous@stellarium.app"

            val postData = StringBuilder()
            postData.append(URLEncoder.encode("emailAddress", "UTF-8") + "=" + URLEncoder.encode(emailToSend, "UTF-8"))
            postData.append("&")
            postData.append(URLEncoder.encode("entry.474494390", "UTF-8") + "=" + URLEncoder.encode(finalMessage.toString(), "UTF-8"))
            
            // Hidden fields to mimic real browser
            postData.append("&")
            postData.append(URLEncoder.encode("pageHistory", "UTF-8") + "=" + URLEncoder.encode("0", "UTF-8"))
            postData.append("&")
            postData.append(URLEncoder.encode("fvv", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8"))

            val url = URL(formUrl)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.doInput = true
            conn.instanceFollowRedirects = true
            
            // CRITICAL HEADERS FOR GOOGLE FORMS
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("Referer", viewUrl) // <--- THIS OFTEN FIXES IT
            
            val userAgents = listOf(
                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
            )
            conn.setRequestProperty("User-Agent", userAgents.random())

            val outputBytes = postData.toString().toByteArray(Charsets.UTF_8)
            conn.outputStream.use { os ->
                os.write(outputBytes)
                os.flush()
            }
            
            val responseCode = conn.responseCode
            // Read response to close stream cleanly
            try {
                BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            } catch (e: Exception) { /* Ignored */ }

            Log.d("ContactForm", "Code: $responseCode")
            
            // 200 = Success
            return@withContext responseCode == 200
            
        } catch (e: Exception) {
            Log.e("ContactForm", "Failed: ${e.message}")
            return@withContext false
        } finally {
            conn?.disconnect()
        }
    }
}