package com.example.thesis_app.models

data class QuizAnswer(
    val order: Int = 0,
    val question: String = "",
    val selectedAnswer: String = "",
    var correctAnswer: String = "",
    val isCorrect: Boolean = false,
    val explanation: String = "",
    var options: List<String>? = null
)

