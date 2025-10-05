package com.example.thesis_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.ProgressItem

class ProgressListAdapter(
    private val onItemClick: (ProgressItem.Part) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ProgressItem>()

    companion object {
        private const val TYPE_DIVIDER = 0
        private const val TYPE_PART = 1
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is ProgressItem.Divider -> TYPE_DIVIDER
        is ProgressItem.Part -> TYPE_PART
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DIVIDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_quiz_divider, parent, false)
                DividerViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_quiz_progress, parent, false)
                PartViewHolder(view, onItemClick)
            }
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ProgressItem.Divider -> (holder as DividerViewHolder).bind(item.title)
            is ProgressItem.Part -> (holder as PartViewHolder).bind(item)
        }
    }

    class DividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dividerTitle: TextView = itemView.findViewById(R.id.dividerTitle)
        fun bind(title: String) {
            dividerTitle.text = title
        }
    }

    class PartViewHolder(
        itemView: View,
        private val onItemClick: (ProgressItem.Part) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val partTitle: TextView = itemView.findViewById(R.id.quizTitle)
        fun bind(item: ProgressItem.Part) {
            partTitle.text = item.levelName
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    fun updateData(newItems: List<ProgressItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
