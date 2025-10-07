package com.example.thesis_app

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.QuizDisplayItem
import com.example.thesis_app.models.QuizPartItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class QuizListAdapter(
    private val items: List<QuizDisplayItem>,
    private val studentId: String,
    private val activity: FragmentActivity
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_PART = 0
        private const val TYPE_DIVIDER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is QuizDisplayItem.Part -> TYPE_PART
            is QuizDisplayItem.Divider -> TYPE_DIVIDER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_PART -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.quiz_item_recycler_row, parent, false)
                PartViewHolder(view, studentId, activity)
            }
            TYPE_DIVIDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_quiz_divider, parent, false)
                DividerViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is QuizDisplayItem.Part -> (holder as PartViewHolder).bind(item.item)
            is QuizDisplayItem.Divider -> (holder as DividerViewHolder).bind(item.title)
        }
    }

    class PartViewHolder(
        itemView: View,
        private val studentId: String,
        private val activity: FragmentActivity
    ) : RecyclerView.ViewHolder(itemView) {

        private val quizTitleText: TextView = itemView.findViewById(R.id.quiz_title_text)
        private val percentageText: TextView = itemView.findViewById(R.id.quiz_percentage_text)
        private val checkImage: View = itemView.findViewById(R.id.checkImage)

        fun bind(item: QuizPartItem) {
            quizTitleText.text = item.displayName

            val db = FirebaseDatabase.getInstance().reference
            val userProgressRef = db.child("users").child(studentId).child("progress")

            userProgressRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // ✅ Get quizzes sorted by order
                    val quizList = snapshot.children.mapNotNull { quizSnap ->
                        val quizKey = quizSnap.key ?: return@mapNotNull null
                        val order = quizSnap.child("order").getValue(Int::class.java) ?: 0
                        quizKey to order
                    }.sortedBy { it.second }

                    val quizKeys = quizList.map { it.first }
                    val quizIndex = quizKeys.indexOf(item.quizId)

                    // ✅ Sort parts (part1, part2, ..., post-test)
                    val partsList = snapshot.child(item.quizId).children
                        .filter { it.key?.startsWith("part") == true || it.key == "post-test" }
                        .mapNotNull { it.key }
                        .sortedBy {
                            if (it == "post-test") Int.MAX_VALUE
                            else it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
                        }

                    val partIndex = partsList.indexOf(item.partId)

                    // ✅ Determine if all parts including post-test are completed
                    val allPartsCompleted = partsList.all { key ->
                        snapshot.child("${item.quizId}/$key").child("isCompleted").getValue(Boolean::class.java) ?: false
                    }

                    // ✅ Update quiz-level isCompleted
                    snapshot.ref.child(item.quizId).child("isCompleted").setValue(allPartsCompleted)

                    // ✅ Lock/unlock logic
                    val previousQuizCompleted = if (quizIndex <= 0) true
                    else snapshot.child(quizKeys[quizIndex - 1]).child("isCompleted").getValue(Boolean::class.java) ?: false

                    val previousPartCompleted = if (partIndex <= 0) true
                    else snapshot.child("${item.quizId}/${partsList[partIndex - 1]}").child("isCompleted").getValue(Boolean::class.java) ?: false

                    val isUnlocked = if (item.partId == "post-test") {
                        partsList.filter { it != "post-test" }.all { key ->
                            snapshot.child("${item.quizId}/$key").child("isCompleted").getValue(Boolean::class.java) ?: false
                        }
                    } else previousQuizCompleted && previousPartCompleted

                    val isCompleted = snapshot.child("${item.quizId}/${item.partId}/isCompleted").getValue(Boolean::class.java) ?: false

                    // ✅ UI updates
                    if (!isUnlocked) {
                        itemView.alpha = 0.5f
                        itemView.isClickable = false
                        percentageText.text = "Locked"
                        checkImage.visibility = View.GONE
                    } else {
                        itemView.alpha = 1f
                        itemView.isClickable = true
                        percentageText.text = ""

                        // ✅ Show or hide check icon based on completion
                        checkImage.visibility = if (isCompleted) View.VISIBLE else View.GONE
                    }

                    // ✅ Click listener
                    itemView.setOnClickListener {
                        if (!isUnlocked) return@setOnClickListener

                        val intent = Intent(itemView.context, QuizActivity::class.java)
                        QuizActivity.questionModelList = item.questions
                        intent.putExtra("QUIZ_ID", item.quizId)
                        intent.putExtra("PART_ID", item.partId)
                        intent.putExtra("STUDENT_ID", studentId)
                        itemView.context.startActivity(intent)
                        activity.finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    class DividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dividerTitle: TextView = itemView.findViewById(R.id.dividerTitle)
        fun bind(title: String) {
            dividerTitle.text = title
        }
    }
}
