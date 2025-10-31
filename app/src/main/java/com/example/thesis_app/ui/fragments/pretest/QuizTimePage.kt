package com.example.thesis_app.ui.fragments.pretest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.thesis_app.PreAssessmentActivity
import com.example.thesis_app.QuizActivity
import com.example.thesis_app.R
import com.example.thesis_app.models.QuestionModel
import com.example.thesis_app.models.QuizPartItem
import com.google.firebase.database.FirebaseDatabase

class QuizTimePage : Fragment(R.layout.pretest_last_page) {
    private lateinit var nextButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nextButton = view.findViewById(R.id.button)

        // Disable back press on this page
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Do nothing (prevent going back)
                }
            })

        nextButton.setOnClickListener {
            val sharedPrefs = requireContext().getSharedPreferences("USER_PREFS", 0)
            val studentId = sharedPrefs.getString("studentId", "") ?: ""

            if (studentId.isEmpty()) {
                Log.e("QuizTimePage", "Student ID missing in SharedPreferences")
                return@setOnClickListener
            }

            val db = FirebaseDatabase.getInstance().reference

            // 🔹 Fetch student's classCode from users/{studentId}/classes
            db.child("users").child(studentId).child("classes")
                .get()
                .addOnSuccessListener { classSnapshot ->
                    if (!classSnapshot.exists()) {
                        Log.e("QuizTimePage", "No class found for this student!")
                        return@addOnSuccessListener
                    }

                    // Get the first classCode (assuming student joins one class)
                    val classCode = classSnapshot.children.firstOrNull()?.key ?: ""

                    if (classCode.isEmpty()) {
                        Log.e("QuizTimePage", "Class code is empty!")
                        return@addOnSuccessListener
                    }

                    // 🔹 Fetch pre-test quiz (ID: 835247)
                    db.child("quizzes").child("835247").get()
                        .addOnSuccessListener { quizSnapshot ->
                            if (!quizSnapshot.exists()) {
                                Log.e("QuizTimePage", "Pre-test quiz not found!")
                                return@addOnSuccessListener
                            }

                            val title = quizSnapshot.child("title").getValue(String::class.java) ?: ""
                            val subtitle = quizSnapshot.child("subtitle").getValue(String::class.java) ?: ""
                            val partSnapshot = quizSnapshot.child("part1").child("questionList")

                            val questions = mutableListOf<QuestionModel>()
                            for (qSnap in partSnapshot.children) {
                                val question = qSnap.getValue(QuestionModel::class.java)
                                if (question != null) {
                                    questions.add(question)
                                }
                            }

                            // 🔹 Completely randomize question order
                            questions.shuffle()

                            val partItem = QuizPartItem(
                                quizId = quizSnapshot.key ?: "quiz1",
                                quizTitle = title,
                                quizSubtitle = subtitle,
                                partId = "part1",
                                questions = questions,
                                order = 2
                            )

                            // 🔹 Launch QuizActivity with classCode
                            val intent = Intent(requireContext(), QuizActivity::class.java)
                            QuizActivity.questionModelList = partItem.questions.toMutableList()
                            intent.putExtra("QUIZ_ID", partItem.quizId)
                            intent.putExtra("PART_ID", "part1")
                            intent.putExtra("STUDENT_ID", studentId)
                            intent.putExtra("CLASS_CODE", classCode) // ✅ Pass class code

                            startActivity(intent)
                            requireActivity().finish()
                        }
                        .addOnFailureListener {
                            Log.e("QuizTimePage", "Failed to fetch pre-test quiz: ${it.message}")
                        }
                }
                .addOnFailureListener {
                    Log.e("QuizTimePage", "Failed to fetch class code: ${it.message}")
                }
        }
    }
}
