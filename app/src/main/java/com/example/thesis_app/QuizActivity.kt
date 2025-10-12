package com.example.thesis_app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

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
    private lateinit var explanationText: TextView
    private lateinit var classCode: String
    private lateinit var partId: String

    private var currentQuestionIndex = 0
    private var selectedAnswer = ""
    private var selectedAnswerIndex = -1
    private var score = 0 // Current attempt score
    private var showingExplanation = false

    private var originalTotalQuestions: Int = 0
    private var answeredCount: Int = 0
    private var correctAnswers = 0 // Persistent count of unique correct answers
    private var firstTryCorrect = 0 // Count of questions correct on first attempt
    private var totalWrongAnswers = 0 // Accumulated wrong answers across all attempts
    private var retries = 0 // Number of retry attempts
    private val correctlyAnswered = mutableSetOf<Int>() // Tracks indices of correctly answered questions
    private val incorrectQuestions = mutableListOf<Int>() // Tracks indices for retry in current attempt
    private var isPartCompleted = false // Tracks if the part is already completed in Firebase

    // Local storage for progress
    private val progressData = mutableMapOf<String, Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quiz)

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
        partId = intent.getStringExtra("PART_ID") ?: ""
        classCode = intent.getStringExtra("CLASS_CODE") ?: ""
        studentId = intent.getStringExtra("STUDENT_ID") ?: getSharedPreferences("USER_PREFS", MODE_PRIVATE).getString("studentId", "") ?: ""

        originalTotalQuestions = questionModelList.size
        if (originalTotalQuestions == 0) {
            Toast.makeText(this, "No questions available!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Check if the part is already completed
        val db = FirebaseDatabase.getInstance().reference
        db.child("users").child(studentId).child("progress").child(quizId).child(partId).child("isCompleted")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isPartCompleted = snapshot.getValue(Boolean::class.java) ?: false
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@QuizActivity, "Error checking quiz status: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

        // Initialize local progress data
        progressData["answeredCount"] = answeredCount
        progressData["totalQuestions"] = originalTotalQuestions
        progressData["isCompleted"] = false
        progressData["correctAnswers"] = correctAnswers
        progressData["firstTryCorrect"] = firstTryCorrect
        progressData["wrongAnswers"] = totalWrongAnswers
        progressData["retries"] = retries
        progressData["lastUpdated"] = System.currentTimeMillis()

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

    @SuppressLint("SetTextI18n")
    private fun loadQuestions() {
        selectedAnswer = ""
        selectedAnswerIndex = -1

        btn0.setBackgroundColor(getColor(R.color.gray))
        btn1.setBackgroundColor(getColor(R.color.gray))
        btn2.setBackgroundColor(getColor(R.color.gray))
        btn3.setBackgroundColor(getColor(R.color.gray))

        if (currentQuestionIndex >= questionModelList.size) {
            checkAndFinishQuiz()
            return
        }

        // ✅ Change text based on retry mode
        if (retries > 0) {
            questionIndicatorTextview.text = "Let's Try Again!"
        } else {
            questionIndicatorTextview.text = "Question ${currentQuestionIndex + 1}/${questionModelList.size}"
        }

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
            if (!showingExplanation) {
                if (selectedAnswerIndex == -1) {
                    Toast.makeText(this, "Please select an answer to continue", Toast.LENGTH_SHORT).show()
                    return
                }

                val correctRaw = questionModelList[currentQuestionIndex].correct
                val correctIndex = correctRaw.toIntOrNull()
                val isCorrect: Boolean = if (correctIndex != null) {
                    (correctIndex == selectedAnswerIndex) || (correctIndex - 1 == selectedAnswerIndex)
                } else {
                    selectedAnswer.trim().equals(correctRaw.trim(), ignoreCase = true)
                }

                val correctBtnIndex = correctIndex?.let {
                    if (it in 0..3) it else if (it - 1 in 0..3) it - 1 else -1
                } ?: -1

                val buttons = listOf(btn0, btn1, btn2, btn3)

                if (correctBtnIndex in buttons.indices)
                    buttons[correctBtnIndex].setBackgroundColor(getColor(R.color.green))

                if (!isCorrect && selectedAnswerIndex in buttons.indices)
                    buttons[selectedAnswerIndex].setBackgroundColor(getColor(R.color.red))

                if (isCorrect && !correctlyAnswered.contains(currentQuestionIndex)) {
                    score++
                    correctAnswers++
                    if (retries == 0 && answeredCount < originalTotalQuestions) {
                        firstTryCorrect++ // Only count first-try correct on initial attempt
                    }
                    correctlyAnswered.add(currentQuestionIndex)
                } else if (!isCorrect && !incorrectQuestions.contains(currentQuestionIndex)) {
                    totalWrongAnswers++
                    incorrectQuestions.add(currentQuestionIndex)
                }

                answeredCount++

                // Update local progress
                progressData["answeredCount"] = answeredCount
                progressData["correctAnswers"] = correctAnswers
                progressData["firstTryCorrect"] = firstTryCorrect
                progressData["wrongAnswers"] = totalWrongAnswers
                progressData["lastUpdated"] = System.currentTimeMillis()

                val explanation = questionModelList[currentQuestionIndex].explanation.ifBlank {
                    if (isCorrect) "Correct! Good job." else "Review this question carefully."
                }

                explanationText.text = explanation
                explanationText.visibility = View.VISIBLE

                buttons.forEach { it.isEnabled = false }

                showingExplanation = true
                nextBtn.text = "Continue"

            } else {
                explanationText.visibility = View.GONE
                showingExplanation = false
                nextBtn.text = "Next"

                // Disable all buttons during transition
                btn0.isEnabled = false
                btn1.isEnabled = false
                btn2.isEnabled = false
                btn3.isEnabled = false
                nextBtn.isEnabled = false

                // Move to next unanswered or incorrect question
                currentQuestionIndex++
                while (currentQuestionIndex < questionModelList.size && correctlyAnswered.contains(currentQuestionIndex)) {
                    currentQuestionIndex++ // Skip already correct questions
                }

                // Add delay (e.g., 800 ms)
                nextBtn.postDelayed({
                    // Load the next question
                    loadQuestions()

                    // Re-enable buttons after transition
                    btn0.isEnabled = true
                    btn1.isEnabled = true
                    btn2.isEnabled = true
                    btn3.isEnabled = true
                    nextBtn.isEnabled = true
                }, 800)
            }
        } else {
            val buttons = listOf(btn0, btn1, btn2, btn3)
            buttons.forEach { it.setBackgroundColor(getColor(R.color.gray)) }

            selectedAnswerIndex = when (clickedBtn.id) {
                R.id.btn0 -> 0
                R.id.btn1 -> 1
                R.id.btn2 -> 2
                R.id.btn3 -> 3
                else -> -1
            }
            selectedAnswer = clickedBtn.text.toString()

            clickedBtn.setBackgroundColor(getColor(R.color.primaryColor))
        }
    }

    private fun updateProgress(studentId: String, quizId: String, partId: String) {
        if (isPartCompleted) return

        progressData["isCompleted"] = correctAnswers >= originalTotalQuestions
        progressData["lastUpdated"] = System.currentTimeMillis()

        saveProgressToFirebase(quizId, partId, progressData)
    }

    private fun saveProgressToFirebase(quizId: String, partId: String, progress: Map<String, Any>) {
        val db = FirebaseDatabase.getInstance().reference

        val paths = mutableListOf(
            db.child("users").child(studentId).child("progress").child(quizId).child(partId)
        )

        if (classCode.isNotEmpty()) {
            paths.add(
                db.child("classes").child(classCode).child("students").child(studentId)
                    .child("progress").child(quizId).child(partId)
            )
        }

        paths.forEach { pathRef ->
            pathRef.updateChildren(progress)
        }
    }

    private fun updateQuizCompletionStatus(studentId: String, quizId: String) {
        if (isPartCompleted) return

        val db = FirebaseDatabase.getInstance().reference

        val paths = mutableListOf(
            db.child("users").child(studentId).child("progress").child(quizId)
        )

        if (classCode.isNotEmpty()) {
            paths.add(
                db.child("classes").child(classCode).child("students").child(studentId)
                    .child("progress").child(quizId)
            )
        }

        paths.forEach { quizRef ->
            quizRef.get().addOnSuccessListener { snapshot ->
                val partNodes = snapshot.children.filter { it.key?.startsWith("part") == true || it.key == "post-test" }
                val allCompleted = partNodes.all { part -> part.child("isCompleted").getValue(Boolean::class.java) ?: false }
                quizRef.child("isCompleted").setValue(allCompleted)
            }
        }
    }

    private fun checkAndFinishQuiz() {
        // Disable retry for pre-test and quiz 835247, and post-test
        val isRetryAllowed = !(quizId == "pre-test" || quizId == "835247" || partId == "post-test")

        if (correctAnswers >= originalTotalQuestions || !isRetryAllowed) {
            finishQuiz()
            return
        }

        // Normal retry flow
        retries++
        progressData["retries"] = retries

        val retryDialog = AlertDialog.Builder(this)
            .setTitle("Try Again")
            .setMessage("You got ${originalTotalQuestions - correctAnswers} question(s) wrong. Please retry the incorrect questions.")
            .setPositiveButton("Retry") { _, _ ->
                // Reset for retry
                answeredCount = 0
                score = 0
                incorrectQuestions.clear()
                // Start with first unanswered question
                currentQuestionIndex = 0
                while (currentQuestionIndex < questionModelList.size && correctlyAnswered.contains(currentQuestionIndex)) {
                    currentQuestionIndex++
                }
                progressData["answeredCount"] = answeredCount
                loadQuestions()
            }
            .setCancelable(false)
            .show()
    }

    private fun finishQuiz() {
        val percentage = ((correctAnswers.toFloat() / originalTotalQuestions.toFloat()) * 100).toInt()

        // Force the pre-test part (835247 / part1) to completed
        progressData["isCompleted"] = true
        progressData["lastUpdated"] = System.currentTimeMillis()
        saveProgressToFirebase(quizId, partId, progressData) // part1.isCompleted = true

        // Update the quiz completion status
        updateQuizCompletionStatus(studentId, quizId) // 835247.isCompleted = true

        // Optional: mark pretestCompleted flag for app logic
        val db = FirebaseDatabase.getInstance().reference
        db.child("users").child(studentId).child("pretestCompleted").setValue(true)

        // Show score dialog
        val dialogView = layoutInflater.inflate(R.layout.score_dialog, null)
        val scoreProgressIndicator: ProgressBar = dialogView.findViewById(R.id.score_progress_indicator)
        val scoreProgressText: TextView = dialogView.findViewById(R.id.score_progress_text)
        val scoreTitle: TextView = dialogView.findViewById(R.id.score_title)
        val finishBtn: Button = dialogView.findViewById(R.id.finish_btn)

        scoreProgressIndicator.progress = percentage
        scoreProgressText.text = "$percentage %"

        scoreTitle.text = "Congrats! You completed the pre-test!"
        scoreTitle.setTextColor(Color.BLUE)

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
            .setMessage(
                if (isPartCompleted) "Are you sure you want to exit review?"
                else "Are you sure you want to exit? Progress might not be saved."
            )
            .setPositiveButton("Yes") { _, _ ->
                val intent = Intent(this, StudentActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
}