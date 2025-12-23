package com.jv.stellariumapp

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

// --- Data Models ---

data class QuizCategory(
    val name: String,
    val questions: List<Question>
)

data class Question(
    val text: String,
    val options: List<String>,
    val correctIndex: Int
)

// --- Main Screen ---

@Composable
fun QuizScreen() {
    val context = LocalContext.current
    
    // Application States
    var categories by remember { mutableStateOf<List<QuizCategory>>(emptyList()) }
    var activeQuizSession by remember { mutableStateOf<QuizSession?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load JSON Data & Create "All" Category
    LaunchedEffect(Unit) {
        try {
            val loadedCategories = loadQuizzesFromAssets(context)
            
            // Create a "General / All" Category by combining everyone
            val allQuestions = loadedCategories.flatMap { it.questions }
            val generalCategory = QuizCategory(
                name = "General Knowledge (All Topics)",
                questions = allQuestions
            )
            
            // Add "General" to the top of the list
            categories = listOf(generalCategory) + loadedCategories
            
            isLoading = false
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Error loading quizzes: ${e.message}"
            isLoading = false
        }
    }

    // --- Navigation / View Logic ---

    if (activeQuizSession != null) {
        // If inside a quiz, handle Back press to return to menu
        BackHandler {
            activeQuizSession = null
        }
        QuizSessionView(
            session = activeQuizSession!!,
            onReturnToMenu = { activeQuizSession = null },
            onRetake = { 
                // Shuffle and restart the SAME category
                val originalCategory = categories.find { it.name == activeQuizSession!!.categoryName }
                if (originalCategory != null) {
                    val shuffledQuestions = originalCategory.questions.shuffled().take(15)
                    activeQuizSession = QuizSession(
                        categoryName = originalCategory.name,
                        questions = shuffledQuestions
                    )
                }
            }
        )
    } else {
        // Show Category Selection Menu
        QuizMenu(
            categories = categories,
            isLoading = isLoading,
            error = errorMessage,
            onCategorySelected = { category ->
                // Start new session: Shuffle questions and take max 15
                val shuffledQuestions = category.questions.shuffled().take(15)
                activeQuizSession = QuizSession(
                    categoryName = category.name,
                    questions = shuffledQuestions
                )
            }
        )
    }
}

// --- Views ---

@Composable
fun QuizMenu(
    categories: List<QuizCategory>,
    isLoading: Boolean,
    error: String?,
    onCategorySelected: (QuizCategory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Explanatory Header ---
        Text(
            text = "Stellarium Knowledge Base",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Master the principles of the Foundation. Select a module below or choose 'General Knowledge' to test yourself on everything.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        } else {
            // Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categories) { category ->
                    CategoryButton(category, onCategorySelected)
                }
            }
        }
    }
}

@Composable
fun CategoryButton(category: QuizCategory, onClick: (QuizCategory) -> Unit) {
    // Highlight the "General Knowledge" button slightly differently if desired
    val isGeneral = category.name.contains("General Knowledge")
    val containerColor = if (isGeneral) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isGeneral) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary

    OutlinedCard(
        onClick = { onClick(category) },
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = containerColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                minLines = 2
            )
        }
    }
}

@Composable
fun QuizSessionView(
    session: QuizSession, 
    onReturnToMenu: () -> Unit,
    onRetake: () -> Unit
) {
    // Session State
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }

    val question = session.questions.getOrNull(currentQuestionIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp) // Reduced padding
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isFinished || question == null) {
            // --- Result Screen ---
            Spacer(modifier = Modifier.height(40.dp))
            Text(text = "Assessment Complete", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = session.categoryName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            // Score Display
            Text(
                text = "$score / ${session.questions.size}",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Correct Answers",
                style = MaterialTheme.typography.labelLarge
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Retake Button
            Button(
                onClick = onRetake,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("Retake Quiz (New Questions)")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Menu Button
            OutlinedButton(
                onClick = onReturnToMenu,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Return to Topics")
            }

        } else {
            // --- Question Screen ---
            
            // Progress Bar
            LinearProgressIndicator(
                progress = { (currentQuestionIndex + 1).toFloat() / session.questions.size },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiary
            )
            
            Spacer(modifier = Modifier.height(12.dp)) // Tight spacing
            
            Text(
                text = "Question ${currentQuestionIndex + 1} of ${session.questions.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(12.dp)) // Tight spacing

            // Question Text
            Text(
                text = question.text,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                minLines = 3,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(24.dp)) // Tight spacing

            // Options Buttons (White Background)
            question.options.forEachIndexed { index, option ->
                Button(
                    onClick = {
                        if (index == question.correctIndex) {
                            score++
                        }
                        
                        // Move to next or finish
                        if (currentQuestionIndex < session.questions.size - 1) {
                            currentQuestionIndex++
                        } else {
                            isFinished = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp), // Tighter vertical padding
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White, // White Background
                        contentColor = Color.Black    // Dark Text
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = option, 
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}

// --- Data Logic ---

data class QuizSession(
    val categoryName: String,
    val questions: List<Question>
)

fun loadQuizzesFromAssets(context: Context): List<QuizCategory> {
    val categoryList = mutableListOf<QuizCategory>()
    
    // Open quizzes.json
    val inputStream = context.assets.open("quizzes.json")
    val reader = BufferedReader(InputStreamReader(inputStream))
    val jsonString = reader.readText()
    reader.close()

    // Parse Root Object
    val rootObject = JSONObject(jsonString)
    
    // "categories" is inside the root object based on your structure
    val categoriesArray = rootObject.getJSONArray("categories")

    for (i in 0 until categoriesArray.length()) {
        val catObj = categoriesArray.getJSONObject(i)
        
        val name = catObj.getString("categoryName")
        val questionsArray = catObj.getJSONArray("questions")
        val questionList = mutableListOf<Question>()

        for (j in 0 until questionsArray.length()) {
            val qObj = questionsArray.getJSONObject(j)
            
            val text = qObj.getString("text")
            val correctIndex = qObj.getInt("correctIndex")
            val optionsJson = qObj.getJSONArray("options")
            val options = mutableListOf<String>()
            
            for (k in 0 until optionsJson.length()) {
                options.add(optionsJson.getString(k))
            }

            questionList.add(Question(text, options, correctIndex))
        }

        categoryList.add(QuizCategory(name, questionList))
    }

    return categoryList
}