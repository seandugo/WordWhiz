package com.example.thesis_app

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.ClassItem
import com.example.thesis_app.utils.NetworkUtils
import com.google.android.material.snackbar.Snackbar

class ClassAdapter(
    private val classList: MutableList<ClassItem>,
    private val onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null,
    private val onItemClick: (ClassItem) -> Unit,
    private val onEditClick: (ClassItem) -> Unit,
    private val onDeleteClick: (ClassItem) -> Unit,
    private val onArchiveClick: (ClassItem) -> Unit
) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    inner class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val className: TextView = itemView.findViewById(R.id.classNameText)
        val roomNumber: TextView = itemView.findViewById(R.id.roomNumberText)
        val menuButton: ImageView = itemView.findViewById(R.id.menuButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.class_card_item, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val classItem = classList[position]
        val context = holder.itemView.context

        holder.className.text = classItem.className
        holder.roomNumber.text = classItem.roomNo

        // ✅ Click listener with internet check
        holder.itemView.setOnClickListener {
            if (!NetworkUtils.isInternetAvailable(context)) {
                Snackbar.make(holder.itemView, "No internet connection. Try again later.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Only call the callback — don't start activity here
            onItemClick(classItem)
        }


        // ✅ Drag support
        holder.itemView.setOnLongClickListener {
            onStartDrag?.invoke(holder)
            true
        }

        // ✅ Popup menu for edit/delete/archive
        holder.menuButton.setOnClickListener { v ->
            val popup = androidx.appcompat.widget.PopupMenu(v.context, v)
            popup.inflate(R.menu.class_item_menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        onEditClick(classItem)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick(classItem)
                        true
                    }
                    R.id.action_archive -> {
                        onArchiveClick(classItem)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int = classList.size

    // ✅ Helper functions for item management
    fun swapItems(fromPos: Int, toPos: Int) {
        val temp = classList[fromPos]
        classList[fromPos] = classList[toPos]
        classList[toPos] = temp
        notifyItemMoved(fromPos, toPos)
    }

    fun archiveItem(classItem: ClassItem) {
        val index = classList.indexOfFirst { it.classCode == classItem.classCode }
        if (index != -1) {
            classList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun addItemAtTop(newClass: ClassItem) {
        classList.add(0, newClass)
        notifyItemInserted(0)
    }

    fun updateItem(updatedClass: ClassItem) {
        val index = classList.indexOfFirst { it.classCode == updatedClass.classCode }
        if (index != -1) {
            classList[index] = updatedClass
            notifyItemChanged(index)
        }
    }

    fun removeItem(classItem: ClassItem) {
        val index = classList.indexOfFirst { it.classCode == classItem.classCode }
        if (index != -1) {
            classList.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
