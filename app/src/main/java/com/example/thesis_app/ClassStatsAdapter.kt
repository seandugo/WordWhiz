package com.example.thesis_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.BarGraphView

data class ClassStats(
    val className: String,
    val activeCount: Int,
    val inLectureCount: Int,
    val inactiveCount: Int,
    val totalStudents: Int
)

class ClassStatsAdapter(private val classList: List<ClassStats>) :
    RecyclerView.Adapter<ClassStatsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.dividerTitle)
        val barView: BarGraphView = view.findViewById(R.id.accuracyPercentage)
        val active: TextView = view.findViewById(R.id.activeCount)
        val inLecture: TextView = view.findViewById(R.id.inLectureCount) // ✅ Added missing view
        val inactive: TextView = view.findViewById(R.id.inactiveCount)
        val total: TextView = view.findViewById(R.id.studentsTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.class_overview_recycler, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = classList[position]

        holder.title.text = item.className
        holder.active.text = item.activeCount.toString()
        holder.inLecture.text = item.inLectureCount.toString() // ✅ Display inLecture count
        holder.inactive.text = item.inactiveCount.toString()
        holder.total.text = item.totalStudents.toString()

        // ✅ Update the bar view for visualization
        holder.barView.activeCount = item.activeCount.toFloat()
        holder.barView.inLectureCount = item.inLectureCount.toFloat()
        holder.barView.inactiveCount = item.inactiveCount.toFloat()
        holder.barView.totalStudents = item.totalStudents.toFloat()

        holder.barView.invalidate()
    }

    override fun getItemCount() = classList.size
}
