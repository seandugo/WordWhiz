package com.example.thesis_app

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import androidx.core.content.ContextCompat
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.QuizDetailProgress
import com.google.firebase.database.FirebaseDatabase

class QuizDetailActivity : AppCompatActivity() {

    private lateinit var appBarLayout: AppBarLayout
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var toolbar: MaterialToolbar

    private lateinit var nameTextExpanded: TextView
    private lateinit var subtitleQuiz: TextView
    private lateinit var progressRecycler: RecyclerView

    private val TAG = "QuizDetailActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quiz_review)

        // AppBar / Toolbar
        appBarLayout = findViewById(R.id.appbar)
        collapsingToolbar = findViewById(R.id.collapsingToolbar)
        toolbar = findViewById(R.id.toolbar)

        // Header Text
        nameTextExpanded = findViewById(R.id.nameTextExpanded)
        subtitleQuiz = findViewById(R.id.subtitleQuiz)

        // Back button
        toolbar.setNavigationOnClickListener {
            finish()
        }

        val quizTitle = intent.getStringExtra("levelName") ?: "Quiz Details"
        nameTextExpanded.text = quizTitle
        collapsingToolbar.title = ""

        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            val isCollapsed = totalScrollRange + verticalOffset <= 0

            if (isCollapsed) {
                toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.black))
            } else {
                toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.white))
            }
        })

        // RecyclerView
        progressRecycler = findViewById(R.id.resultRecycler)
        val progressList = mutableListOf<QuizDetailProgress>()
        val adapter = QuizDetailAdapter(
            progressList,
            onReviewClick = { Log.d(TAG, "Review clicked") },
            onRetakeClick = { Log.d(TAG, "Retake clicked") }
        )

        progressRecycler.layoutManager = LinearLayoutManager(this)
        progressRecycler.adapter = adapter

        val quizId = intent.getStringExtra("quizId") ?: ""
        val studentId = intent.getStringExtra("studentId") ?: ""
        Log.d(TAG, "Fetching progress for studentId=$studentId, quizId=$quizId")

        FirebaseDatabase.getInstance()
            .getReference("users/$studentId/progress/$quizId")
            .get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    Log.d(TAG, "Snapshot exists with ${snapshot.childrenCount} children")

                    snapshot.children.forEach { partSnapshot ->
                        val partKey = partSnapshot.key ?: return@forEach
                        if (partKey.startsWith("part") || partKey == "post-test") {

                            val levelName = when (partKey) {
                                "part1" -> "Level 1"
                                "part2" -> "Level 2"
                                "part3" -> "Level 3"
                                "post-test" -> "Post-Test"
                                else -> partKey.capitalize()
                            }

                            val isCompleted = partSnapshot.child("isCompleted")
                                .getValue(Boolean::class.java) ?: false

                            val totalQuestions = partSnapshot.child("totalQuestions")
                                .getValue(Int::class.java) ?: 0

                            val correctAnswers = partSnapshot.child("correctAnswers")
                                .getValue(Int::class.java) ?: 0

                            val retryAnswers = partSnapshot.child("retryAnswers")
                                .getValue(Int::class.java) ?: 0

                            val wrongAnswers = totalQuestions - correctAnswers

                            // Add a separate card for this part
                            progressList.add(
                                QuizDetailProgress(
                                    levelName = levelName,
                                    totalParts = totalQuestions,
                                    correctParts = correctAnswers,
                                    wrongParts = wrongAnswers,
                                    retryParts = retryAnswers
                                )
                            )
                        }
                    }

                    adapter.notifyDataSetChanged()
                    Log.d(TAG, "Adapter notified, list size: ${progressList.size}")
                } else {
                    Log.d(TAG, "Snapshot does not exist! studentId=$studentId")
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch data: ${e.message}")
            }
    }
    }
