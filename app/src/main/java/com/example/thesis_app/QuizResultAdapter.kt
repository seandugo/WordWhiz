package com.example.thesis_app.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.R
import com.example.thesis_app.models.QuizAnswer

class QuizResultAdapter(
    private val context: Context,
    private val quizAnswers: List<QuizAnswer>
) : RecyclerView.Adapter<QuizResultAdapter.QuizResultViewHolder>() {

    inner class QuizResultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val questionText: TextView = view.findViewById(R.id.question_textview)
        val answerIndicator: TextView = view.findViewById(R.id.answer_indicator)
        val explanationText: TextView = view.findViewById(R.id.explanation_text)
        val selectedAnswer: TextView = view.findViewById(R.id.selected_answer)
        val btn0: Button = view.findViewById(R.id.btn0)
        val btn1: Button = view.findViewById(R.id.btn1)
        val btn2: Button = view.findViewById(R.id.btn2)
        val btn3: Button = view.findViewById(R.id.btn3)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizResultViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.answers_recycler, parent, false)
        return QuizResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuizResultViewHolder, position: Int) {
        val answer = quizAnswers[position]

        // 🧠 Display question
        holder.questionText.text = "Q${answer.order}. ${answer.question}"

        // 🧠 Explanation text
        holder.explanationText.text = answer.explanation

        // 🧠 Selected answer
        holder.selectedAnswer.text = "${answer.selectedAnswer}"

        // 🧠 Display multiple-choice options
        val optionButtons = listOf(holder.btn0, holder.btn1, holder.btn2, holder.btn3)

        // Reset button styles
        optionButtons.forEach {
            it.visibility = View.VISIBLE
            it.background = ContextCompat.getDrawable(context, R.drawable.rounded_button)
            it.setTextColor(ContextCompat.getColor(context, R.color.black))
        }

        // Set options if available
        val options = answer.options ?: emptyList()
        optionButtons.forEachIndexed { index, button ->
            button.text = options.getOrNull(index) ?: ""
        }

        // Determine correct answer text
        val correctIndex = answer.correctAnswer.toIntOrNull()
        val correctText = if (correctIndex != null && correctIndex in options.indices)
            options[correctIndex]
        else
            answer.correctAnswer

        // 🟩 / 🟥 Apply highlighting
        if (answer.isCorrect) {
            // ✅ Correct
            holder.answerIndicator.text = "Correct"
            holder.answerIndicator.background = ContextCompat.getDrawable(context, R.drawable.correct)
            holder.selectedAnswer.background = ContextCompat.getDrawable(context, R.drawable.correct)
            holder.selectedAnswer.setTextColor(ContextCompat.getColor(context, android.R.color.white))

            // Highlight correct option
            optionButtons.forEach {
                if (it.text.toString().equals(correctText, true)) {
                    it.background = ContextCompat.getDrawable(context, R.drawable.correct)
                    it.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                }
            }
        } else {
            // ❌ Wrong
            holder.answerIndicator.text = "Wrong"
            holder.answerIndicator.background = ContextCompat.getDrawable(context, R.drawable.wrong)
            holder.selectedAnswer.background = ContextCompat.getDrawable(context, R.drawable.wrong)
            holder.selectedAnswer.setTextColor(ContextCompat.getColor(context, android.R.color.black))

            // Highlight correct & selected
            optionButtons.forEach {
                when (it.text.toString()) {
                    correctText -> {
                        it.background = ContextCompat.getDrawable(context, R.drawable.correct)
                        it.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                    }
                    answer.selectedAnswer -> {
                        it.background = ContextCompat.getDrawable(context, R.drawable.wrong)
                        it.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = quizAnswers.size
}
