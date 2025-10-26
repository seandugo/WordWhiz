package com.example.thesis_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.ArchivedClass
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

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
        val deleteTime = archived.archivedAt + archived.deleteAfter
        val remaining = deleteTime - System.currentTimeMillis()

        if (remaining > 0) {
            val days = remaining / (1000 * 60 * 60 * 24)
            val hours = (remaining / (1000 * 60 * 60)) % 24
            holder.countdownText.text = "Deleting in: ${days}d ${hours}h"
        } else {
            holder.countdownText.text = "Pending deletion"
        }

        holder.menuButton.setOnClickListener { v ->
            val popup = androidx.appcompat.widget.PopupMenu(v.context, v)
            popup.inflate(R.menu.archive_menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_restore -> {
                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                        if (userId == null) {
                            Snackbar.make(v, "User not logged in", Snackbar.LENGTH_SHORT).show()
                            return@setOnMenuItemClickListener true
                        }

                        val database = FirebaseDatabase.getInstance().reference
                        val classId = archived.classId ?: archived.className // fallback

                        val archivedRef = database.child("users").child(userId).child("archived_classes").child(classId)
                        val globalRef = database.child("classes").child(classId)
                        val userClassRef = database.child("users").child(userId).child("classes").child(classId)

                        // Step 1️⃣: Fetch archived class data
                        archivedRef.get().addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                val classData = snapshot.child("classData").value

                                if (classData == null) {
                                    Snackbar.make(v, "No class data found.", Snackbar.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }

                                // Step 2️⃣: Restore to global classes node
                                globalRef.setValue(classData)
                                    .addOnSuccessListener {
                                        // Step 3️⃣: Restore reference in teacher's active classes
                                        userClassRef.setValue(classData)

                                        // Step 4️⃣: Remove from archive
                                        archivedRef.removeValue()

                                        Snackbar.make(v, "✅ Class restored successfully!", Snackbar.LENGTH_LONG)
                                            .setAction("OK") { }
                                            .show()
                                    }
                                    .addOnFailureListener {
                                        Snackbar.make(v, "❌ Failed to restore class.", Snackbar.LENGTH_LONG).show()
                                    }
                            } else {
                                Snackbar.make(v, "Archived class not found.", Snackbar.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener {
                            Snackbar.make(v, "Error fetching archived data.", Snackbar.LENGTH_SHORT).show()
                        }

                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int = archivedList.size
}
