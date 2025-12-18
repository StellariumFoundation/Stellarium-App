package com.jv.stellariumapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun QuizScreen() {
    var score by remember { mutableIntStateOf(0) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var showResult by remember { mutableStateOf(false) }

    val questions = listOf(
        Question("What is the most important metric according to the Principles?", listOf("Fame", "Wealth", "Power"), 1),
        Question("What is the motto of the Foundation?", listOf("Live Laugh Love", "Do Good, Make Money, Have Fun", "Work Hard Play Hard"), 1),
        Question("War is considered...", listOf("Necessary", "Anti-Wealth", "Profitable"), 1)
    )

    Column(
        modifier = Modifier.padding(16.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showResult) {
            Text(text = "Quiz Completed!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Your Score: $score / ${questions.size}", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Button(
                onClick = { 
                    score = 0
                    currentQuestionIndex = 0
                    showResult = false
                }, 
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Restart Quiz")
            }
        } else {
            val question = questions[currentQuestionIndex]
            
            Text(text = "Question ${currentQuestionIndex + 1}/${questions.size}", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = question.text, 
                style = MaterialTheme.typography.titleLarge, 
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            question.options.forEachIndexed { index, option ->
                Button(
                    onClick = {
                        if (index == question.correctIndex) score++
                        if (currentQuestionIndex < questions.size - 1) {
                            currentQuestionIndex++
                        } else {
                            showResult = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(option)
                }
            }
        }
    }
}

// Helper data class for Quiz
data class Question(val text: String, val options: List<String>, val correctIndex: Int)