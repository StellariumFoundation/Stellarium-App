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
                        // Send using EmailJS API
                        val success = sendViaEmailJS(name, contact, message)
                        isSending = false
                        
                        if (success) {
                            Toast.makeText(context, "Message sent successfully!", Toast.LENGTH_LONG).show()
                            name = ""
                            contact = ""
                            message = ""
                        } else {
                            // Fallback to local email client if API fails
                            Toast.makeText(context, "Opening Email Client...", Toast.LENGTH_SHORT).show()
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
 * Sends data directly to EmailJS REST API
 */
suspend fun sendViaEmailJS(name: String, contact: String, message: String): Boolean {
    return withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("https://api.emailjs.com/api/v1.0/email/send")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.doInput = true
            conn.setRequestProperty("Content-Type", "application/json")
            
            // --- EmailJS Configuration ---
            val serviceId = "service_qye8v7s"
            val templateId = "template_m3bkagb"
            val publicKey = "-tOozrRD3X82Oy7Uk" 

            // Prepare Template Parameters (Matches your screenshot)
            val templateParams = JSONObject()
            templateParams.put("title", "New App Submission")
            templateParams.put("name", if (name.isNotBlank()) name else "Anonymous")
            templateParams.put("email", if (contact.isNotBlank()) contact else "No contact info provided")
            templateParams.put("message", message)
            
            // Add Timestamp
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            templateParams.put("time", timeStamp)

            // Final Payload
            val jsonPayload = JSONObject()
            jsonPayload.put("service_id", serviceId)
            jsonPayload.put("template_id", templateId)
            jsonPayload.put("user_id", publicKey) // In REST API, user_id is the Public Key
            jsonPayload.put("template_params", templateParams)

            // Send Data
            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(jsonPayload.toString())
            writer.flush()
            writer.close()

            val responseCode = conn.responseCode
            val responseMsg = conn.responseMessage
            
            Log.d("EmailJS", "Code: $responseCode, Msg: $responseMsg")

            // EmailJS returns 200 OK for success
            if (responseCode == 200) {
                return@withContext true
            } else {
                // Log error details if failed
                try {
                    val reader = BufferedReader(InputStreamReader(conn.errorStream))
                    val errorResponse = reader.readText()
                    Log.e("EmailJS", "Error Response: $errorResponse")
                } catch (e: Exception) {
                    Log.e("EmailJS", "Could not read error stream")
                }
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e("EmailJS", "Exception: ${e.message}")
            e.printStackTrace()
            return@withContext false
        } finally {
            conn?.disconnect()
        }
    }
}