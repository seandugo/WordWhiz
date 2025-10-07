package com.example.thesis_app.models

import com.google.firebase.database.DataSnapshot

data class QuizMeta(
    val snapshot: DataSnapshot,
    val order: Int,
    val quizId: String,
    val title: String,
    val subtitle: String
)