package com.example.thesis_app

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.QuizModel
import com.google.firebase.database.FirebaseDatabase

class QuizListAdapter(
    private val quizModelList: List<QuizModel>,
    private val studentId: String,
    private val activity: FragmentActivity
) : RecyclerView.Adapter<QuizListAdapter.MyViewHolder>() {

    class MyViewHolder(itemView: View, private val studentId: String, private val activity: FragmentActivity) : RecyclerView.ViewHolder(itemView) {
        private val quizTitleText: TextView = itemView.findViewById(R.id.quiz_title_text)
        private val quizSubtitleText: TextView = itemView.findViewById(R.id.quiz_subtitle_text)
        private val progressBar: com.google.android.material.progressindicator.CircularProgressIndicator =
            itemView.findViewById(R.id.quiz_progress_bar)
        private val percentageText: TextView = itemView.findViewById(R.id.quiz_percentage_text)

        fun bind(model: QuizModel) {
            quizTitleText.text = model.title
            quizSubtitleText.text = model.subtitle

            loadUserProgress(model.id) { answeredCount ->
                val total = model.questionList.size
                val progress = if (total > 0) (answeredCount * 100) / total else 0

                progressBar.setProgress(progress, true)
                percentageText.text = "$progress%"
            }

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, QuizActivity::class.java)
                QuizActivity.questionModelList = model.questionList
                intent.putExtra("QUIZ_ID", model.id)
                intent.putExtra("STUDENT_ID", studentId)

                // Start QuizActivity
                itemView.context.startActivity(intent)

                // Finish the parent activity so it reloads later
                activity.finish()
            }

        }

        private fun loadUserProgress(quizId: String, callback: (Int) -> Unit) {
            val db = FirebaseDatabase.getInstance().reference

            db.child("users")
                .child(studentId)  // ✅ now works, studentId is available
                .child("progress")
                .child(quizId)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val answered = snapshot.child("answeredCount").getValue(Int::class.java) ?: 0
                        callback(answered)
                    } else {
                        callback(0)
                    }
                }
                .addOnFailureListener {
                    callback(0)
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.quiz_item_recycler_row, parent, false)
        return MyViewHolder(view, studentId, activity) // ✅ pass studentId here
    }

    override fun getItemCount(): Int = quizModelList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(quizModelList[position])
    }
}
