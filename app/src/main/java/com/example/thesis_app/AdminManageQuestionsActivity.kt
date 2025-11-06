package com.example.thesis_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.database.*

data class QuestionLevel(
    val id: String = "",
    val levelName: String = "",
    val questionCount: Int = 0
)

class AdminManageQuestionsActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var levelRecycler: RecyclerView
    private lateinit var adapter: AdminQuestionLevelAdapter
    private val levels = mutableListOf<QuestionLevel>()

    private var lectureId: String? = null
    private var lectureTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_manage_questions)

        // 🎓 Get lecture info
        lectureId = intent.getStringExtra("lectureId")
        lectureTitle = intent.getStringExtra("lectureTitle")

        database = FirebaseDatabase.getInstance().reference

        // 🧭 Toolbar setup
        val toolbar = findViewById<MaterialToolbar>(R.id.manageQuestionsToolbar)
        toolbar.title = "Manage Questions"
        toolbar.subtitle = lectureTitle
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor))
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.black))
        toolbar.setNavigationOnClickListener { finish() }

        // ♻️ Recycler setup
        levelRecycler = findViewById(R.id.levelRecyclerView)
        levelRecycler.layoutManager = LinearLayoutManager(this)
        adapter = AdminQuestionLevelAdapter(levels) { selectedLevel ->
            openQuestionEditor(selectedLevel)
        }
        levelRecycler.adapter = adapter

        loadLevels()
    }

    private fun loadLevels() {
        levels.clear()

        // 🧩 If this is lecture 835247 — only show Level 1
        val baseLevels = if (lectureId == "835247") {
            listOf("Level 1")
        } else {
            listOf("Level 1", "Level 2", "Level 3", "Post-Test")
        }

        baseLevels.forEach { levelName ->
            val partId = when (levelName.lowercase()) {
                "level 1" -> "part1"
                "level 2" -> "part2"
                "level 3" -> "part3"
                "post-test" -> "post-test"
                else -> levelName.lowercase()
            }

            database.child("quizzes").child(lectureId ?: return)
                .child(partId).child("questionList")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val count = snapshot.childrenCount.toInt()
                        levels.add(QuestionLevel(partId, levelName, count))
                        adapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@AdminManageQuestionsActivity,
                            "Error loading $levelName: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    private fun openQuestionEditor(level: QuestionLevel) {
        val intent = Intent(this, AdminQuestionEditorActivity::class.java)
        intent.putExtra("lectureId", lectureId)
        intent.putExtra("levelId", level.id)
        intent.putExtra("levelName", level.levelName)
        startActivity(intent)
    }
}
