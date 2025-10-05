package com.example.thesis_app.models

sealed class ProgressItem {
    data class Divider(val title: String) : ProgressItem()
    data class Part(
        val levelName: String,
        val isCompleted: Boolean = false // ✅ added here
    ) : ProgressItem()
}
