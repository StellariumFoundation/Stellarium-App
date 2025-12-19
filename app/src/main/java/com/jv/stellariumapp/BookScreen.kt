package com.jv.stellariumapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
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
import androidx.compose.material.icons.filled.Book
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
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

// --- Data Model ---
data class LiteratureBook(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val notionUrl: String
)

@Composable
fun BookScreen() {
    val context = LocalContext.current
    
    // State
    var allBooks by remember { mutableStateOf<List<LiteratureBook>>(emptyList()) }
    var groupedBooks by remember { mutableStateOf<Map<String, List<LiteratureBook>>>(emptyMap()) }
    var selectedBook by remember { mutableStateOf<LiteratureBook?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // --- Load and Organize Data ---
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

// --- Logic to Parse JSON and Organize Categories ---

fun loadBooksFromAssets(context: Context): List<LiteratureBook> {
    val result = mutableListOf<LiteratureBook>()
    try {
        val inputStream = context.assets.open("literature.json")
        val reader = BufferedReader(InputStreamReader(inputStream))
        val jsonString = reader.readText()
        reader.close()

        val rootObject = JSONObject(jsonString)
        val keys = rootObject.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val bookObj = rootObject.getJSONObject(key)
            val title = bookObj.optString("title", "Untitled")
            val content = bookObj.optString("content", "")
            val url = bookObj.optString("notion_url", "")
            val desc = bookObj.optString("comment_from_index", "Tap to read full content.")
            
            result.add(LiteratureBook(key, title, desc, content, url))
        }
    } catch (e: Exception) {
        throw e
    }
    return result
}

fun organizeBooksByCategory(books: List<LiteratureBook>): Map<String, List<LiteratureBook>> {
    val indexBook = books.find { it.title.trim().equals("Stellarium Literature", ignoreCase = true) } 
        ?: return mapOf("All Literature" to books.sortedBy { it.title })

    val categorizedMap = linkedMapOf<String, MutableList<LiteratureBook>>()
    val booksById = books.associateBy { it.id }
    val assignedIds = mutableSetOf<String>()
    assignedIds.add(indexBook.id)

    val lines = indexBook.content.lines()
    var currentCategory = "General"
    val idPattern = Pattern.compile("([a-f0-9]{32})\\.md")

    for (line in lines) {
        val trimmed = line.trim()
        
        if (trimmed.startsWith("**") && trimmed.endsWith("**") && trimmed.length > 4) {
            currentCategory = trimmed.removeSurrounding("**").trim()
            continue
        }

        val matcher = idPattern.matcher(trimmed)
        if (matcher.find()) {
            val extractedId = matcher.group(1)
            val book = booksById[extractedId]
            if (book != null) {
                categorizedMap.getOrPut(currentCategory) { mutableListOf() }.add(book)
                assignedIds.add(extractedId)
            }
        }
    }

    val unassigned = books.filter { !assignedIds.contains(it.id) }
    if (unassigned.isNotEmpty()) {
        categorizedMap.getOrPut("Other Resources") { mutableListOf() }.addAll(unassigned)
    }

    return categorizedMap
}

// --- UI Components ---

@Composable
fun BookListView(
    groupedBooks: Map<String, List<LiteratureBook>>,
    allBooks: List<LiteratureBook>,
    isLoading: Boolean,
    error: String?,
    onBookClick: (LiteratureBook) -> Unit
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
                // --- Core PDFs Section ---
                item {
                    Text(
                        text = "Official Books (PDF)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // The Stellarium Book PDF Card
                    PDFBookCard(
                        title = "The Stellarium Book",
                        fileName = "The.Stellarium.Book.pdf",
                        context = context
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    // The Stellarium Society PDF Card
                    PDFBookCard(
                        title = "Stellarium Society",
                        fileName = "Stellarium.Society.pdf",
                        context = context
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // External Links for Purchase/Subscription
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.amazon.com/dp/B0FLPSQ6ZS"))
                            context.startActivity(intent)
                        }) {
                            Text("Buy on Amazon")
                        }
                        Button(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.everand.com/book/897831454/The-Stellarium-Book"))
                            context.startActivity(intent)
                        }) {
                            Text("Read on Everand")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                // --- Render Categories ---
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
                    items(allBooks) { book -> BookCard(book, onBookClick) }
                }
            }
        }
    }
}

@Composable
fun PDFBookCard(title: String, fileName: String, context: Context) {
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
                // Open Button
                FilledTonalButton(onClick = { openPdfFromAssets(context, fileName) }) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open")
                }
                
                // Download Button
                OutlinedButton(onClick = { savePdfToDownloads(context, fileName) }) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }
        }
    }
}

@Composable
fun BookCard(book: LiteratureBook, onClick: (LiteratureBook) -> Unit) {
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
            horizontalAlignment = Alignment.CenterHorizontally // Enforce Center Alignment
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            if (book.description.isNotBlank() && book.description != "null") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = book.description,
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
fun BookReaderView(book: LiteratureBook, onBack: () -> Unit) {
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
            // Render the Content using the Markdown Parser
            MarkdownText(content = book.content)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (book.notionUrl.isNotEmpty()) {
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
            // Handle Link Clicks
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

            // 1. Headers (#)
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

            // 2. Bold Lines (Specific for Stellarium **Title**)
            if (trimmedLine.startsWith("**") && trimmedLine.endsWith("**")) {
                if (length > 0) append("\n\n")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                    append(trimmedLine.removeSurrounding("**"))
                }
                continue
            }

            // 3. Standard Body Text (mixed bold and links)
            if (length > 0) append("\n") // Newline between paragraphs
            
            val regex = Regex("(\\[(.*?)\\]\\((.*?)\\))|(\\*\\*(.*?)\\*\\*)")
            
            var lastIndex = 0
            val matches = regex.findAll(trimmedLine)

            for (match in matches) {
                if (match.range.first > lastIndex) {
                    append(trimmedLine.substring(lastIndex, match.range.first))
                }

                if (match.groups[1] != null) { 
                    // LINK: [Text](Url)
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
                    // BOLD: **Text**
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

// --- PDF Helpers ---

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
        
        // Use FileProvider to share file securely
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
        
        // Try to start activity
        val chooser = Intent.createChooser(intent, "Open PDF")
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Error opening PDF: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}

fun savePdfToDownloads(context: Context, fileName: String) {
    try {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        
        context.assets.open(fileName).use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        Toast.makeText(context, "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}