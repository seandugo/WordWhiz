package com.example.thesis_app.models

sealed class ProgressItem {
    data class Part(
        val levelName: String,
        val isCompleted: Boolean = false, // ✅ added here
        val completedParts: Int = 0,
        val totalParts: Int = 1,
        val quizId : String,
    ) : ProgressItem()
}
