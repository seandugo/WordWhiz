package com.example.thesis_app.models

data class ArchivedClass(
    val className: String = "",
    val roomNumber: String = "",
    val archivedAt: Long = 0L,
    val deleteAfter: Long = 604800000L // default: 7 days
)
