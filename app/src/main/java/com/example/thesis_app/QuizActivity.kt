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
    private var selectedAnswerIndex = -1   // <-- track index (0..3)
    private var score = 0

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
        quizId = intent.getStringExtra("QUIZ_ID") ?: ""
        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        studentId = prefs.getString("studentId", "") ?: ""

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

    private fun updateProgress(studentId: String, quizId: String, answeredCount: Int, totalQuestions: Int) {
        val db = FirebaseDatabase.getInstance().reference

        val progressData = mapOf(
            "answeredCount" to answeredCount,
            "totalQuestions" to totalQuestions,
            "lastUpdated" to System.currentTimeMillis()
        )

        db.child("users")
            .child(studentId)
            .child("progress")
            .child(quizId)
            .setValue(progressData)
            .addOnSuccessListener {
                Log.d("QuizActivity", "Progress updated: $answeredCount/$totalQuestions")
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
            finishQuiz()
            return
        }

        questionIndicatorTextview.text =
            "Question ${currentQuestionIndex + 1}/ ${questionModelList.size} "
        questionProgressIndicator.progress =
            (currentQuestionIndex.toFloat() / questionModelList.size.toFloat() * 100).toInt()
        questionTextview.text = questionModelList[currentQuestionIndex].question
        btn0.text = questionModelList[currentQuestionIndex].options[0]
        btn1.text = questionModelList[currentQuestionIndex].options[1]
        btn2.text = questionModelList[currentQuestionIndex].options[2]
        btn3.text = questionModelList[currentQuestionIndex].options[3]
    }

    override fun onClick(view: View?) {
        // Reset button colors every click (so selected is highlighted after)
        btn0.setBackgroundColor(getColor(R.color.gray))
        btn1.setBackgroundColor(getColor(R.color.gray))
        btn2.setBackgroundColor(getColor(R.color.gray))
        btn3.setBackgroundColor(getColor(R.color.gray))

        val clickedBtn = view as Button

        if (clickedBtn.id == R.id.next_btn) {
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

            // Logging for debugging
            Log.d("QuizDebug", "SelectedText='$selectedAnswer' SelectedIdx=$selectedAnswerIndex CorrectRaw='$correctRaw' CorrectIdx=$correctIndex isCorrect=$isCorrect")

            if (isCorrect) {
                score++
                Log.i("QuizActivity", "Correct! Score: $score")
            } else {
                Log.i("QuizActivity", "Wrong! Selected: $selectedAnswer, Correct: $correctRaw")
            }

            val answeredCount = currentQuestionIndex + 1
            val totalQuestions = questionModelList.size

            // Always update progress when Next is pressed
            updateProgress(studentId, quizId, answeredCount, totalQuestions)

            currentQuestionIndex++
            loadQuestions()
        } else {
            // options button is clicked -> set selected text & index and highlight
            selectedAnswer = clickedBtn.text.toString()
            selectedAnswerIndex = when (clickedBtn.id) {
                R.id.btn0 -> 0
                R.id.btn1 -> 1
                R.id.btn2 -> 2
                R.id.btn3 -> 3
                else -> -1
            }
            clickedBtn.setBackgroundColor(getColor(R.color.primaryColor))
        }
    }

    private fun finishQuiz() {
        val totalQuestions = questionModelList.size
        val percentage = ((score.toFloat() / totalQuestions.toFloat()) * 100).toInt()

        // Make sure progress is saved even if it's the last question
        updateProgress(studentId, quizId, totalQuestions, totalQuestions)

        // Mark pretest as completed in Firebase
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
        scoreSubtitle.text = "$score out of $totalQuestions are correct"

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
            .setMessage("Are you sure you want exit? Unsaved progress might lost.")
            .setPositiveButton("Yes") { _, _ ->
                val intent = Intent(this, StudentActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
}