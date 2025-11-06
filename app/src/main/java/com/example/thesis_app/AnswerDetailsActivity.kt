package com.example.thesis_app

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.adapters.QuizResultAdapter
import com.example.thesis_app.models.QuizAnswer
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.database.*

class AnswerDetailsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var quizHeaderText: TextView
    private lateinit var scrollView: NestedScrollView
    private lateinit var answerList: RecyclerView
    private lateinit var adapter: QuizResultAdapter

    private val answers = mutableListOf<QuizAnswer>()
    private val TAG = "AnswerDetailsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.answer_details)

        toolbar = findViewById(R.id.toolbar)
        scrollView = findViewById(R.id.scrollView)
        quizHeaderText = findViewById(R.id.quizIdAnswers)
        answerList = findViewById(R.id.answerList)

        answerList.layoutManager = LinearLayoutManager(this)
        adapter = QuizResultAdapter(this, answers)
        answerList.adapter = adapter

        // 🧠 Retrieve intent data
        val studentId = intent.getStringExtra("studentId") ?: return
        val quizId = intent.getStringExtra("quizId") ?: return
        val rawPartId = intent.getStringExtra("partId") ?: return
        val levelName = intent.getStringExtra("levelName") ?: "Unknown Topic"

        // 🧩 Convert Firebase part key back into user-friendly text
        val displayPartName = when (rawPartId.lowercase()) {
            "part1" -> "Level 1"
            "part2" -> "Level 2"
            "part3" -> "Level 3"
            "post-test" -> "Post-Test"
            else -> rawPartId.replaceFirstChar { it.uppercase() }
        }

        // 🧾 Header title
        quizHeaderText.text = "$levelName - $displayPartName"

        fetchQuizAnswers(studentId, quizId, rawPartId)

        toolbar.setNavigationOnClickListener { finish() }

        // Toolbar title on scroll
        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val threshold = 100
            if (scrollY > threshold) {
                toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor))
                toolbar.title = "$levelName - $displayPartName"
            } else {
                toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor))
                toolbar.title = ""
            }
        }
    }

    /**
     * ✅ Fetch quiz answers, then retrieve corresponding options from quizzes DB
     */
    private fun fetchQuizAnswers(studentId: String, quizId: String, partId: String) {
        val userAnswersRef = FirebaseDatabase.getInstance()
            .getReference("users/$studentId/progress/$quizId/$partId/quizAnswers")

        userAnswersRef.orderByChild("order").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                answers.clear()
                if (!snapshot.exists()) {
                    Log.w(TAG, "No quizAnswers found for $quizId → $partId")
                    adapter.notifyDataSetChanged()
                    return
                }

                for (answerSnap in snapshot.children) {
                    val order = answerSnap.child("order").getValue(Int::class.java) ?: 0
                    val question = answerSnap.child("question").getValue(String::class.java) ?: ""
                    val answer = answerSnap.child("answer").getValue(String::class.java) ?: ""
                    val explanation = answerSnap.child("explanation").getValue(String::class.java) ?: ""
                    val isCorrect = answerSnap.child("isCorrect").getValue(Boolean::class.java) ?: false

                    answers.add(
                        QuizAnswer(
                            order = order,
                            question = question,
                            selectedAnswer = answer,
                            correctAnswer = "",
                            isCorrect = isCorrect,
                            explanation = explanation,
                            options = emptyList()
                        )
                    )
                }

                answers.sortBy { it.order }

                // 🧩 Now fetch the original question list to attach options
                fetchQuestionOptions(quizId, partId)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching answers: ${error.message}")
            }
        })
    }

    /**
     * ✅ Fetch questionList from quizzes DB and match options to answers
     */
    private fun fetchQuestionOptions(quizId: String, partId: String) {
        val questionListRef = FirebaseDatabase.getInstance()
            .getReference("quizzes/$quizId/$partId/questionList")

        questionListRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.w(TAG, "No questionList found in quizzes/$quizId/$partId")
                    adapter.notifyDataSetChanged()
                    return
                }

                // Match each student answer to its original question data
                for (answer in answers) {
                    for (questionSnap in snapshot.children) {
                        val questionText = questionSnap.child("question").getValue(String::class.java) ?: continue
                        if (questionText.trim() == answer.question.trim()) {
                            val correct = questionSnap.child("correct").getValue(String::class.java) ?: ""
                            val options = mutableListOf<String>()
                            for (i in 0..3) {
                                val opt = questionSnap.child("options/$i").getValue(String::class.java)
                                if (opt != null) options.add(opt)
                            }

                            answer.correctAnswer = correct
                            answer.options = options
                            break
                        }
                    }
                }

                adapter.notifyDataSetChanged()
                Log.d(TAG, "✅ Options + correct answers successfully matched to ${answers.size} items")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching question options: ${error.message}")
            }
        })
    }
}
