package com.example.thesis_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AchievementsAdapter(
    private val items: List<AchievementItem>,
    private val userCount: Int
) : RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder>() {

    data class AchievementItem(val drawable: Int, val label: String, val requiredCount: Int)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val item = items[position]
        holder.image.setImageResource(item.drawable)
        holder.label.text = item.label

        if (userCount >= item.requiredCount) {
            holder.itemView.alpha = 1f
            holder.lockOverlay.visibility = View.GONE
        } else {
            holder.itemView.alpha = 0.5f
            holder.lockOverlay.visibility = View.VISIBLE
        }
    }


    class AchievementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.achievementIcon)
        val label: TextView = view.findViewById(R.id.achievementTitle)
        val lockOverlay: View = view.findViewById(R.id.lockOverlay)
    }
}


