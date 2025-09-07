package com.example.thesis_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.StudentItem

class StudentAdapter(
    private val studentList: MutableList<StudentItem>,
    private val onItemClick: (StudentItem) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.studentName)
        val id: TextView = itemView.findViewById(R.id.studentId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.student_card_item, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = studentList[position]
        holder.name.text = student.name
        holder.id.text = student.studentId

        holder.itemView.setOnClickListener {
            onItemClick(student)
        }
    }

    override fun getItemCount(): Int = studentList.size
    fun addItemAtTop(student: StudentItem) {
        studentList.add(0, student)
        notifyItemInserted(0)
    }
}