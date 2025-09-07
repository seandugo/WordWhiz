package com.example.thesis_app

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ClassDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_detail)
        window.navigationBarColor = getColor(R.color.my_nav_color)
        window.statusBarColor = getColor(R.color.my_nav_color)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        val className = intent.getStringExtra("CLASS_NAME") ?: "Unknown Class"
        val roomNo = intent.getStringExtra("ROOM_NO") ?: "Unknown Room"
        val classCode = intent.getStringExtra("CLASS_CODE") ?: ""

        findViewById<TextView>(R.id.classNameText).text = className

        // Firebase integration
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val database = FirebaseDatabase.getInstance()
        val classRef = database.getReference("users")
            .child(user.uid)
            .child("classes")
            .child(classCode)

        // Example: Fetch students for the class (placeholder)
        classRef.child("students").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // TODO: Process student data (e.g., display in a RecyclerView)
                // Example: val students = snapshot.children.mapNotNull { it.getValue(Student::class.java) }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ClassDetailActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}