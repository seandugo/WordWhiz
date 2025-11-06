package com.example.thesis_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdminLectureAdapter(
    private val lectures: List<AdminLecture>,
    private val onItemClick: (AdminLecture) -> Unit // 👈 Callback for click handling
) : RecyclerView.Adapter<AdminLectureAdapter.LectureViewHolder>() {

    inner class LectureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.lectureTitle)
        val description: TextView = itemView.findViewById(R.id.lectureDescription)
        val order: TextView = itemView.findViewById(R.id.lectureOrder)

        init {
            // ✅ Handle clicks directly inside adapter
            itemView.setOnClickListener {
                val position = adapterPosition // 🔧 Changed from bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(lectures[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LectureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_lecture, parent, false)
        return LectureViewHolder(view)
    }

    override fun onBindViewHolder(holder: LectureViewHolder, position: Int) {
        val lecture = lectures[position]
        holder.title.text = lecture.title
        holder.description.text = lecture.description
        holder.order.text = "Order: ${lecture.order}"
    }

    override fun getItemCount(): Int = lectures.size
}
