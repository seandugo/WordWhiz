package com.example.thesis_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*

class AdminEditLectureActivity : AppCompatActivity() {

    private lateinit var titleInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var orderInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var deleteButton: MaterialButton

    private val database = FirebaseDatabase.getInstance().reference
    private var lectureId: String? = null
    private var originalOrder: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_edit_lecture)

        // 🧭 Toolbar setup
        val toolbar = findViewById<MaterialToolbar>(R.id.editLectureToolbar)
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor))
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.black))
        toolbar.setNavigationOnClickListener { finish() }

        // 📝 UI elements
        titleInput = findViewById(R.id.editLectureTitle)
        descriptionInput = findViewById(R.id.editLectureDescription)
        orderInput = findViewById(R.id.editLectureOrder)
        saveButton = findViewById(R.id.saveLectureButton)
        deleteButton = findViewById(R.id.deleteLectureButton)

        // 🎯 Get data passed from previous activity
        lectureId = intent.getStringExtra("lectureId")
        val title = intent.getStringExtra("title")
        val description = intent.getStringExtra("description")
        val passedOrder = intent.getIntExtra("order", -1)

        // Prefill title/description
        titleInput.setText(title)
        descriptionInput.setText(description)

        // ✅ If order was passed, use it temporarily
        if (passedOrder != -1) {
            orderInput.setText(passedOrder.toString())
        }

        // ✅ Fetch current order from DB to confirm it’s accurate
        fetchCurrentOrder()

        saveButton.setOnClickListener { saveLectureChanges() }
        deleteButton.setOnClickListener { deleteLecture() }

        // 🧠 Manage Questions button
        findViewById<MaterialButton>(R.id.manageQuestionsButton).setOnClickListener {
            val intent = Intent(this, AdminManageQuestionsActivity::class.java)
            intent.putExtra("lectureId", lectureId)
            intent.putExtra("lectureTitle", titleInput.text.toString())
            startActivity(intent)
        }
    }

    // 🔍 Detect the actual current order from Firebase
    private fun fetchCurrentOrder() {
        val id = lectureId ?: return
        database.child("quizzes").child(id).child("order")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val orderValue = snapshot.getValue(Int::class.java)
                    if (orderValue != null) {
                        originalOrder = orderValue
                        orderInput.setText(orderValue.toString())
                    } else {
                        orderInput.setText("1")
                        originalOrder = 1
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@AdminEditLectureActivity,
                        "Failed to fetch order: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // 💾 Save changes with order validation
    private fun saveLectureChanges() {
        val id = lectureId ?: return
        val newTitle = titleInput.text.toString().trim()
        val newDescription = descriptionInput.text.toString().trim()
        val newOrder = orderInput.text.toString().trim().toIntOrNull() ?: 1

        if (newTitle.isEmpty()) {
            Toast.makeText(this, "Please enter a title.", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Check if order number is already taken
        database.child("quizzes").orderByChild("order").equalTo(newOrder.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isDuplicate = snapshot.children.any { it.key != id }

                    if (isDuplicate) {
                        Toast.makeText(
                            this@AdminEditLectureActivity,
                            "Order $newOrder is already used by another lecture.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        updateLecture(id, newTitle, newDescription, newOrder)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@AdminEditLectureActivity,
                        "Validation failed: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updateLecture(id: String, newTitle: String, newSubtitle: String, newOrder: Int) {
        val updates = mapOf(
            "title" to newTitle,
            "subtitle" to newSubtitle, // ✅ correct field name
            "order" to newOrder,
            "quizid" to id // ✅ ensure quizid is consistent with node key
        )

        database.child("quizzes").child(id).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Lecture updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteLecture() {
        val id = lectureId ?: return

        // 🛑 Show confirmation dialog before deleting
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Lecture")
            .setMessage("Are you sure you want to delete this lecture? This will also remove all its questions and data.")
            .setCancelable(false)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                database.child("quizzes").child(id).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Lecture deleted successfully.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to delete: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .create()
        dialog.show()
    }
}
