package com.example.thesis_app.models
data class QuizModel(
    val id : String,
    val title : String,
    val subtitle : String,
    val parts: List<PartModel> = emptyList(),
    val progress: Int,
    val totalQuestions: Int = 0
){
    constructor() : this("","","", emptyList(),0)
}

data class PartModel(
    val id: String = "",
    val questions: List<QuestionModel> = emptyList()
)

data class QuestionModel(
    val question : String,
    val options : List<String>,
    val correct : String,
    val explanation: String = ""
){
    constructor() : this ("", emptyList(),"")
}

data class QuizPartItem(
    val quizId: String,
    val quizTitle: String,
    val quizSubtitle: String,
    val partId: String,
    val questions: List<QuestionModel>,
    val isUnlocked: Boolean = false,
    val displayName: String = "",
    val order: Int
)

sealed class QuizDisplayItem {
    data class Part(val item: QuizPartItem) : QuizDisplayItem()
    data class Divider(val title: String) : QuizDisplayItem()
}
