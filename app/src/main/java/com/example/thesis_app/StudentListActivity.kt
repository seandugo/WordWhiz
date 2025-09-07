package com.example.thesis_app

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.StudentItem

class StudentListActivity : AppCompatActivity() {

    private lateinit var adapter: StudentAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var backIcon: ImageView
    private lateinit var classNameText: TextView
    private val studentList = mutableListOf<StudentItem>()
    private var className: String = "Unknown Class" // default, can be set via intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student)

        // ✅ Set navigation bar and status bar colors
        window.navigationBarColor = getColor(R.color.my_nav_color)
        window.statusBarColor = getColor(R.color.my_nav_color)

        // Get class name from Intent
        className = intent.getStringExtra("CLASS_NAME") ?: "Unknown Class"

        // Find views
        recyclerView = findViewById(R.id.studentRecyclerView)
        backIcon = findViewById(R.id.backIcon)
        classNameText = findViewById(R.id.classNameText)

        // Set class name dynamically
        classNameText.text = className

        // Back button functionality
        backIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // RecyclerView setup
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StudentAdapter(studentList) { student ->
            Toast.makeText(this, "Clicked: ${student.studentName}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter

        // Example: Add one student dynamically
        val newStudent = StudentItem("John Doe", "2025-01")
        adapter.addItemAtTop(newStudent)
    }
}