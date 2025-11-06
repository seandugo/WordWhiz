package com.example.thesis_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdminQuestionAdapter(
    private val questions: List<AdminQuestion>,
    private val onDelete: (AdminQuestion) -> Unit,
    private val onEdit: (AdminQuestion) -> Unit
) : RecyclerView.Adapter<AdminQuestionAdapter.QuestionViewHolder>() {

    inner class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val questionText: TextView = itemView.findViewById(R.id.questionText)
        val correctText: TextView = itemView.findViewById(R.id.correctAnswer)
        val explanationText: TextView = itemView.findViewById(R.id.explanationText)
        val editBtn: ImageButton = itemView.findViewById(R.id.editQuestionBtn)
        val deleteBtn: ImageButton = itemView.findViewById(R.id.deleteQuestionBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val question = questions[position]

        holder.questionText.text = "Q${position + 1}: ${question.question}"
        holder.correctText.text = "✅ Answer: ${question.correct}"
        holder.explanationText.text =
            if (question.explanation.isNotEmpty()) "💬 ${question.explanation}" else "💬 No explanation provided"

        holder.editBtn.setOnClickListener { onEdit(question) }
        holder.deleteBtn.setOnClickListener { onDelete(question) }
    }

    override fun getItemCount(): Int = questions.size
}