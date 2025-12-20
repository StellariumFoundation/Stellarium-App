package com.jv.stellariumapp

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import org.json.JSONArray
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

// --- Data Model matching your Interface ---
data class StellariumDocument(
    val title: String,               // Required
    val content: String,             // Required (Markdown)
    val notionUrl: String?,          // Optional
    val commentFromIndex: String?    // Optional
)

@Composable
fun BookScreen() {
    val context = LocalContext.current
    
    // State
    var allBooks by remember { mutableStateOf<List<StellariumDocument>>(emptyList()) }
    var groupedBooks by remember { mutableStateOf<Map<String, List<StellariumDocument>>>(emptyMap()) }
    var selectedBook by remember { mutableStateOf<StellariumDocument?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // --- Load JSON and Organize ---
    LaunchedEffect(Unit) {
        try {
            val books = loadBooksFromAssets(context)
            allBooks = books
            groupedBooks = organizeBooksByCategory(books)
            isLoading = false
        } catch (e: Exception) {
            e.printStackTrace()
            errorMsg = "Error loading library. Ensure 'assets/literature.json' exists.\n${e.message}"
            isLoading = false
        }
    }

    // --- Navigation Logic ---
    if (selectedBook != null) {
        BackHandler { selectedBook = null }
        BookReaderView(
            book = selectedBook!!,
            onBack = { selectedBook = null }
        )
    } else {
        BookListView(
            groupedBooks = groupedBooks,
            allBooks = allBooks,
            isLoading = isLoading,
            error = errorMsg,
            onBookClick = { selectedBook = it }
        )
    }
}

// --- Logic to Parse JSON Array ---
fun loadBooksFromAssets(context: Context): List<StellariumDocument> {
    val result = mutableListOf<StellariumDocument>()
    
    // Open the file
    val inputStream = context.assets.open("literature.json")
    val reader = BufferedReader(InputStreamReader(inputStream))
    val jsonString = reader.readText()
    reader.close()

    // Parse JSON Array
    val jsonArray = JSONArray(jsonString)
    for (i in 0 until jsonArray.length()) {
        val bookObj = jsonArray.getJSONObject(i)
        
        // Required Fields
        val title = bookObj.getString("title")
        val content = bookObj.getString("content")
        
        // Optional Fields (check if they exist and are not null)
        val notionUrl = if (bookObj.has("notion_url") && !bookObj.isNull("notion_url")) {
            bookObj.getString("notion_url")
        } else null

        val comment = if (bookObj.has("comment_from_index") && !bookObj.isNull("comment_from_index")) {
            bookObj.getString("comment_from_index")
        } else null
        
        result.add(StellariumDocument(title, content, notionUrl, comment))
    }
    return result
}

// --- Logic to Group Books based on "Stellarium Literature" index ---
fun organizeBooksByCategory(books: List<StellariumDocument>): Map<String, List<StellariumDocument>> {
    // 1. Find the Index Book ("Stellarium Literature")
    val indexBook = books.find { it.title.trim().equals("Stellarium Literature", ignoreCase = true) } 
        ?: return mapOf("All Literature" to books.sortedBy { it.title })

    val categorizedMap = linkedMapOf<String, MutableList<StellariumDocument>>()
    // Map titles to books for easy lookup (normalize keys to lowercase/trimmed)
    val booksByTitle = books.associateBy { it.title.trim().lowercase() }
    val assignedTitles = mutableSetOf<String>()
    
    // Hide the index book itself from the list
    assignedTitles.add(indexBook.title.trim().lowercase())

    val lines = indexBook.content.lines()
    var currentCategory = "General"
    
    // Regex to find: [Title](...) 
    // Matches standard markdown links
    val linkPattern = Pattern.compile("\\[(.*?)\\]\\(.*?\\)")

    for (line in lines) {
        val trimmed = line.trim()
        
        // Detect Headers: **Category Name**
        if (trimmed.startsWith("**") && trimmed.endsWith("**") && trimmed.length > 4) {
            currentCategory = trimmed.removeSurrounding("**").trim()
            continue
        }

        // Detect Links: [Book Title]
        val matcher = linkPattern.matcher(trimmed)
        if (matcher.find()) {
            val extractedTitle = matcher.group(1)?.trim() ?: ""
            
            // Find book by matching title
            val book = booksByTitle[extractedTitle.lowercase()] 
            
            if (book != null) {
                categorizedMap.getOrPut(currentCategory) { mutableListOf() }.add(book)
                assignedTitles.add(book.title.trim().lowercase())
            }
        }
    }

    // Add unassigned books to "Other Resources"
    val unassigned = books.filter { !assignedTitles.contains(it.title.trim().lowercase()) }
    if (unassigned.isNotEmpty()) {
        categorizedMap.getOrPut("Other Resources") { mutableListOf() }.addAll(unassigned)
    }

    return categorizedMap
}

// --- UI Components ---

@Composable
fun BookListView(
    groupedBooks: Map<String, List<StellariumDocument>>,
    allBooks: List<StellariumDocument>,
    isLoading: Boolean,
    error: String?,
    onBookClick: (StellariumDocument) -> Unit
) {
    val context = LocalContext.current

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
                Text(text = error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- PDF Section ---
                item {
                    Text(
                        text = "Official Books (PDF)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // The Stellarium Book PDF (With Amazon/Everand Links)
                    PDFBookCard(
                        title = "The Stellarium Book",
                        fileName = "The.Stellarium.Book.pdf",
                        context = context,
                        showExternalLinks = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    // The Stellarium Society PDF (No External Links)
                    PDFBookCard(
                        title = "The Stellarium Society",
                        fileName = "Stellarium.Society.pdf",
                        context = context,
                        showExternalLinks = false
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                // --- Grouped Books Section ---
                if (groupedBooks.isNotEmpty()) {
                    groupedBooks.forEach { (category, books) ->
                        if (books.isNotEmpty()) {
                            item {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp, bottom = 8.dp)
                                )
                            }
                            items(books) { book ->
                                BookCard(book, onBookClick)
                            }
                        }
                    }
                } else {
                    // Fallback
                    items(allBooks) { book -> BookCard(book, onBookClick) }
                }
            }
        }
    }
}

@Composable
fun PDFBookCard(title: String, fileName: String, context: Context, showExternalLinks: Boolean) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilledTonalButton(onClick = { openPdfFromAssets(context, fileName) }) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open")
                }
                OutlinedButton(onClick = { savePdfToDownloads(context, fileName) }) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }

            if (showExternalLinks) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.amazon.com/dp/B0FLPSQ6ZS"))
                        context.startActivity(intent)
                    }) {
                        Text("Buy on Amazon")
                    }
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.everand.com/book/897831454/The-Stellarium-Book"))
                        context.startActivity(intent)
                    }) {
                        Text("Read on Everand")
                    }
                }
            }
        }
    }
}

