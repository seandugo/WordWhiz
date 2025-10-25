package com.example.thesis_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.ArchivedClass

class ArchivedClassAdapter(
    private val archivedList: List<ArchivedClass>
) : RecyclerView.Adapter<ArchivedClassAdapter.ArchivedViewHolder>() {

    inner class ArchivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val classNameText: TextView = itemView.findViewById(R.id.classNameText)
        val roomNumberText: TextView = itemView.findViewById(R.id.roomNumberText)
        val countdownText: TextView = itemView.findViewById(R.id.countdownText)
        val menuButton: ImageView = itemView.findViewById(R.id.menuButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchivedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.archive_class_item, parent, false)
        return ArchivedViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArchivedViewHolder, position: Int) {
        val archived = archivedList[position]
        holder.classNameText.text = archived.className
        holder.roomNumberText.text = "Room: ${archived.roomNumber}"

        // Countdown
        val deleteTime = archived.timestampArchived + archived.deleteAfter
        val remaining = deleteTime - System.currentTimeMillis()

        if (remaining > 0) {
            val days = remaining / (1000 * 60 * 60 * 24)
            val hours = (remaining / (1000 * 60 * 60)) % 24
            holder.countdownText.text = "Deleting in: ${days}d ${hours}h"
        } else {
            holder.countdownText.text = "Pending deletion"
        }
    }

    override fun getItemCount(): Int = archivedList.size
}
