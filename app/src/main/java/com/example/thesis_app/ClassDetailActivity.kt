package com.example.thesis_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.StudentItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ClassDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var classNameText: TextView
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
        classNameText = findViewById(R.id.classNameText)

        // Set class name dynamically
        classNameText.text = className

        // ✅ Setup Toolbar with back navigation
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // RecyclerView setup
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StudentAdapter(studentList) { student ->
            Toast.makeText(this, "Clicked: ${student.name}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter

        // Load existing students
        loadStudentsFromFirebase()

        // Add student button (CardView)
        findViewById<androidx.cardview.widget.CardView>(R.id.addStudentCard).setOnClickListener {
            showAddStudentDialog()
        }
    }

    private fun loadStudentsFromFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val classCode = intent.getStringExtra("CLASS_CODE") ?: return

        val studentsRef = database.child("users")
            .child(uid)
            .child("classes")
            .child(classCode)
            .child("students")

        studentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                studentList.clear()
                for (studentSnap in snapshot.children) {
                    val student = studentSnap.getValue(StudentItem::class.java)
                    if (student != null) {
                        val studentWithId = student.copy()
                        studentWithId.studentId = studentSnap.key ?: ""
                        studentList.add(studentWithId)
                    }
                }
                studentList.sortBy { it.order }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
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
                // ✅ set email = null
                val newStudent = StudentItem(name = name, email = null, order = studentList.size)

                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
                val classCode = intent.getStringExtra("CLASS_CODE") ?: return@setOnClickListener

                database.child("users")
                    .child(uid)
                    .child("classes")
                    .child(classCode)
                    .child("students")
                    .child(id) // studentId as key
                    .setValue(newStudent)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Added $name", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // ✅ Handle Toolbar back button
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
