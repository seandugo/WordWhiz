package com.example.thesis_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.ClassRequest
import com.google.firebase.database.FirebaseDatabase

class RequestAdapter(
    private val requests: MutableList<ClassRequest>,
    private val onApprove: (ClassRequest) -> Unit,
    private val onReject: (ClassRequest) -> Unit
) : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val studentName: TextView = itemView.findViewById(R.id.requestStudentName)
        val classText: TextView = itemView.findViewById(R.id.requestClassCode)
        val btnApprove: Button = itemView.findViewById(R.id.approveBtn)
        val btnReject: Button = itemView.findViewById(R.id.rejectBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_class_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]
        holder.studentName.text = request.studentName

        // 🔹 Default text while loading
        holder.classText.text = "Requested Class: Loading..."

        // 🔹 Fetch the class name from Firebase using the requested class code
        val classCode = request.requestedClassCode ?: ""
        if (classCode.isNotEmpty()) {
            FirebaseDatabase.getInstance().getReference("classes/$classCode/className")
                .get()
                .addOnSuccessListener { snapshot ->
                    val className = snapshot.getValue(String::class.java) ?: classCode
                    holder.classText.text = "Requested Class: $className"
                }
                .addOnFailureListener {
                    holder.classText.text = "Requested Class: $classCode"
                }
        } else {
            holder.classText.text = "Requested Class: N/A"
        }

        // ✅ Approve / Reject actions
        holder.btnApprove.setOnClickListener { onApprove(request) }
        holder.btnReject.setOnClickListener { onReject(request) }
    }

    override fun getItemCount(): Int = requests.size
}
