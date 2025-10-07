package com.example.thesis_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.ProgressItem
import com.google.android.material.progressindicator.LinearProgressIndicator

class ProgressListAdapter(
    private val onItemClick: (ProgressItem.Part) -> Unit
) : RecyclerView.Adapter<ProgressListAdapter.PartViewHolder>() {

    private val items = mutableListOf<ProgressItem.Part>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quiz_progress, parent, false)
        return PartViewHolder(view, onItemClick)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: PartViewHolder, position: Int) {
        // Pass previous item's isCompleted to determine if current should be locked
        val previousCompleted = if (position == 0) true else items[position - 1].isCompleted
        holder.bind(items[position], previousCompleted)
    }

    class PartViewHolder(
        itemView: View,
        private val onItemClick: (ProgressItem.Part) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val partTitle: TextView = itemView.findViewById(R.id.quizTitle)
        private val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.progressBar)
        private val progressText: TextView = itemView.findViewById(R.id.progressText)

        fun bind(item: ProgressItem.Part, previousCompleted: Boolean) {
            partTitle.text = item.levelName

            val percentage = if (item.totalParts == 0) 0
            else (item.completedParts * 100 / item.totalParts)

            progressBar.progress = percentage

            // Determine if current quiz is unlocked
            val isUnlocked = previousCompleted

            if (!isUnlocked) {
                itemView.alpha = 0.5f
                itemView.isClickable = false
                progressText.text = "Locked"
            } else {
                itemView.alpha = 1f
                itemView.isClickable = true
                progressText.text = "$percentage% completed"

                // Only allow click if unlocked
                itemView.setOnClickListener { onItemClick(item) }
            }
        }
    }

    fun updateData(newItems: List<ProgressItem.Part>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}