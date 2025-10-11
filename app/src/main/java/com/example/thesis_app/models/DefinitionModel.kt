package com.example.thesis_app.models

data class DefinitionModel(
    val number: Int,
    val title: String,
    val emoji: String,
    val definition: String,
    val example: String,
    val explanation: String?
)