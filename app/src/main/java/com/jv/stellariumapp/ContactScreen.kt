package com.jv.stellariumapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun ContactScreen() {
    var name by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Contact Us", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Send a message directly to the Stellarium Foundation.")
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                // Here you would implement API logic to send the message
                Toast.makeText(context, "Message sent to John Victor!", Toast.LENGTH_SHORT).show()
                name = ""
                message = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send Message")
        }
    }
}