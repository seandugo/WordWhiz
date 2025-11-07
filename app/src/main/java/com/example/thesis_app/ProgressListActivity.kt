package com.example.thesis_app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.ProgressItem
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.database.FirebaseDatabase

class ProgressListActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var studentIdProgress: TextView
    private lateinit var scrollView: NestedScrollView
    private lateinit var progressList: RecyclerView
    private lateinit var progressAdapter: ProgressListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student_progress_list)

        toolbar = findViewById(R.id.toolbar)
        scrollView = findViewById(R.id.scrollView)
        studentIdProgress = findViewById(R.id.studentIdProgress)
        progressList = findViewById(R.id.progressList)
        val studentId = intent.getStringExtra("studentId") ?: "Unknown"
        progressList.isNestedScrollingEnabled = false

        // Pass click listener to adapter
        progressAdapter = ProgressListAdapter { part ->
            // Launch new activity on part click
            val intent = Intent(this, QuizDetailActivity::class.java)
            intent.putExtra("levelName", part.levelName)
            intent.putExtra("quizId", part.quizId)
            intent.putExtra("studentId", studentId)
            startActivity(intent)
        }

        progressList.layoutManager = LinearLayoutManager(this)
        progressList.adapter = progressAdapter

        studentIdProgress.text = studentId
        fetchProgressData(studentId)

        // Back button
        toolbar.setNavigationOnClickListener { finish() }

        // Change toolbar background on scroll
        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val threshold = 100
            if (scrollY > threshold) {
                toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor))
                toolbar.title = "Your Progress"
            } else {
                toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor))
                toolbar.title = ""
            }
        }
    }

    private fun fetchProgressData(studentId: String) {
        val quizzesRef = FirebaseDatabase.getInstance().getReference("users/$studentId/progress")

        quizzesRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                progressAdapter.updateData(emptyList())
                return@addOnSuccessListener
            }

            val newItems = mutableListOf<ProgressItem.Part>()

            // Get all quizzes with order, excluding pre-test or specific quiz ID
            val quizList = snapshot.children.mapNotNull { quizSnapshot ->
                val quizId = quizSnapshot.key ?: return@mapNotNull null

                val order = quizSnapshot.child("order").getValue(Int::class.java) ?: Int.MAX_VALUE
                quizSnapshot to order
            }.sortedBy { it.second }

            quizList.forEach { (quizSnapshot, _) ->
                val quizId = quizSnapshot.key ?: return@forEach
                val quizTitleRef = FirebaseDatabase.getInstance()
                    .getReference("quizzes/$quizId/title")

                quizTitleRef.get().addOnSuccessListener { titleSnapshot ->
                    val quizTitle = titleSnapshot.getValue(String::class.java) ?: "Untitled Quiz"

                    // Calculate completed parts
                    val partsSnapshot = quizSnapshot
                    val partKeys = partsSnapshot.children.mapNotNull { it.key }
                        .filter { it.startsWith("part") || it == "post-test" }

                    val completedParts = partsSnapshot.children.count { partSnapshot ->
                        partSnapshot.child("isCompleted").getValue(Boolean::class.java) == true
                    }

                    newItems.add(
                        ProgressItem.Part(
                            levelName = quizTitle,
                            totalParts = partKeys.size,
                            completedParts = completedParts,
                            quizId = quizId,
                            isCompleted = quizSnapshot.child("isCompleted").getValue(Boolean::class.java) ?: false
                        )
                    )

                    // Update adapter after each quiz
                    progressAdapter.updateData(newItems)
                }.addOnFailureListener { it.printStackTrace() }
            }
        }.addOnFailureListener { it.printStackTrace() }
    }
}
