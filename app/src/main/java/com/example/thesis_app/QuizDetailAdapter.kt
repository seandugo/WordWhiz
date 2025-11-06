package com.example.thesis_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.MultiSegmentProgressView
import com.example.thesis_app.models.QuizDetailProgress
import android.content.Intent

class QuizDetailAdapter(
    private val items: List<QuizDetailProgress>,
    private val onReviewClick: (QuizDetailProgress) -> Unit,
    private val onSeeAnswersClick: (QuizDetailProgress) -> Unit // ✅ Only two callbacks now
) : RecyclerView.Adapter<QuizDetailAdapter.QuizDetailViewHolder>() {

    class QuizDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.dividerTitle)
        val correctCount: TextView = itemView.findViewById(R.id.correctCount)
        val wrongCount: TextView = itemView.findViewById(R.id.wrongCount)
        val retriedCount: TextView = itemView.findViewById(R.id.retriedCount)
        val retriedLabel: TextView = itemView.findViewById(R.id.retriedLabel)
        val multiProgress: MultiSegmentProgressView = itemView.findViewById(R.id.accuracyPercentage)
        val seeAnswersButton: Button = itemView.findViewById(R.id.seeAnswerDetails)
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

        // Hide retry section if post-test
        if (item.levelName.equals("post-test", ignoreCase = true)) {
            holder.retriedCount.visibility = View.GONE
            holder.retriedLabel.visibility = View.GONE
        } else {
            holder.retriedCount.visibility = View.VISIBLE
            holder.retriedLabel.visibility = View.VISIBLE
        }

        // Disable card if not completed
        if (!item.isCompleted) {
            holder.itemView.isEnabled = false
            holder.itemView.alpha = 0.5f
            holder.seeAnswersButton.isEnabled = false
        } else {
            holder.itemView.isEnabled = true
            holder.itemView.alpha = 1f
            holder.seeAnswersButton.isEnabled = true

            // 🔹 "See Answer Details" button launches AnswerDetailsActivity
            holder.seeAnswersButton.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, AnswerDetailsActivity::class.java)

                // ✅ Pass topic name, quiz ID, student ID, and correct part
                val parentActivity = context as? QuizDetailActivity
                val quizTitle = parentActivity?.intent?.getStringExtra("levelName") ?: "Unknown Quiz"
                val quizId = parentActivity?.intent?.getStringExtra("quizId") ?: ""
                val studentId = parentActivity?.intent?.getStringExtra("studentId") ?: ""

                intent.putExtra("levelName", quizTitle)
                intent.putExtra("quizId", quizId)
                intent.putExtra("studentId", studentId)
                intent.putExtra("partId", mapLevelNameToPartId(item.levelName))

                context.startActivity(intent)
            }

            // Optional: clicking the whole card still does review
            holder.itemView.setOnClickListener {
                onReviewClick(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // ✅ Helper function to map readable names → Firebase part keys
    private fun mapLevelNameToPartId(levelName: String): String {
        return when (levelName.lowercase()) {
            "level 1" -> "part1"
            "level 2" -> "part2"
            "level 3" -> "part3"
            "post-test" -> "post-test"
            else -> levelName.lowercase()
        }
    }
}
