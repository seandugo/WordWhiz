package com.example.thesis_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.ClassItem   // ✅ import here

class ClassAdapter(
    private val classList: MutableList<ClassItem>,
    private val onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null,
    private val onItemClick: (ClassItem) -> Unit
) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    inner class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val className: TextView = itemView.findViewById(R.id.classNameText)
        val roomNumber: TextView = itemView.findViewById(R.id.roomNumberText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.class_card_item, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val classItem = classList[position]
        holder.className.text = classItem.className
        holder.roomNumber.text = classItem.roomNo

        holder.itemView.setOnClickListener {
            onItemClick(classItem)
        }

        // Optional: drag handle if you have one
        holder.itemView.setOnLongClickListener {
            onStartDrag?.invoke(holder)
            true
        }
    }

    override fun getItemCount(): Int = classList.size

    fun swapItems(fromPos: Int, toPos: Int) {
        val temp = classList[fromPos]
        classList[fromPos] = classList[toPos]
        classList[toPos] = temp
        notifyItemMoved(fromPos, toPos)
    }

    fun addItemAtTop(newClass: ClassItem) {
        classList.add(0, newClass)
        notifyItemInserted(0)
    }
}