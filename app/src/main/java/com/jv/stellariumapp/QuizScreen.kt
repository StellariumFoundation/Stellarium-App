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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

    // Load JSON Data
    LaunchedEffect(Unit) {
        try {
            categories = loadQuizzesFromAssets(context)
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
            onQuizComplete = { activeQuizSession = null } // Go back to menu
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
        Text(
            text = "Knowledge Base",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select a category to begin",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
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
            // 3x3 Grid (Fixed count 3 columns)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
    OutlinedCard(
        onClick = { onClick(category) },
        modifier = Modifier
            .aspectRatio(1f) // Makes it square
            .fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun QuizSessionView(session: QuizSession, onQuizComplete: () -> Unit) {
    // Session State
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }

    val question = session.questions.getOrNull(currentQuestionIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()), // Scrollable for long text
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isFinished || question == null) {
            // --- Result Screen ---
            Spacer(modifier = Modifier.height(60.dp))
            Text(text = "Quiz Completed!", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${session.categoryName}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            // Score Display
            Text(
                text = "$score / ${session.questions.size}",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onQuizComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Return to Menu")
            }

        } else {
            // --- Question Screen ---
            
            // Progress Bar
            LinearProgressIndicator(
                progress = { (currentQuestionIndex + 1).toFloat() / session.questions.size },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Question ${currentQuestionIndex + 1} of ${session.questions.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Question Text
            Text(
                text = question.text,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                minLines = 3
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Options Buttons
            question.options.forEachIndexed { index, option ->
                OutlinedButton(
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
                        .padding(vertical = 6.dp)
                ) {
                    Text(text = option, textAlign = TextAlign.Center)
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