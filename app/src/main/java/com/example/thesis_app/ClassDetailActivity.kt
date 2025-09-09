package com.example.thesis_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import com.example.thesis_app.models.Achievement
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
        classNameText = findViewById(R.id.headerClassName)

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

        val studentsRef = database.child("classes")
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
        val idInput = dialogView.findViewById<EditText>(R.id.editStudentId)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Student")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val id = idInput.text.toString().trim() // student UID typed in

            if (id.isEmpty()) {
                Toast.makeText(this, "Please enter a student ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val classCode = intent.getStringExtra("CLASS_CODE") ?: return@setOnClickListener

            // 🔹 Look up the student in global users
            val studentRef = database.child("users").child(id)
            studentRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(this@ClassDetailActivity, "No student found with that ID", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val role = snapshot.child("role").getValue(String::class.java)
                    if (role != "student") {
                        Toast.makeText(this@ClassDetailActivity, "This user is not a student", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val name = snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val email = snapshot.child("email").getValue(String::class.java)

                    val achievementsList = snapshot.child("achievements").children.mapNotNull { achSnap ->
                        achSnap.getValue(Achievement::class.java)
                    }

                    val newStudent = StudentItem(
                        name = name,
                        email = email,
                        order = studentList.size,
                        studentId = id,
                        achievements = achievementsList
                    )


                    // 🔹 Save student into the class
                    database.child("classes")
                        .child(classCode)
                        .child("students")
                        .child(id)
                        .setValue(newStudent)
                        .addOnSuccessListener {
                            // 🔹 Also add a pointer in student account -> classes
                            database.child("users")
                                .child(id)
                                .child("classes")
                                .child(classCode)
                                .setValue(true)

                            Toast.makeText(this@ClassDetailActivity, "Added $name", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@ClassDetailActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
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