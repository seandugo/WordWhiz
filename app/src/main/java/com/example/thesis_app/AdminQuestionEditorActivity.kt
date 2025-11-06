package com.example.thesis_app

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*

data class AdminQuestion(
    val id: String = "",
    val question: String = "",
    val correct: String = "",
    val explanation: String = "",
    val instruction: String = "",
    val options: List<String> = listOf()
)

class AdminQuestionEditorActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminQuestionAdapter
    private lateinit var database: DatabaseReference
    private lateinit var addQuestionBtn: MaterialButton
    private lateinit var deployQuizBtn: MaterialButton

    private var lectureId: String? = null
    private var levelId: String? = null
    private var levelName: String? = null

    private val questions = mutableListOf<AdminQuestion>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_question_editor)

        lectureId = intent.getStringExtra("lectureId")
        levelId = intent.getStringExtra("levelId")
        levelName = intent.getStringExtra("levelName")

        database = FirebaseDatabase.getInstance().reference

        // 🧭 Toolbar setup
        val toolbar = findViewById<MaterialToolbar>(R.id.questionsToolbar)
        toolbar.title = "Manage Questions"
        toolbar.subtitle = levelName
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor))
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.black))
        toolbar.setNavigationOnClickListener { finish() }

        recyclerView = findViewById(R.id.questionsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdminQuestionAdapter(
            questions,
            onDelete = { confirmDeleteQuestion(it) },
            onEdit = { showQuestionDialog(it) }
        )
        recyclerView.adapter = adapter

        addQuestionBtn = findViewById(R.id.addQuestionButton)
        addQuestionBtn.setOnClickListener { showQuestionDialog(null) }

        // 🧠 Deploy button validation
        deployQuizBtn = findViewById(R.id.deployQuizButton)
        deployQuizBtn.setOnClickListener { validateBeforeDeploy() }

        loadQuestions()
    }

    private fun loadQuestions() {
        val path = "quizzes/$lectureId/$levelId/questionList"
        questions.clear()

        database.child(path)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    questions.clear()
                    for (qSnap in snapshot.children) {
                        val id = qSnap.key ?: continue
                        val question = qSnap.child("question").getValue(String::class.java) ?: ""
                        val correct = qSnap.child("correct").getValue(String::class.java) ?: ""
                        val explanation = qSnap.child("explanation").getValue(String::class.java) ?: ""
                        val instruction = qSnap.child("instruction").getValue(String::class.java) ?: ""
                        val options = mutableListOf<String>()
                        qSnap.child("options").children.forEach { opt ->
                            options.add(opt.getValue(String::class.java) ?: "")
                        }

                        questions.add(AdminQuestion(id, question, correct, explanation, instruction, options))
                    }

                    adapter.notifyDataSetChanged()
                    if (questions.isEmpty()) {
                        Toast.makeText(this@AdminQuestionEditorActivity, "No questions yet.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AdminQuestionEditorActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showQuestionDialog(existing: AdminQuestion?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (existing == null) "Add Question" else "Edit Question")

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_question, null)
        builder.setView(view)

        val instructionInput = view.findViewById<EditText>(R.id.inputInstruction)
        val qInput = view.findViewById<EditText>(R.id.inputQuestion)
        val aInput = view.findViewById<EditText>(R.id.inputOptionA)
        val bInput = view.findViewById<EditText>(R.id.inputOptionB)
        val cInput = view.findViewById<EditText>(R.id.inputOptionC)
        val dInput = view.findViewById<EditText>(R.id.inputOptionD)
        val correctInput = view.findViewById<EditText>(R.id.inputCorrect)
        val explanationInput = view.findViewById<EditText>(R.id.inputExplanation)

        // Prefill if editing
        existing?.let {
            instructionInput.setText(it.instruction)
            qInput.setText(it.question)
            aInput.setText(it.options.getOrNull(0) ?: "")
            bInput.setText(it.options.getOrNull(1) ?: "")
            cInput.setText(it.options.getOrNull(2) ?: "")
            dInput.setText(it.options.getOrNull(3) ?: "")
            correctInput.setText(it.correct)
            explanationInput.setText(it.explanation)
        }

        builder.setPositiveButton(if (existing == null) "Add" else "Update") { _, _ ->
            val instruction = instructionInput.text.toString().trim()
            val question = qInput.text.toString().trim()
            val options = listOf(
                aInput.text.toString().trim(),
                bInput.text.toString().trim(),
                cInput.text.toString().trim(),
                dInput.text.toString().trim()
            )
            val correct = correctInput.text.toString().trim()
            val explanation = explanationInput.text.toString().trim()

            if (instruction.isEmpty() || question.isEmpty() || correct.isEmpty() || options.any { it.isEmpty() }) {
                Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val questionData = mapOf(
                "instruction" to instruction,
                "question" to question,
                "correct" to correct,
                "explanation" to explanation,
                "options" to options
            )

            val path = "quizzes/$lectureId/$levelId/questionList"
            val ref = if (existing == null) database.child(path).push()
            else database.child("$path/${existing.id}")

            ref.setValue(questionData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Saved successfully!", Toast.LENGTH_SHORT).show()
                    loadQuestions()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun confirmDeleteQuestion(question: AdminQuestion) {
        AlertDialog.Builder(this)
            .setTitle("Delete Question")
            .setMessage("Are you sure you want to delete this question?")
            .setPositiveButton("Delete") { _, _ ->
                database.child("quizzes/$lectureId/$levelId/questionList/${question.id}")
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show()
                        loadQuestions()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ✅ Validate question count before allowing deployment
    private fun validateBeforeDeploy() {
        val path = "quizzes/$lectureId/$levelId/questionList"

        database.child(path).get().addOnSuccessListener { snapshot ->
            val count = snapshot.childrenCount.toInt()
            val required = if (levelId?.lowercase() == "post-test" || levelName?.lowercase()?.contains("post") == true) 20 else 10

            if (count < required) {
                AlertDialog.Builder(this)
                    .setTitle("Not Enough Questions")
                    .setMessage("This level requires at least $required questions before it can be deployed.\nCurrently: $count.")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Deploy Quiz?")
                    .setMessage("All requirements met! Deploy this quiz level now?")
                    .setPositiveButton("Deploy") { _, _ ->
                        database.child("quizzes/$lectureId/$levelId/deployed").setValue(true)
                        Toast.makeText(this, "Quiz deployed successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to validate: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
