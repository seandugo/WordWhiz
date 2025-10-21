package com.example.thesis_app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.DefinitionModel
import org.json.JSONObject
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.thesis_app.models.QuestionModel
import com.google.firebase.database.DatabaseError

class LectureReviewActivity : AppCompatActivity() {

    private lateinit var startButton: MaterialButton
    private lateinit var studentId: String
    private lateinit var quizId: String
    private lateinit var partId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lecture_review)

        startButton = findViewById(R.id.startButton)
        studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        quizId = intent.getStringExtra("QUIZ_ID") ?: ""
        partId = intent.getStringExtra("PART_ID") ?: ""
        val quizPart = intent.getStringExtra("quiz_part") ?: ""
        val quizOrder = intent.getStringExtra("quiz_order")?.toIntOrNull() ?: 0

        findViewById<TextView>(R.id.nameTextExpanded).text = quizPart
        findViewById<TextView>(R.id.lecture).text = "Lecture ${quizOrder}"

        // Load definitions normally (your existing code)
        val resourceId = when (quizOrder - 1) {
            0 -> R.raw.analogies
            1 -> R.raw.genre_viewing
            2 -> R.raw.active_passive
            3 -> R.raw.simple_past
            4 -> R.raw.directed_report
            5 -> R.raw.figurative
            6 -> R.raw.informative_essay
            7 -> R.raw.nouns_adverbs
            8 -> R.raw.verbs_adjectives
            9 -> R.raw.comparison_contrast
            10 -> R.raw.cohesive_devices
            11 -> R.raw.modal_verbs
            12 -> R.raw.modal_verbs_2
            13 -> R.raw.comm_style
            14 -> R.raw.text_connections
            15 -> R.raw.understanding_unc_val
            16 -> R.raw.authors_purpose
            17 -> R.raw.analysis_comparison
            else -> R.raw.analogies
        }
        val definitions = parseJSON(loadJSONFromRaw(resourceId))
        findViewById<RecyclerView>(R.id.lectureRecycler).apply {
            layoutManager = LinearLayoutManager(this@LectureReviewActivity)
            adapter = DefinitionAdapter(definitions)
        }

        // ✅ Check completion
        checkAndSetupButton()
    }

    private fun checkAndSetupButton() {
        val dbRef = FirebaseDatabase.getInstance().reference
            .child("users").child(studentId).child("progress")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                // ✅ Get all quizzes sorted by order
                val quizList = snapshot.children.mapNotNull { quizSnap ->
                    val quizKey = quizSnap.key ?: return@mapNotNull null
                    val order = quizSnap.child("order").getValue(Int::class.java) ?: 0
                    quizKey to order
                }.sortedBy { it.second }

                val quizKeys = quizList.map { it.first }
                val quizIndex = quizKeys.indexOf(quizId)

                // ✅ Get all parts of current quiz sorted
                val partsList = snapshot.child(quizId).children
                    .filter { it.key?.startsWith("part") == true || it.key == "post-test" }
                    .mapNotNull { it.key }
                    .sortedBy { if (it == "post-test") Int.MAX_VALUE else it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }

                val partIndex = partsList.indexOf(partId)

                // ✅ Check if current part is completed
                val isCurrentCompleted = snapshot.child("$quizId/$partId/isCompleted").getValue(Boolean::class.java) ?: false

                // ✅ Lock/unlock logic
                val previousQuizCompleted = if (quizIndex <= 0) true
                else snapshot.child(quizKeys[quizIndex - 1]).child("isCompleted").getValue(Boolean::class.java) ?: false

                val previousPartCompleted = if (partIndex <= 0) true
                else snapshot.child("$quizId/${partsList[partIndex - 1]}").child("isCompleted").getValue(Boolean::class.java) ?: false

                val isUnlocked = if (partId == "post-test") {
                    partsList.filter { it != "post-test" }.all { key ->
                        snapshot.child("$quizId/$key").child("isCompleted").getValue(Boolean::class.java) ?: false
                    }
                } else previousQuizCompleted && previousPartCompleted

                // ✅ Update button based on lock/unlock and completion
                when {
                    !isUnlocked -> {
                        startButton.isEnabled = false
                        startButton.text = "Locked 🔒"
                    }
                    isCurrentCompleted -> {
                        startButton.isEnabled = false
                        startButton.text = "Completed ✅"
                    }
                    else -> {
                        startButton.isEnabled = true
                        startButton.text = "Start"

                        startButton.setOnClickListener {
                            startQuiz(partId, snapshot)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /**
     * Starts the quiz for the given part, but only if there are questions.
     */
    private fun startQuiz(partIdToStart: String, snapshot: DataSnapshot) {
        val questionsRef = FirebaseDatabase.getInstance().reference
            .child("quizzes").child(quizId).child(partIdToStart).child("questionList")

        questionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(questionsSnap: DataSnapshot) {
                val questions = questionsSnap.children.mapNotNull { it.getValue(QuestionModel::class.java) }

                if (questions.isEmpty()) {
                    startButton.isEnabled = false
                    startButton.text = "No Questions ❌"
                    return
                }

                QuizActivity.questionModelList = questions

                val intent = Intent(this@LectureReviewActivity, QuizActivity::class.java)
                intent.putExtra("QUIZ_ID", quizId)
                intent.putExtra("PART_ID", partIdToStart)
                intent.putExtra("STUDENT_ID", studentId)
                startActivity(intent)
                finish()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadJSONFromRaw(resourceId: Int): String {
        val inputStream = resources.openRawResource(resourceId)
        return inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseJSON(jsonString: String) = mutableListOf<DefinitionModel>().apply {
        val jsonArray = JSONObject(jsonString).getJSONArray("definitions")
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            add(DefinitionModel(
                number = obj.getInt("number"),
                title = obj.getString("title"),
                emoji = obj.getString("emoji"),
                definition = obj.getString("definition"),
                example = obj.getString("example"),
                explanation = obj.optString("explanation", "")
            ))
        }
    }
}