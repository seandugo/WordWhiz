package com.example.thesis_app.models

data class ClassRequest(
    val studentId: String = "",
    val studentName: String = "",
    val requestedClassCode: String = "",
    val status: String = "pending"
)
