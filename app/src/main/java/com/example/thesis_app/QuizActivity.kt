package com.example.thesis_app

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.thesis_app.models.QuestionModel
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import android.animation.AnimatorListenerAdapter
import com.google.firebase.database.ValueEventListener
import android.animation.Animator
import com.google.firebase.database.DatabaseReference

class QuizActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        var questionModelList: MutableList<QuestionModel> = mutableListOf()
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
    private var score = 0
    private var showingExplanation = false
    private var isProcessingClick = false
    private lateinit var explanationLayout: View
    private lateinit var timeBeforeNext: LinearProgressIndicator

    private var originalTotalQuestions: Int = 0
    private var answeredCount: Int = 0
    private var correctAnswers = 0
    private var firstTryCorrect = 0
    private var totalWrongAnswers = 0
    private var retries = 0
    private val correctlyAnswered = mutableSetOf<Int>()
    private val incorrectQuestions = mutableListOf<Int>()
    private var isPartCompleted = false

    private val progressData = mutableMapOf<String, Any>()
    private var presenceRef: DatabaseReference? = null

    private val shuffledOptionsMap = mutableMapOf<Int, List<String>>()
    private var isRetryPhase = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quiz)

        // --- View bindings ---
        btn0 = findViewById(R.id.btn0)
        btn1 = findViewById(R.id.btn1)
        btn2 = findViewById(R.id.btn2)
        btn3 = findViewById(R.id.btn3)
        nextBtn = findViewById(R.id.next_btn)
        questionIndicatorTextview = findViewById(R.id.question_indicator_textview)
        questionProgressIndicator = findViewById(R.id.question_progress_indicator)
        questionTextview = findViewById(R.id.question_textview)
        explanationText = findViewById(R.id.explanation_text)
        explanationLayout = findViewById(R.id.explanation_layout)
        timeBeforeNext = findViewById(R.id.time_before_next)

        // --- Intent extras & prefs ---
        quizId = intent.getStringExtra("QUIZ_ID") ?: ""
        partId = intent.getStringExtra("PART_ID") ?: ""
        classCode = intent.getStringExtra("CLASS_CODE") ?: ""
        studentId = intent.getStringExtra("STUDENT_ID")
            ?: getSharedPreferences("USER_PREFS", MODE_PRIVATE).getString("studentId", "") ?: ""

        // --- Animators ---
        questionProgressIndicator.animate().setInterpolator(AccelerateDecelerateInterpolator())
        nextBtn.animate().setInterpolator(AccelerateDecelerateInterpolator())
        explanationLayout.animate().setInterpolator(AccelerateDecelerateInterpolator())

        // --- Presence ---
        presenceRef = FirebaseDatabase.getInstance().getReference("users/$studentId/presence")
        presenceRef?.child("status")?.setValue("in_lecture")
        presenceRef?.child("lastSeen")?.setValue(System.currentTimeMillis())

        // Local helper: common setup once questionModelList is ready
        fun setupAfterQuestionsReady() {
            // Ensure there are questions
            if (questionModelList.isEmpty()) {
                Toast.makeText(this, "No questions available!", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Determine question limit for non-pre-test flows
            val isPostTest = partId.equals("post-test", ignoreCase = true)
            val questionLimit = if (isPostTest) 15 else 5

            // For non-pre-test quizzes, limit questions; for pre-test / special id skip limiting (they are already limited to up to 20)
            if (quizId != "pre-test" && quizId != "835247") {
                val allQuestions = questionModelList.toMutableList()
                allQuestions.shuffle()
                questionModelList = if (allQuestions.size > questionLimit)
                    allQuestions.take(questionLimit).toMutableList()
                else
                    allQuestions
                questionModelList.shuffle()
            } else {
                // For pre-test / 835247: ensure shuffle (they were already limited upstream)
                questionModelList.shuffle()
            }

            // Setup shuffled options map
            originalTotalQuestions = questionModelList.size
            questionModelList.forEachIndexed { index, q ->
                if (q.options.isNotEmpty()) {
                    shuffledOptionsMap[index] = q.options.shuffled()
                }
            }

            // Firebase: check if this part is already completed
            val db = FirebaseDatabase.getInstance().reference
            db.child("users").child(studentId).child("progress").child(quizId).child(partId)
                .child("isCompleted")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        isPartCompleted = snapshot.getValue(Boolean::class.java) ?: false
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@QuizActivity,
                            "Error checking quiz status: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })

            // Initialize progress data map
            progressData["answeredCount"] = answeredCount
            progressData["totalQuestions"] = originalTotalQuestions
            progressData["isCompleted"] = false
            progressData["correctAnswers"] = correctAnswers
            progressData["firstTryCorrect"] = firstTryCorrect
            progressData["wrongAnswers"] = totalWrongAnswers
            progressData["retries"] = retries
            progressData["lastUpdated"] = System.currentTimeMillis()

            // Set click listeners
            btn0.setOnClickListener(this)
            btn1.setOnClickListener(this)
            btn2.setOnClickListener(this)
            btn3.setOnClickListener(this)
            nextBtn.setOnClickListener(this)

            // Start quiz
            loadQuestions()

            // Handle back press
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmation()
                }
            })
        }

        // --- Pre-test / special quiz handling ---
        if (quizId == "pre-test" || quizId == "835247") {
            // If questions were already provided (e.g., QuizTimePage set QuizActivity.questionModelList),
            // use them directly. Otherwise fetch from DB at quizzes/{quizId}/part1/questionList
            if (questionModelList.isNotEmpty()) {
                // Use already-populated questions (from previous fragment)
                setupAfterQuestionsReady()
            } else {
                val dbRef = FirebaseDatabase.getInstance()
                    .getReference("quizzes")
                    .child(quizId)
                    .child("part1")
                    .child("questionList") // matches your QuizTimePage usage

                dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val fetchedQuestions = mutableListOf<QuestionModel>()
                        for (qSnap in snapshot.children) {
                            val q = qSnap.getValue(QuestionModel::class.java)
                            if (q != null) fetchedQuestions.add(q)
                        }

                        if (fetchedQuestions.isEmpty()) {
                            Toast.makeText(this@QuizActivity, "No questions found for pre-test part1.", Toast.LENGTH_LONG).show()
                            finish()
                            return
                        }

                        // Shuffle and take up to 20
                        fetchedQuestions.shuffle()
                        questionModelList = if (fetchedQuestions.size > 20)
                            fetchedQuestions.take(20).toMutableList()
                        else
                            fetchedQuestions.toMutableList()

                        setupAfterQuestionsReady()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@QuizActivity, "Failed to load pre-test questions: ${error.message}", Toast.LENGTH_LONG).show()
                        finish()
                    }
                })
            }
        } else {
            // For all other quizIds keep existing behavior: use already populated questionModelList
            if (questionModelList.isEmpty()) {
                Toast.makeText(this, "No questions available!", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            setupAfterQuestionsReady()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadQuestions() {
        if (currentQuestionIndex >= questionModelList.size) {
            checkAndFinishQuiz()
            return
        }

        selectedAnswer = ""
        selectedAnswerIndex = -1

        val buttons = listOf(btn0, btn1, btn2, btn3)
        buttons.forEach { it.setBackgroundColor(getColor(R.color.gray)) }

        val currentQ = questionModelList[currentQuestionIndex]

        questionIndicatorTextview.text =
            if (currentQ.instruction.isNotBlank()) currentQ.instruction
            else "Question ${currentQuestionIndex + 1}/${questionModelList.size}"

        questionProgressIndicator.max = questionModelList.size
        ObjectAnimator.ofInt(
            questionProgressIndicator,
            "progress",
            questionProgressIndicator.progress,
            currentQuestionIndex + 1
        ).apply {
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        questionTextview.text = currentQ.question

        // ✅ Use shuffled options only on first attempt
        val optionsToShow = if (!isRetryPhase)
            shuffledOptionsMap[currentQuestionIndex] ?: currentQ.options
        else
            currentQ.options

        btn0.text = optionsToShow.getOrNull(0) ?: ""
        btn1.text = optionsToShow.getOrNull(1) ?: ""
        btn2.text = optionsToShow.getOrNull(2) ?: ""
        btn3.text = optionsToShow.getOrNull(3) ?: ""
    }

    // ✅ Save all quiz answers after finishing (first try only)
    // ✅ Save all quiz answers after finishing (first try only)
    private fun recordAllAnswersAfterQuiz() {
        if (retries > 0) return // ❗ Only first attempt

        val db = FirebaseDatabase.getInstance().reference
        val quizPath = db.child("users")
            .child(studentId)
            .child("progress")
            .child(quizId)
            .child(partId)
            .child("quizAnswers")

        val answersData = mutableMapOf<String, Any>()

        questionModelList.forEachIndexed { index, question ->
            val wasCorrect = correctlyAnswered.contains(index)
            val selectedAnswerText = if (wasCorrect) {
                // Student got it right; answer was the correct one
                val correctRaw = question.correct.trim()
                val correctAnswerText = correctRaw.toIntOrNull()?.let {
                    if (it in question.options.indices) question.options[it]
                    else correctRaw
                } ?: correctRaw
                correctAnswerText
            } else {
                // If got it wrong, store the last selected answer
                selectedAnswer
            }

            val resultData = mapOf(
                "order" to (index + 1),
                "question" to question.question,
                "answer" to selectedAnswerText,
                "isCorrect" to wasCorrect,
                "explanation" to (question.explanation.ifBlank {
                    if (wasCorrect) "Correct! Good job." else "Review this question carefully."
                }),
                "timestamp" to System.currentTimeMillis()
            )

            answersData["Q${index + 1}"] = resultData
        }

        // ✅ Store answers under user's progress → quizId → partId → quizAnswers
        quizPath.updateChildren(answersData)
    }

    override fun onClick(view: View?) {
        if (isProcessingClick) return

        val clickedBtn = view as Button

        if (clickedBtn.id == R.id.next_btn) {
            handleNextButton()
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

    private fun handleNextButton() {
        isProcessingClick = true
        if (!showingExplanation) {
            val currentQ = questionModelList[currentQuestionIndex]
            val optionsToCheck = if (!isRetryPhase)
                shuffledOptionsMap[currentQuestionIndex] ?: currentQ.options
            else
                currentQ.options

            val correctRaw = currentQ.correct.trim()
            val correctAnswerText = correctRaw.toIntOrNull()?.let {
                if (it in currentQ.options.indices) currentQ.options[it]
                else correctRaw
            } ?: correctRaw

            val correctIndexInDisplay =
                optionsToCheck.indexOfFirst { it.equals(correctAnswerText, true) }

            val isCorrect = selectedAnswer.equals(correctAnswerText, true)
            val buttons = listOf(btn0, btn1, btn2, btn3)

            if (selectedAnswerIndex == -1) {
                Toast.makeText(this, "Please select an answer first!", Toast.LENGTH_SHORT).show()
                isProcessingClick = false
                return
            }

            if (correctIndexInDisplay in buttons.indices)
                buttons[correctIndexInDisplay].setBackgroundColor(getColor(R.color.green))

            if (!isCorrect && selectedAnswerIndex in buttons.indices)
                buttons[selectedAnswerIndex].setBackgroundColor(getColor(R.color.red))

            if (isCorrect && !correctlyAnswered.contains(currentQuestionIndex)) {
                score++
                correctAnswers++
                if (retries == 0 && answeredCount < originalTotalQuestions) firstTryCorrect++
                correctlyAnswered.add(currentQuestionIndex)
            } else if (!isCorrect && !incorrectQuestions.contains(currentQuestionIndex)) {
                totalWrongAnswers++
                incorrectQuestions.add(currentQuestionIndex)
            }

            answeredCount++
            progressData["answeredCount"] = answeredCount
            progressData["correctAnswers"] = correctAnswers
            progressData["firstTryCorrect"] = firstTryCorrect
            progressData["wrongAnswers"] = totalWrongAnswers
            progressData["lastUpdated"] = System.currentTimeMillis()

            val explanation = currentQ.explanation.ifBlank {
                if (isCorrect) "Correct! Good job." else "Review this question carefully."
            }

            showExplanationAndNext(explanation)
            showingExplanation = true
            isProcessingClick = false
        }
    }

    private fun showExplanationAndNext(explanation: String) {
        // Hide the "Next" button until the timer ends
        nextBtn.visibility = View.GONE

        // Disable all answer buttons during explanation
        val buttons = listOf(btn0, btn1, btn2, btn3)
        buttons.forEach { it.isEnabled = false }

        // Show explanation
        explanationText.text = explanation
        explanationLayout.visibility = View.VISIBLE

        // Reset and animate progress bar
        timeBeforeNext.progress = 0
        timeBeforeNext.max = 100

        val animation = ObjectAnimator.ofInt(timeBeforeNext, "progress", 0, 100)
        animation.duration = 3000
        animation.interpolator = LinearInterpolator()
        animation.start()

        animation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                explanationLayout.animate().alpha(0f).setDuration(400).withEndAction {
                    explanationLayout.visibility = View.GONE
                    explanationLayout.alpha = 1f

                    nextBtn.animate().alpha(0f).setDuration(400).withEndAction {
                        nextBtn.visibility = View.GONE
                        nextBtn.alpha = 1f

                        nextBtn.postDelayed({
                            // Move to the next question
                            currentQuestionIndex++
                            while (currentQuestionIndex < questionModelList.size &&
                                correctlyAnswered.contains(currentQuestionIndex)
                            ) {
                                currentQuestionIndex++
                            }

                            loadQuestions()

                            // ✅ Re-enable buttons for next question
                            buttons.forEach { it.isEnabled = true }

                            showingExplanation = false
                            isProcessingClick = false

                            nextBtn.alpha = 0f
                            nextBtn.visibility = View.VISIBLE
                            nextBtn.animate().alpha(1f).setDuration(400).start()
                        }, 200)
                    }.start()
                }.start()
            }
        })
    }

    private fun saveProgressToFirebase(quizId: String, partId: String, progress: Map<String, Any>) {
        val db = FirebaseDatabase.getInstance().reference
        val progressRef = db.child("users")
            .child(studentId)
            .child("progress")
            .child(quizId)
            .child(partId)

        // ✅ Only update the progress fields we want
        val safeData = mapOf(
            "answeredCount" to (progress["answeredCount"] ?: 0),
            "correctAnswers" to (progress["correctAnswers"] ?: 0),
            "firstTryCorrect" to (progress["firstTryCorrect"] ?: 0),
            "isCompleted" to (progress["isCompleted"] ?: false),
            "lastUpdated" to (progress["lastUpdated"] ?: System.currentTimeMillis()),
            "retries" to (progress["retries"] ?: 0),
            "totalQuestions" to (progress["totalQuestions"] ?: 0),
            "wrongAnswers" to (progress["wrongAnswers"] ?: 0)
        )

        progressRef.updateChildren(safeData)
    }


    private fun updateQuizCompletionStatus(studentId: String, quizId: String) {
        if (isPartCompleted) return
        val db = FirebaseDatabase.getInstance().reference
        val quizRef = db.child("users").child(studentId).child("progress").child(quizId)
        quizRef.get().addOnSuccessListener { snapshot ->
            val allCompleted = snapshot.children.filter {
                it.key?.startsWith("part") == true || it.key == "post-test"
            }.all { it.child("isCompleted").getValue(Boolean::class.java) ?: false }
            quizRef.child("isCompleted").setValue(allCompleted)
        }
    }

    private fun checkAndFinishQuiz() {
        val isRetryAllowed = !(quizId == "pre-test" || quizId == "835247" || partId == "post-test")

        if (correctAnswers >= originalTotalQuestions || !isRetryAllowed) {
            finishQuiz()
            return
        }

        // ✅ Save all first-attempt answers before retry starts
        recordAllAnswersAfterQuiz()

        retries++
        progressData["retries"] = retries
        isRetryPhase = true

        AlertDialog.Builder(this)
            .setTitle("Try Again")
            .setMessage("You got ${originalTotalQuestions - correctAnswers} question(s) wrong. Please retry the incorrect questions.")
            .setPositiveButton("Retry") { _, _ ->
                answeredCount = 0
                score = 0
                currentQuestionIndex = 0
                loadQuestions()
            }
            .setCancelable(false)
            .show()
    }

    private fun finishQuiz() {
        recordAllAnswersAfterQuiz()

        val percentage = ((correctAnswers.toFloat() / originalTotalQuestions.toFloat()) * 100).toInt()

        progressData["isCompleted"] = true
        progressData["lastUpdated"] = System.currentTimeMillis()
        progressData.remove("quizAnswers")
        saveProgressToFirebase(quizId, partId, progressData)
        updateQuizCompletionStatus(studentId, quizId)
        if (quizId == "pre-test" || quizId == "835247") {
            markPretestCompleted()
        }
        val dialogView = layoutInflater.inflate(R.layout.score_dialog, null)
        val scoreProgressIndicator: ProgressBar = dialogView.findViewById(R.id.score_progress_indicator)
        val scoreProgressText: TextView = dialogView.findViewById(R.id.score_progress_text)
        val scoreTitle: TextView = dialogView.findViewById(R.id.score_title)
        val finishBtn: Button = dialogView.findViewById(R.id.finish_btn)

        scoreProgressIndicator.progress = percentage
        scoreProgressText.text = "$percentage %"
        scoreTitle.text = "Congrats! You have completed this test!"
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
        presenceRef?.child("status")?.setValue("online")
        presenceRef?.child("lastSeen")?.setValue(System.currentTimeMillis())
    }

    private fun markPretestCompleted() {
        val id = studentId.ifEmpty {
            getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                .getString("studentId", null)
        }

        if (!id.isNullOrEmpty()) {
            val dbRef = FirebaseDatabase.getInstance().reference
            dbRef.child("users").child(id).child("pretestCompleted").setValue(true)
        }
    }


    private fun showExitConfirmation() {
        val builder = AlertDialog.Builder(this)
        if (quizId == "pre-test" || quizId == "835247") {
            builder.setTitle("Exit Quiz")
                .setMessage("Are you sure you want to exit? Your progress will not be saved for this quiz.")
        } else {
            builder.setTitle("Exit")
                .setMessage(if (isPartCompleted) "Are you sure you want to exit review?"
                else "Are you sure you want to exit? Progress might not be saved.")
        }

        builder.setPositiveButton("Yes") { _, _ ->
            presenceRef?.child("status")?.setValue("online")
            presenceRef?.child("lastSeen")?.setValue(System.currentTimeMillis())
            val intent = Intent(this, StudentActivity::class.java)
            startActivity(intent)
            finish()
        }
            .setNegativeButton("No", null)
            .setCancelable(false)
            .show()
    }
}