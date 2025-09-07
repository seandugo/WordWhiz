package com.example.thesis_app.models
data class StudentItem(
    var name: String? = null,
    var email: String? = null,  // always null in DB
    var order: Int = 0,
    var studentId: String? = null // Firebase key
)


