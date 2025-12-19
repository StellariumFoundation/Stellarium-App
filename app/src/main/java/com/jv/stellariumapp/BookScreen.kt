package com.jv.stellariumapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

// --- Data Model matching your JSON structure ---
data class LiteratureBook(
    val id: String,
    val title: String,
    val description: String, // Maps to 'comment_from_index'
    val content: String,
    val notionUrl: String
)

@Composable
fun BookScreen() {
    val context = LocalContext.current
    
    // State to hold the list of books
    var books by remember { mutableStateOf<List<LiteratureBook>>(emptyList()) }
    // State to track if a specific book is open (Detail View)
    var selectedBook by remember { mutableStateOf<LiteratureBook?>(null) }
    // Loading/Error states
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // --- Load JSON from Assets on Startup ---
    LaunchedEffect(Unit) {
        try {
            books = loadBooksFromAssets(context)
            isLoading = false
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: If file missing, show error or empty list
            errorMsg = "Please create 'assets/literature.json'.\nError: ${e.message}"
            isLoading = false
        }
    }

    // --- Navigation Logic (List vs Reader) ---
    if (selectedBook != null) {
        // If a book is selected, show the Reader
        // Handle system "Back" button to close the book instead of exiting app
        BackHandler {
            selectedBook = null
        }
        BookReaderView(
            book = selectedBook!!,
            onBack = { selectedBook = null }
        )
    } else {
        // Otherwise, show the Library List
        BookListView(
            books = books,
            isLoading = isLoading,
            error = errorMsg,
            onBookClick = { selectedBook = it }
        )
    }
}

/**
 * Parses the literature.json file from the assets folder.
 */
fun loadBooksFromAssets(context: Context): List<LiteratureBook> {
    val result = mutableListOf<LiteratureBook>()
    
    // 1. Open the file
    val inputStream = context.assets.open("literature.json")
    val reader = BufferedReader(InputStreamReader(inputStream))
    val jsonString = reader.readText()
    reader.close()

    // 2. Parse the JSON Object
    // Structure is { "id1": {Obj}, "id2": {Obj} }
    val rootObject = JSONObject(jsonString)
    val keys = rootObject.keys()

    while (keys.hasNext()) {
        val key = keys.next() // This is the ID (e.g., "19fc...")
        val bookObj = rootObject.getJSONObject(key)
        
        val title = bookObj.optString("title", "Untitled")
        val content = bookObj.optString("content", "")
        val url = bookObj.optString("notion_url", "")
        // Some items might not have a comment, use a default string if so
        val desc = bookObj.optString("comment_from_index", "No description available.")

        result.add(LiteratureBook(key, title, desc, content, url))
    }

    // 3. Sort alphabetically by title for a clean library
    return result.sortedBy { it.title }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListView(
    books: List<LiteratureBook>,
    isLoading: Boolean,
    error: String?,
    onBookClick: (LiteratureBook) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Stellarium Library",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(books) { book ->
                    OutlinedCard(
                        onClick = { onBookClick(book) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (book.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = book.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3 // Keep cards tidy
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderView(book: LiteratureBook, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = book.title, 
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Display the Content (Markdown / Text)
            Text(
                text = book.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Button to open original Notion Link
            if (book.notionUrl.isNotEmpty()) {
                val context = LocalContext.current
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(book.notionUrl))
                            context.startActivity(intent)
                        } catch(e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("View Original on Notion")
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}