package com.example.thesis_app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.thesis_app.models.QuestionModel
import com.google.firebase.database.FirebaseDatabase

class QuizActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        var questionModelList: List<QuestionModel> = listOf()
        var time: String = ""
    }

    private lateinit var btn0: Button
    private lateinit var btn1: Button
    private lateinit var btn2: Button
    private lateinit var btn3: Button
    private lateinit var nextBtn: Button
    private lateinit var questionIndicatorTextview: TextView
    private lateinit var questionProgressIndicator: ProgressBar
    private lateinit var questionTextview: TextView
    private lateinit var quizId: String
    private lateinit var studentId: String

    private var currentQuestionIndex = 0
    private var selectedAnswer = ""
    private var selectedAnswerIndex = -1
    private var score = 0
    private var wrongQuestions: MutableList<QuestionModel> = mutableListOf()
    private lateinit var explanationText: TextView
    private var showingExplanation = false

    // ✅ Always keep the original total count
    private var originalTotalQuestions: Int = 0
    private var answeredCount: Int = 0  // ✅ Track across retries
    private lateinit var partId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quiz)

        // Initialize views
        btn0 = findViewById(R.id.btn0)
        btn1 = findViewById(R.id.btn1)
        btn2 = findViewById(R.id.btn2)
        btn3 = findViewById(R.id.btn3)
        nextBtn = findViewById(R.id.next_btn)
        questionIndicatorTextview = findViewById(R.id.question_indicator_textview)
        questionProgressIndicator = findViewById(R.id.question_progress_indicator)
        questionTextview = findViewById(R.id.question_textview)
        explanationText = findViewById(R.id.explanation_text)
        quizId = intent.getStringExtra("QUIZ_ID") ?: ""
        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        studentId = prefs.getString("studentId", "") ?: ""
        partId = intent.getStringExtra("PART_ID") ?: ""

        // Save the original total (before retries)
        originalTotalQuestions = questionModelList.size

        // Set click listeners
        btn0.setOnClickListener(this)
        btn1.setOnClickListener(this)
        btn2.setOnClickListener(this)
        btn3.setOnClickListener(this)
        nextBtn.setOnClickListener(this)

        loadQuestions()

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmation()
                }
            })
    }

    private fun updateProgress(studentId: String, quizId: String, partId: String, answeredCount: Int) {
        val db = FirebaseDatabase.getInstance().reference

        // Determine completion
        val isCompleted = answeredCount >= originalTotalQuestions

        val progressData = mapOf(
            "answeredCount" to answeredCount,
            "totalQuestions" to originalTotalQuestions,
            "isCompleted" to isCompleted,
            "lastUpdated" to System.currentTimeMillis()
        )

        db.child("users")
            .child(studentId)
            .child("progress")
            .child(quizId)
            .child(partId)
            .setValue(progressData)
            .addOnSuccessListener {
                Log.d(
                    "QuizActivity",
                    "Progress updated: $answeredCount/$originalTotalQuestions, isCompleted=$isCompleted"
                )
            }
            .addOnFailureListener { e ->
                Log.w("QuizActivity", "Error updating progress", e)
            }
    }

    @SuppressLint("SetTextI18n")
    private fun loadQuestions() {
        selectedAnswer = ""
        selectedAnswerIndex = -1

        // reset button colors
        btn0.setBackgroundColor(getColor(R.color.gray))
        btn1.setBackgroundColor(getColor(R.color.gray))
        btn2.setBackgroundColor(getColor(R.color.gray))
        btn3.setBackgroundColor(getColor(R.color.gray))

        if (currentQuestionIndex == questionModelList.size) {
            if (quizId != "quiz1" && wrongQuestions.isNotEmpty()) {
                // 🔄 Retry only wrong questions for non-pretest quizzes
                questionModelList = wrongQuestions.toList()
                wrongQuestions.clear()
                currentQuestionIndex = 0
                Toast.makeText(this, "Retry the incorrect questions!", Toast.LENGTH_LONG).show()
                loadQuestions()
                return
            } else {
                // ✅ For quiz1 → no retry
                // ✅ For others → finish if no wrong questions left
                finishQuiz()
                return
            }
        }

        questionIndicatorTextview.text =
            "Question ${currentQuestionIndex + 1}/ ${questionModelList.size} "
        questionProgressIndicator.progress =
            (answeredCount.toFloat() / originalTotalQuestions.toFloat() * 100).toInt()

        val currentQ = questionModelList[currentQuestionIndex]
        questionTextview.text = currentQ.question
        btn0.text = currentQ.options[0]
        btn1.text = currentQ.options[1]
        btn2.text = currentQ.options[2]
        btn3.text = currentQ.options[3]
    }

    override fun onClick(view: View?) {
        val clickedBtn = view as Button

        if (clickedBtn.id == R.id.next_btn) {
            // ✅ Next/Continue button logic
            if (!showingExplanation) {
                if (selectedAnswerIndex == -1) {
                    Toast.makeText(
                        applicationContext,
                        "Please select answer to continue",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val correctRaw = questionModelList[currentQuestionIndex].correct
                val correctIndex = correctRaw.toIntOrNull()
                val isCorrect: Boolean = if (correctIndex != null) {
                    (correctIndex == selectedAnswerIndex) || (correctIndex - 1 == selectedAnswerIndex)
                } else {
                    selectedAnswer.trim().equals(correctRaw.trim(), ignoreCase = true)
                }

                if (isCorrect) {
                    score++
                } else {
                    wrongQuestions.add(questionModelList[currentQuestionIndex])
                }

                if (answeredCount < originalTotalQuestions) {
                    answeredCount++
                    updateProgress(studentId, quizId, partId, answeredCount)
                    updateQuizCompletionStatus(studentId, quizId)
                }

                val explanation = questionModelList[currentQuestionIndex].explanation.ifBlank {
                    if (isCorrect) "Correct! Good job." else "Review this question carefully."
                }

                explanationText.text = explanation
                explanationText.visibility = View.VISIBLE

                // Disable choices
                btn0.isEnabled = false
                btn1.isEnabled = false
                btn2.isEnabled = false
                btn3.isEnabled = false

                showingExplanation = true
                nextBtn.text = "Continue"

            } else {
                // ✅ Load next question
                explanationText.visibility = View.GONE
                showingExplanation = false
                nextBtn.text = "Next"

                // Enable choices again
                btn0.isEnabled = true
                btn1.isEnabled = true
                btn2.isEnabled = true
                btn3.isEnabled = true

                currentQuestionIndex++
                loadQuestions()
            }
        } else {
            // ✅ Option button clicked
            // Reset button colors
            btn0.setBackgroundColor(getColor(R.color.gray))
            btn1.setBackgroundColor(getColor(R.color.gray))
            btn2.setBackgroundColor(getColor(R.color.gray))
            btn3.setBackgroundColor(getColor(R.color.gray))

            // Mark selected
            selectedAnswerIndex = when (clickedBtn.id) {
                R.id.btn0 -> 0
                R.id.btn1 -> 1
                R.id.btn2 -> 2
                R.id.btn3 -> 3
                else -> -1
            }
            selectedAnswer = clickedBtn.text.toString()

            // Highlight selected
            clickedBtn.setBackgroundColor(getColor(R.color.primaryColor))
        }
    }

    fun updateQuizCompletionStatus(studentId: String, quizId: String) {
        val db = FirebaseDatabase.getInstance().reference
        val quizRef = db.child("users").child(studentId).child("progress").child(quizId)

        quizRef.get().addOnSuccessListener { snapshot ->
            // Get all part nodes
            val partNodes = snapshot.children.filter { it.key?.startsWith("part") == true }

            // Check if all parts are completed
            val allCompleted = partNodes.all { part ->
                part.child("isCompleted").getValue(Boolean::class.java) ?: false
            }

            // Update quiz-level isCompleted
            quizRef.child("isCompleted").setValue(allCompleted)
                .addOnSuccessListener {
                    if (allCompleted) {
                        println("Quiz $quizId is now marked as completed for $studentId")
                    }
                }
                .addOnFailureListener { e -> e.printStackTrace() }
        }
    }

    private fun finishQuiz() {
        val percentage = ((score.toFloat() / originalTotalQuestions.toFloat()) * 100).toInt()

        // ✅ Final progress
        updateProgress(studentId, quizId, partId,originalTotalQuestions)

        // Mark pretest as completed
        val db = FirebaseDatabase.getInstance().reference
        db.child("users").child(studentId).child("pretestCompleted").setValue(true)

        Log.d("QuizDebug", "Final Score: $score (percentage $percentage%)")

        val dialogView = layoutInflater.inflate(R.layout.score_dialog, null)
        val scoreProgressIndicator: ProgressBar =
            dialogView.findViewById(R.id.score_progress_indicator)
        val scoreProgressText: TextView = dialogView.findViewById(R.id.score_progress_text)
        val scoreTitle: TextView = dialogView.findViewById(R.id.score_title)
        val scoreSubtitle: TextView = dialogView.findViewById(R.id.score_subtitle)
        val finishBtn: Button = dialogView.findViewById(R.id.finish_btn)

        scoreProgressIndicator.progress = percentage
        scoreProgressText.text = "$percentage %"
        if (percentage > 60) {
            scoreTitle.text = "Congrats! You have passed"
            scoreTitle.setTextColor(Color.BLUE)
        } else {
            scoreTitle.text = "Oops! You have failed"
            scoreTitle.setTextColor(Color.RED)
        }
        scoreSubtitle.text = "$score out of $originalTotalQuestions are correct"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        finishBtn.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, StudentActivity::class.java)
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit")
            .setMessage("Are you sure you want exit? Unsaved progress might be lost.")
            .setPositiveButton("Yes") { _, _ ->
                val intent = Intent(this, StudentActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
}