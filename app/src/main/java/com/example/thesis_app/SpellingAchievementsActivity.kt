package com.example.thesis_app

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class SpellingAchievementsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var studentIdProgress: TextView
    private var userCount = 0 // fetched from Firebase

    private val TAG = "SpellingAchievements"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.spelling_list)

        recycler = findViewById(R.id.achievementsRecycler)
        studentIdProgress = findViewById(R.id.studentIdProgress)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Go back to previous screen
        }

        // Fetch studentId from Intent or SharedPreferences
        val studentId = intent.getStringExtra("studentId")
            ?: getSharedPreferences("USER_PREFS", MODE_PRIVATE).getString("studentId", "Unknown")
        studentIdProgress.text = studentId

        Log.d(TAG, "Student ID: $studentId")

        // Fetch saved words count from Firebase (only where skipped = false)
        val ref = FirebaseDatabase.getInstance()
            .getReference("users/$studentId/spellingActivity/savedWords")

        ref.get().addOnSuccessListener { snapshot ->
            var count = 0
            for (child in snapshot.children) {
                val skipped = child.child("skipped").getValue(Boolean::class.java) ?: false
                if (!skipped) {
                    count++
                }
            }

            userCount = count
            Log.d(TAG, "User non-skipped words count: $userCount")

            val achievements = listOf(
                AchievementsAdapter.AchievementItem(R.drawable.five_spells, "5 Words", 5),
                AchievementsAdapter.AchievementItem(R.drawable.ten_spells, "10 Words", 10),
                AchievementsAdapter.AchievementItem(R.drawable.fifteen_spells, "15 Words", 15),
                AchievementsAdapter.AchievementItem(R.drawable.twenty_spells, "20 Words", 20),
                AchievementsAdapter.AchievementItem(R.drawable.twenty_five_spells, "25 Words", 25),
                AchievementsAdapter.AchievementItem(R.drawable.thirty_spells, "30 Words", 30),
                AchievementsAdapter.AchievementItem(R.drawable.english_ex, "35 Words", 35),
                AchievementsAdapter.AchievementItem(R.drawable.english_adventurer, "50+ Words", 50)
            )

            recycler.layoutManager = GridLayoutManager(this, 3)
            recycler.adapter = AchievementsAdapter(achievements, userCount)

            Log.d(TAG, "Achievements adapter set with ${achievements.size} items")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to fetch saved words: ${e.message}")
        }
    }
}
