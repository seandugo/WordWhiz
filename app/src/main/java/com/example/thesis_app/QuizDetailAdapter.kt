package com.example.thesis_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.QuizDetailProgress
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.button.MaterialButton

class QuizDetailAdapter(
    private val items: List<QuizDetailProgress>,
    private val onReviewClick: (QuizDetailProgress) -> Unit,
    private val onRetakeClick: (QuizDetailProgress) -> Unit
) : RecyclerView.Adapter<QuizDetailAdapter.QuizDetailViewHolder>() {

    class QuizDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.dividerTitle)
        val accuracyText: TextView = itemView.findViewById(R.id.progressText)
        val circularProgress: CircularProgressIndicator = itemView.findViewById(R.id.circularProgress)
        val correctCount: TextView = itemView.findViewById(R.id.correctCount)
        val wrongCount: TextView = itemView.findViewById(R.id.wrongCount)
        val retriedCount: TextView = itemView.findViewById(R.id.retriedCount)
        val retriedLabel: TextView = itemView.findViewById(R.id.retriedLabel) // new
        val reviewButton: MaterialButton = itemView.findViewById(R.id.reviewButton)
        val retakeButton: MaterialButton = itemView.findViewById(R.id.retakeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizDetailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.progress_recycler, parent, false)
        return QuizDetailViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuizDetailViewHolder, position: Int) {
        val item = items[position]

        holder.title.text = item.levelName

        // Calculate percentage
        val percentage = if (item.totalParts == 0) 0 else (item.correctParts * 100 / item.totalParts)
        holder.circularProgress.progress = percentage
        holder.accuracyText.text = "$percentage%"

        holder.correctCount.text = item.correctParts.toString()
        holder.wrongCount.text = item.wrongParts.toString()
        holder.retriedCount.text = item.retryParts.toString()

        // Hide retried info if this is a post-test
        if (item.levelName.equals("post-test", ignoreCase = true)) {
            holder.retriedCount.visibility = View.GONE
            holder.retriedLabel.visibility = View.GONE
        } else {
            holder.retriedCount.visibility = View.VISIBLE
            holder.retriedLabel.visibility = View.VISIBLE
        }

        holder.reviewButton.setOnClickListener { onReviewClick(item) }
        holder.retakeButton.setOnClickListener { onRetakeClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
