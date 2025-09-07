package com.example.thesis_app

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections

class ClassAdapter(
    private val classList: MutableList<ClassItem>,
    private val startDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    inner class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.classTitle)
        val subtitle: TextView = itemView.findViewById(R.id.roomNo)
        val dragHandle: TextView = itemView.findViewById(R.id.dragHandle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.class_card_item, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val item = classList[position]
        holder.title.text = item.className
        holder.subtitle.text = item.roomNo

        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                startDrag(holder)
            }
            false
        }
    }

    override fun getItemCount(): Int = classList.size

    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(classList, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    // Insert new item at the top
    fun addItemAtTop(item: ClassItem) {
        classList.add(0, item)
        notifyItemInserted(0)
    }
}