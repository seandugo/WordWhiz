package com.example.thesis_app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.database.*

data class AdminLecture(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val order: Int = 0
)

class AdminManageLecturesActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminLectureAdapter
    private val lectures = mutableListOf<AdminLecture>()
    private var lecturesListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_manage_lectures)

        database = FirebaseDatabase.getInstance().reference

        val toolbar = findViewById<MaterialToolbar>(R.id.lecturesToolbar)
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor))
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.black))
        toolbar.title = "Manage Lectures"
        toolbar.setNavigationIcon(R.drawable.back)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerView = findViewById(R.id.lecturesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdminLectureAdapter(lectures) { selectedLecture ->
            val intent = Intent(this, AdminEditLectureActivity::class.java)
            intent.putExtra("lectureId", selectedLecture.id)
            intent.putExtra("title", selectedLecture.title)
            intent.putExtra("description", selectedLecture.description)
            intent.putExtra("order", selectedLecture.order)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // ✅ Add new lecture button
        findViewById<androidx.cardview.widget.CardView>(R.id.addLectureCard).setOnClickListener {
            showAddLectureDialog()
        }

        // ✅ Start listening for live updates
        listenToLectures()
    }

    private fun showAddLectureDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_lecture, null)
        val titleInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputLectureTitle)
        val subtitleInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputLectureSubtitle)
        val orderInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputLectureOrder)
        val addBtn = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.addLectureConfirmBtn)
        val cancelBtn = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.addLectureCancelBtn)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // 🧮 Auto-fill next order
        database.child("quizzes").get().addOnSuccessListener { snapshot ->
            var highestOrder = 0
            for (quizSnap in snapshot.children) {
                val order = quizSnap.child("order").getValue(Int::class.java) ?: 0
                if (order > highestOrder) highestOrder = order
            }
            orderInput.setText((highestOrder + 1).toString())
        }

        addBtn.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val subtitle = subtitleInput.text.toString().trim()
            val order = orderInput.text.toString().trim().toIntOrNull() ?: 0

            if (title.isEmpty()) {
                titleInput.error = "Title required"
                return@setOnClickListener
            }

            val newLectureRef = database.child("quizzes").push()
            val lectureId = newLectureRef.key ?: return@setOnClickListener
            val lectureData = mapOf(
                "title" to title,
                "subtitle" to subtitle,
                "order" to order,
                "quizid" to lectureId
            )

            newLectureRef.setValue(lectureData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Lecture added successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add lecture: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        cancelBtn.setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    /**
     * 🔄 Live listener for lectures — auto updates when data changes
     */
    private fun listenToLectures() {
        val TAG = "🔥 AdminLecturesLive"
        val ref = database.child("quizzes")

        lecturesListener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lectures.clear()

                if (!snapshot.exists()) {
                    Log.w(TAG, "⚠️ No quizzes found.")
                    adapter.notifyDataSetChanged()
                    return
                }

                for (quizSnap in snapshot.children) {
                    val id = quizSnap.key ?: continue
                    val title = quizSnap.child("title").getValue(String::class.java) ?: id
                    val desc = quizSnap.child("subtitle").getValue(String::class.java) ?: "No description"
                    val order = quizSnap.child("order").getValue(Int::class.java) ?: 0

                    lectures.add(AdminLecture(id, title, desc, order))
                }

                lectures.sortBy { it.order }
                Log.d(TAG, "✅ Live updated ${lectures.size} lectures.")
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Listener cancelled: ${error.message}")
            }
        })
    }

    /**
     * 🧹 Remove listener to avoid leaks or double updates
     */
    override fun onDestroy() {
        super.onDestroy()
        try {
            lecturesListener?.let {
                database.child("quizzes").removeEventListener(it)
                Log.d("🔥 AdminLecturesLive", "🧹 Listener removed on destroy.")
            }
        } catch (e: Exception) {
            Log.w("🔥 AdminLecturesLive", "⚠️ Failed to remove listener: ${e.message}")
        }
    }
}
