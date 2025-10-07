package com.example.thesis_app.models

data class QuizDetailProgress(
    val levelName: String,
    val totalParts: Int,
    val correctParts: Int,
    val wrongParts: Int,
    val retryParts: Int
)
