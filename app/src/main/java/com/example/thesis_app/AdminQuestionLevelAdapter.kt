package com.example.thesis_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdminQuestionLevelAdapter(
    private val levels: List<QuestionLevel>,
    private val onItemClick: (QuestionLevel) -> Unit
) : RecyclerView.Adapter<AdminQuestionLevelAdapter.LevelViewHolder>() {

    inner class LevelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val levelTitle: TextView = itemView.findViewById(R.id.levelTitle)
        val questionCount: TextView = itemView.findViewById(R.id.questionCount)

        init {
            itemView.setOnClickListener {
                // ✅ Changed from bindingAdapterPosition → adapterPosition
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(levels[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_question_level, parent, false)
        return LevelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        val level = levels[position]
        holder.levelTitle.text = level.levelName
        holder.questionCount.text = "Questions: ${level.questionCount}"
    }

    override fun getItemCount(): Int = levels.size
}
