package com.example.thesis_app.models

import com.google.firebase.database.Exclude

data class ClassItem(
    var className: String = "",
    var roomNo: String = "",
    var order: Int = 0,
    @get:Exclude var classCode: String = "",
    val archivedAt: Long? = null
)