@Composable
fun BookCard(book: StellariumDocument, onClick: (StellariumDocument) -> Unit) {
    OutlinedCard(
        onClick = { onClick(book) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(), 
            horizontalAlignment = Alignment.CenterHorizontally // Center Aligned
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            // Show comment/description if available
            if (!book.commentFromIndex.isNullOrBlank() && book.commentFromIndex != "null") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = book.commentFromIndex,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderView(book: StellariumDocument, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = book.title, 
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally // Center content
        ) {
            MarkdownText(content = book.content)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Link to Notion if available
            if (!book.notionUrl.isNullOrBlank()) {
                val context = LocalContext.current
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(book.notionUrl))
                            context.startActivity(intent)
                        } catch(e: Exception) { e.printStackTrace() }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Read Original on Notion")
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// --- Custom Markdown Parser & Renderer ---

@Composable
fun MarkdownText(content: String) {
    val context = LocalContext.current
    val styledText = remember(content) { parseMarkdown(content) }

    ClickableText(
        text = styledText,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center, // CENTRALIZED MARKDOWN TEXT
            lineHeight = 24.sp
        ),
        onClick = { offset ->
            styledText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        }
    )
}

fun parseMarkdown(markdown: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.lines()
        
        for (line in lines) {
            val trimmedLine = line.trim()

            // Header (#)
            if (trimmedLine.startsWith("#")) {
                val level = trimmedLine.takeWhile { it == '#' }.length
                val text = trimmedLine.substring(level).trim()
                
                if (length > 0) append("\n\n")
                
                withStyle(
                    style = SpanStyle(
                        fontSize = if (level == 1) 26.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White 
                    )
                ) {
                    append(text)
                }
                continue
            }

            // Bold Lines (**Text**) - Treated like Subheaders
            if (trimmedLine.startsWith("**") && trimmedLine.endsWith("**")) {
                if (length > 0) append("\n\n")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                    append(trimmedLine.removeSurrounding("**"))
                }
                continue
            }

            if (length > 0) append("\n") 
            
            // Regex for Links and Bold inside text
            val regex = Regex("(\\[(.*?)\\]\\((.*?)\\))|(\\*\\*(.*?)\\*\\*)")
            
            var lastIndex = 0
            val matches = regex.findAll(trimmedLine)

            for (match in matches) {
                if (match.range.first > lastIndex) {
                    append(trimmedLine.substring(lastIndex, match.range.first))
                }

                if (match.groups[1] != null) { 
                    // Link
                    val linkText = match.groups[2]?.value ?: ""
                    val linkUrl = match.groups[3]?.value ?: ""
                    
                    pushStringAnnotation(tag = "URL", annotation = linkUrl)
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFF64B5F6), 
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(linkText)
                    }
                    pop()
                } else if (match.groups[4] != null) { 
                    // Bold
                    val boldText = match.groups[5]?.value ?: ""
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(boldText)
                    }
                }
                lastIndex = match.range.last + 1
            }
            
            if (lastIndex < trimmedLine.length) {
                append(trimmedLine.substring(lastIndex))
            }
        }
    }
}

// --- PDF Helpers using MediaStore (Fixes download issue) ---

fun openPdfFromAssets(context: Context, fileName: String) {
    try {
        val file = File(context.cacheDir, fileName)
        if (!file.exists()) {
            context.assets.open(fileName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
        
        val chooser = Intent.createChooser(intent, "Open PDF")
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Error opening PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun savePdfToDownloads(context: Context, fileName: String) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Modern Android (API 29+): Use MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            
            if (uri != null) {
                resolver.openOutputStream(uri).use { output ->
                    context.assets.open(fileName).use { input ->
                        input.copyTo(output!!)
                    }
                }
                Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to create file", Toast.LENGTH_LONG).show()
            }
        } else {
            // Older Android: Direct File Access
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            context.assets.open(fileName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(context, "Saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}