package com.jv.stellariumapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import org.json.JSONObject
import java.io.BufferedReader
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
            errorMsg = "Error loading library: ${e.message}"
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
            allBooks = allBooks, // Fallback if grouping fails
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
            val desc = bookObj.optString("comment_from_index", "").ifBlank { "Tap to read." }
            
            result.add(LiteratureBook(key, title, desc, content, url))
        }
    } catch (e: Exception) {
        throw e
    }
    return result
}

fun organizeBooksByCategory(books: List<LiteratureBook>): Map<String, List<LiteratureBook>> {
    // 1. Find the "Index" book (Stellarium Literature) to parse the structure
    val indexBook = books.find { it.title.contains("Stellarium Literature", ignoreCase = true) } 
        ?: return mapOf("All Literature" to books)

    val categorizedMap = mutableMapOf<String, MutableList<LiteratureBook>>()
    val booksById = books.associateBy { it.id }
    val assignedIds = mutableSetOf<String>()

    // 2. Parse the Index content line by line to find Headers and Links
    val lines = indexBook.content.lines()
    var currentCategory = "General"

    // Regex to find links like [Title](...ID.md)
    // We look for the ID at the end of the URL path before .md
    val linkPattern = Pattern.compile(".*\\[(.*?)\\]\\((.*?)(\\w{32})\\.md\\).*")

    for (line in lines) {
        val trimmed = line.trim()
        
        // Detect Category Headers (Bold text like **The Principles**)
        if (trimmed.startsWith("**") && trimmed.endsWith("**") && trimmed.length > 4) {
            currentCategory = trimmed.removeSurrounding("**")
            continue
        }

        // Detect Links to Books
        val matcher = linkPattern.matcher(trimmed)
        if (matcher.find()) {
            val extractedId = matcher.group(3) // The 32-char ID
            val book = booksById[extractedId]
            if (book != null) {
                categorizedMap.getOrPut(currentCategory) { mutableListOf() }.add(book)
                assignedIds.add(extractedId)
            }
        }
    }

    // 3. Add any books that weren't linked in the index to "Other Resources"
    val unassigned = books.filter { !assignedIds.contains(it.id) && it.id != indexBook.id }
    if (unassigned.isNotEmpty()) {
        categorizedMap["Other Resources"] = unassigned.toMutableList()
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
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Render Categories
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
                                    .padding(vertical = 8.dp)
                            )
                        }
                        items(books) { book ->
                            BookCard(book, onBookClick)
                        }
                    }
                }
                
                // Fallback if grouping found nothing (e.g. empty index)
                if (groupedBooks.isEmpty() && allBooks.isNotEmpty()) {
                    items(allBooks) { book -> BookCard(book, onBookClick) }
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
                .padding(16.dp)
                .fillMaxWidth(), // Ensure contents fill width for centering
            horizontalAlignment = Alignment.CenterHorizontally // Center horizontally
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            if (book.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
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
                .padding(24.dp)
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
                    Text("Read on Notion")
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
            textAlign = TextAlign.Start // Reader text usually looks better aligned start
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
            // 1. Headers (#)
            if (line.startsWith("#")) {
                val level = line.takeWhile { it == '#' }.length
                val text = line.substring(level).trim()
                
                // Add extra newline before headers
                if (length > 0) append("\n\n")
                
                withStyle(
                    style = SpanStyle(
                        fontSize = if (level == 1) 24.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White // Force white for headers in dark theme
                    )
                ) {
                    append(text)
                }
                continue
            }

            // 2. Standard Line
            if (length > 0) append("\n") // Newline between paragraphs
            
            var currentIndex = 0
            // Regex for Bold (**text**) and Links ([text](url))
            // We use a simple loop to find matches in order
            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
            val linkRegex = Regex("\\[(.*?)\\]\\((.*?)\\)")
            
            // We process the line character by character (simplified logic for mixing bold/links)
            // For a robust implementation, usually a full parser is needed. 
            // Here we prioritize Links then Bold for an MVP.
            
            var remainingLine = line
            
            while (remainingLine.isNotEmpty()) {
                val linkMatch = linkRegex.find(remainingLine)
                val boldMatch = boldRegex.find(remainingLine)
                
                // Find which comes first
                val firstMatch = listOfNotNull(linkMatch, boldMatch).minByOrNull { it.range.first }
                
                if (firstMatch == null) {
                    append(remainingLine)
                    break
                }
                
                // Append text before the match
                append(remainingLine.substring(0, firstMatch.range.first))
                
                if (firstMatch == linkMatch) {
                    val linkText = linkMatch!!.groupValues[1]
                    val linkUrl = linkMatch.groupValues[2]
                    
                    pushStringAnnotation(tag = "URL", annotation = linkUrl)
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFF64B5F6), // Light Blue for links
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(linkText)
                    }
                    pop()
                } else {
                    val boldText = boldMatch!!.groupValues[1]
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(boldText)
                    }
                }
                
                remainingLine = remainingLine.substring(firstMatch.range.last + 1)
            }
        }
    }
}