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

        quizId = intent.getStringExtra("QUIZ_ID") ?: ""
        partId = intent.getStringExtra("PART_ID") ?: ""
        classCode = intent.getStringExtra("CLASS_CODE") ?: ""
        studentId = intent.getStringExtra("STUDENT_ID")
            ?: getSharedPreferences("USER_PREFS", MODE_PRIVATE).getString("studentId", "") ?: ""

        nextBtn.animate().setInterpolator(AccelerateDecelerateInterpolator())
        explanationLayout.animate().setInterpolator(AccelerateDecelerateInterpolator())

        presenceRef = FirebaseDatabase.getInstance().getReference("users/$studentId/presence")
        presenceRef?.child("status")?.setValue("in_lecture")
        presenceRef?.child("lastSeen")?.setValue(System.currentTimeMillis())

        if (questionModelList.isEmpty()) {
            Toast.makeText(this, "No questions available!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // ✅ Randomize question order only on first attempt
        questionModelList.shuffle()

        originalTotalQuestions = questionModelList.size

        // ✅ Randomize options only for the first attempt
        questionModelList.forEachIndexed { index, q ->
            if (q.options.isNotEmpty()) {
                shuffledOptionsMap[index] = q.options.shuffled()
            }
        }

        // Firebase check
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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmation()
            }
        })
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
        nextBtn.visibility = View.GONE
        explanationText.text = explanation
        explanationLayout.visibility = View.VISIBLE

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
                            currentQuestionIndex++
                            while (currentQuestionIndex < questionModelList.size &&
                                correctlyAnswered.contains(currentQuestionIndex)
                            ) {
                                currentQuestionIndex++
                            }
                            loadQuestions()

                            listOf(btn0, btn1, btn2, btn3).forEach { it.isEnabled = true }
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
        db.child("users").child(studentId).child("progress").child(quizId).child(partId)
            .updateChildren(progress)
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
        val percentage = ((correctAnswers.toFloat() / originalTotalQuestions.toFloat()) * 100).toInt()

        progressData["isCompleted"] = true
        progressData["lastUpdated"] = System.currentTimeMillis()
        saveProgressToFirebase(quizId, partId, progressData)
        updateQuizCompletionStatus(studentId, quizId)
        FirebaseDatabase.getInstance().getReference("users/$studentId/pretestCompleted").setValue(true)

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
