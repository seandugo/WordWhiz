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
import com.google.firebase.database.FirebaseDatabase

class QuizListAdapter(
    private val items: List<QuizDisplayItem>,
    private val studentId: String,
    private val activity: FragmentActivity
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_PART = 0
        private const val TYPE_DIVIDER = 1
    }

    private val externalProgressCache = mutableMapOf<String, Pair<Int, Boolean>>()

    fun setPreloadedProgress(cache: Map<String, Pair<Int, Boolean>>) {
        externalProgressCache.clear()
        externalProgressCache.putAll(cache)
        PartViewHolder.setExternalCache(externalProgressCache)
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
            is QuizDisplayItem.Part -> (holder as PartViewHolder).bind(item.item, position)
            is QuizDisplayItem.Divider -> (holder as DividerViewHolder).bind(item.title)
        }
    }

    class PartViewHolder(
        itemView: View,
        private val studentId: String,
        private val activity: FragmentActivity
    ) : RecyclerView.ViewHolder(itemView) {

        companion object {
            private var externalCache: Map<String, Pair<Int, Boolean>>? = null
            private val partStatusCache = mutableMapOf<String, Pair<Int, Boolean>>()

            fun setExternalCache(cache: Map<String, Pair<Int, Boolean>>) {
                externalCache = cache
                partStatusCache.putAll(cache)
            }
        }

        private val quizTitleText: TextView = itemView.findViewById(R.id.quiz_title_text)
        private val progressBar: com.google.android.material.progressindicator.CircularProgressIndicator =
            itemView.findViewById(R.id.quiz_progress_bar)
        private val percentageText: TextView = itemView.findViewById(R.id.quiz_percentage_text)

        fun bind(item: QuizPartItem, index: Int) {
            quizTitleText.text = item.displayName
            val partKey = "${item.quizId}_${item.partId}"

            // ✅ If cached, instantly update UI without re-fetching from Firebase
            if (partStatusCache.containsKey(partKey)) {
                val (answeredCount, isUnlocked) = partStatusCache[partKey]!!
                updateUI(item, answeredCount, isUnlocked, animate = false)
            } else {
                // 🕓 Fetch only once from Firebase if not cached
                checkPartUnlockStatus(item.quizId, item.partId, item.questions.size) { answeredCount, isUnlocked ->
                    partStatusCache[partKey] = Pair(answeredCount, isUnlocked)
                    updateUI(item, answeredCount, isUnlocked, animate = true)
                }
            }
        }

        private fun updateUI(
            item: QuizPartItem,
            answeredCount: Int,
            isUnlocked: Boolean,
            animate: Boolean
        ) {
            if (!isUnlocked) {
                itemView.alpha = 0.5f
                itemView.isClickable = false
                progressBar.visibility = View.INVISIBLE
                percentageText.text = "Locked"
            } else {
                itemView.alpha = 1f
                itemView.isClickable = true
                progressBar.visibility = View.VISIBLE

                val progress = if (item.questions.isNotEmpty())
                    (answeredCount * 100) / item.questions.size
                else 0

                // Disable animation if data is from cache
                progressBar.setProgress(progress, animate)
                percentageText.text = "$progress%"
            }

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

        private fun checkPartUnlockStatus(
            quizId: String,
            partId: String,
            totalQuestions: Int,
            callback: (answeredCount: Int, isUnlocked: Boolean) -> Unit
        ) {
            val db = FirebaseDatabase.getInstance().reference
            val userProgressRef = db.child("users").child(studentId).child("progress")

            userProgressRef.get().addOnSuccessListener { snapshot ->

                val quizKeys = snapshot.children.map { it.key ?: "" }.sorted()
                val quizIndex = quizKeys.indexOf(quizId)
                val previousQuizCompleted = if (quizIndex == 0) true
                else snapshot.child(quizKeys[quizIndex - 1]).child("isCompleted")
                    .getValue(Boolean::class.java) ?: false

                val answered = snapshot.child("$quizId/$partId/answeredCount")
                    .getValue(Int::class.java) ?: 0
                snapshot.child("$quizId/$partId/isCompleted")
                    .getValue(Boolean::class.java) ?: false

                val partsList = snapshot.child(quizId).children
                    .filter { it.key?.startsWith("part") == true }
                    .map { it.key!! }.sorted()
                val partIndex = partsList.indexOf(partId)
                val previousPartCompleted = if (partIndex == 0) true
                else snapshot.child("$quizId/${partsList[partIndex - 1]}/isCompleted")
                    .getValue(Boolean::class.java) ?: false

                val isUnlocked = previousQuizCompleted && previousPartCompleted

                val allPartsCompleted = partsList.all { pid ->
                    snapshot.child("$quizId/$pid/isCompleted").getValue(Boolean::class.java) ?: false
                }
                userProgressRef.child(quizId).child("isCompleted").setValue(allPartsCompleted)

                callback(answered, isUnlocked)

            }.addOnFailureListener {
                callback(0, partId == "part1")
            }
        }
    }

    class DividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dividerTitle: TextView = itemView.findViewById(R.id.dividerTitle)
        fun bind(title: String) {
            dividerTitle.text = "$title"
        }
    }
}