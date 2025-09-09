package com.example.thesis_app.models
import com.google.firebase.database.Exclude

data class ClassItem(
    val className: String = "",
    val roomNo: String = "",
    var order: Int = 0,
    @get:Exclude var classCode: String = "" // not saved in Firebase
)
