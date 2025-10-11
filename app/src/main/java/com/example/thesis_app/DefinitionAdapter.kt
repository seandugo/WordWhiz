package com.example.thesis_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.DefinitionModel

class DefinitionAdapter(private val items: List<DefinitionModel>) :
    RecyclerView.Adapter<DefinitionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emoji: TextView = view.findViewById(R.id.tvEmoji)
        val title: TextView = view.findViewById(R.id.tvTitle)
        val definition: TextView = view.findViewById(R.id.tvDefinition)
        val example: TextView = view.findViewById(R.id.tvExample)
        val explanation: TextView = view.findViewById(R.id.tvExplanation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_definition, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.emoji.text = item.emoji
        holder.title.text = "${item.number}. ${item.title}"
        holder.definition.text = item.definition
        holder.example.text = "Example: ${item.example}"
        holder.explanation.text = item.explanation ?: ""
    }

    override fun getItemCount() = items.size
}
