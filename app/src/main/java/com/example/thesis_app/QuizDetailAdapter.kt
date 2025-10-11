package com.example.thesis_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.graphics.findFirstRoot
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.MultiSegmentProgressView
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
        val correctCount: TextView = itemView.findViewById(R.id.correctCount)
        val wrongCount: TextView = itemView.findViewById(R.id.wrongCount)
        val retriedCount: TextView = itemView.findViewById(R.id.retriedCount)
        val retriedLabel: TextView = itemView.findViewById(R.id.retriedLabel) // new
        val reviewButton: MaterialButton = itemView.findViewById(R.id.reviewButton)
        val retakeButton: MaterialButton = itemView.findViewById(R.id.retakeButton)
        val multiProgress : MultiSegmentProgressView = itemView.findViewById(R.id.accuracyPercentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizDetailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.progress_recycler, parent, false)
        return QuizDetailViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuizDetailViewHolder, position: Int) {
        val item = items[position]

        holder.title.text = item.levelName

        val total = item.correctParts + item.wrongParts + item.retryParts

        holder.correctCount.text = item.correctParts.toString()
        holder.wrongCount.text = item.wrongParts.toString()
        holder.retriedCount.text = item.retryParts.toString()

        holder.multiProgress.correctPercent = if (total == 0) 0f else item.correctParts * 100f / total
        holder.multiProgress.wrongPercent = if (total == 0) 0f else item.wrongParts * 100f / total
        holder.multiProgress.retryPercent = if (total == 0) 0f else item.retryParts * 100f / total
        holder.multiProgress.invalidate()

        // Hide retried info if this is a post-test
        if (item.levelName.equals("post-test", ignoreCase = true)) {
            holder.retriedCount.visibility = View.GONE
            holder.retriedLabel.visibility = View.GONE
        } else {
            holder.retriedCount.visibility = View.VISIBLE
            holder.retriedLabel.visibility = View.VISIBLE
        }

        // ✅ Lock buttons if part is not completed
        if (!item.isCompleted) {
            holder.reviewButton.isEnabled = false
            holder.retakeButton.isEnabled = false
            holder.reviewButton.alpha = 0.5f // visually show as disabled
            holder.retakeButton.alpha = 0.5f
        } else {
            holder.reviewButton.isEnabled = true
            holder.retakeButton.isEnabled = true
            holder.reviewButton.alpha = 1f
            holder.retakeButton.alpha = 1f
        }

        holder.reviewButton.setOnClickListener {
            if (item.isCompleted) onReviewClick(item)
        }
        holder.retakeButton.setOnClickListener {
            if (item.isCompleted) onRetakeClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
