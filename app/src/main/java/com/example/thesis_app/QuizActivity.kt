package com.example.thesis_app

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.thesis_app.models.QuestionModel

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
    private lateinit var timerIndicatorTextview: TextView
    private lateinit var questionIndicatorTextview: TextView
    private lateinit var questionProgressIndicator: ProgressBar
    private lateinit var questionTextview: TextView

    private var currentQuestionIndex = 0
    private var selectedAnswer = ""
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
        timerIndicatorTextview = findViewById(R.id.timer_indicator_textview)
        questionIndicatorTextview = findViewById(R.id.question_indicator_textview)
        questionProgressIndicator = findViewById(R.id.question_progress_indicator)
        questionTextview = findViewById(R.id.question_textview)

        // Set click listeners
        btn0.setOnClickListener(this)
        btn1.setOnClickListener(this)
        btn2.setOnClickListener(this)
        btn3.setOnClickListener(this)
        nextBtn.setOnClickListener(this)

        loadQuestions()
        startTimer()
    }

    private fun startTimer() {
        val totalTimeInMillis = time.toInt() * 60 * 1000L
        object : CountDownTimer(totalTimeInMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                timerIndicatorTextview.text =
                    String.format("%02d:%02d", minutes, remainingSeconds)
            }

            override fun onFinish() {
                finishQuiz()
            }
        }.start()
    }

    private fun loadQuestions() {
        selectedAnswer = ""
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
        // Reset button colors
        btn0.setBackgroundColor(getColor(R.color.gray))
        btn1.setBackgroundColor(getColor(R.color.gray))
        btn2.setBackgroundColor(getColor(R.color.gray))
        btn3.setBackgroundColor(getColor(R.color.gray))

        val clickedBtn = view as Button
        if (clickedBtn.id == R.id.next_btn) {
            // next button is clicked
            if (selectedAnswer.isEmpty()) {
                Toast.makeText(
                    applicationContext,
                    "Please select answer to continue",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            if (selectedAnswer == questionModelList[currentQuestionIndex].correct) {
                score++
                Log.i("Score of quiz", score.toString())
            }
            currentQuestionIndex++
            loadQuestions()
        } else {
            // options button is clicked
            selectedAnswer = clickedBtn.text.toString()
            clickedBtn.setBackgroundColor(getColor(R.color.primaryColor))
        }
    }

    private fun finishQuiz() {
        val totalQuestions = questionModelList.size
        val percentage = ((score.toFloat() / totalQuestions.toFloat()) * 100).toInt()

        val dialogView = layoutInflater.inflate(R.layout.score_dialog, null)
        val scoreProgressIndicator: ProgressBar = dialogView.findViewById(R.id.score_progress_indicator)
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
            finish()
        }

        dialog.show()
    }
}