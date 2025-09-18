package com.example.thesis_app.models
data class QuizModel(
    val id : String,
    val title : String,
    val subtitle : String,
    val time : String,
    val questionList : List<QuestionModel>,
    val progress: Int,
    val totalQuestions: Int = 0
){
    constructor() : this("","","","", emptyList(),0)
}

data class QuestionModel(
    val question : String,
    val options : List<String>,
    val correct : String,
){
    constructor() : this ("", emptyList(),"")
}