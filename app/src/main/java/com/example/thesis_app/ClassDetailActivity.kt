package com.example.thesis_app

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.StudentItem
import com.google.firebase.database.*

class ClassDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var backIcon: ImageView
    private lateinit var classNameText: TextView
    private lateinit var addStudentCard: CardView
    private lateinit var adapter: StudentAdapter
    private val studentList = mutableListOf<StudentItem>()
    private var className: String = "Unknown Class"
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_detail)

        // System colors
        window.navigationBarColor = ContextCompat.getColor(this, R.color.my_nav_color)
        window.statusBarColor = ContextCompat.getColor(this, R.color.my_nav_color)

        // Firebase reference
        database = FirebaseDatabase.getInstance().reference

        // Get class name from Intent
        className = intent.getStringExtra("CLASS_NAME") ?: "Unknown Class"

        // Find views
        recyclerView = findViewById(R.id.studentRecyclerView)
        backIcon = findViewById(R.id.backIcon)
        classNameText = findViewById(R.id.classNameText)
        addStudentCard = findViewById(R.id.addStudentCard)

        classNameText.text = className
        backIcon.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StudentAdapter(studentList) {}
        recyclerView.adapter = adapter

        loadStudentsFromFirebase()

        addStudentCard.setOnClickListener { showAddStudentDialog() }
    }

    private fun loadStudentsFromFirebase() {
        val classKey = className.replace(" ", "_")
        database.child("classes").child(classKey).child("students")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    studentList.clear()
                    for (child in snapshot.children) {
                        val student = child.getValue(StudentItem::class.java)
                        student?.let { studentList.add(it) }
                    }
                    adapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ClassDetailActivity, "Failed to load students", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showAddStudentDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_student, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editStudentName)
        val idInput = dialogView.findViewById<EditText>(R.id.editStudentId)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Student")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val id = idInput.text.toString().trim()
            if (name.isEmpty() || id.isEmpty()) {
                Toast.makeText(this, "Please enter both name and ID", Toast.LENGTH_SHORT).show()
            } else {
                val newStudent = StudentItem(name, id)
                adapter.addItemAtTop(newStudent)
                recyclerView.scrollToPosition(0)

                // Save to Firebase
                val classKey = className.replace(" ", "_")
                database.child("classes").child(classKey).child("students").push().setValue(newStudent)
                    .addOnSuccessListener { Toast.makeText(this, "Added $name", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { e -> Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                dialog.dismiss()
            }
        }
    }
}