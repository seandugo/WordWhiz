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
import com.example.thesis_app.models.QuizModel
import com.google.firebase.database.FirebaseDatabase

class QuizTimePage : Fragment(R.layout.pretest_last_page) {
    private lateinit var nextButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nextButton = view.findViewById(R.id.button)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    (activity as? PreAssessmentActivity)?.previousStep()
                    parentFragmentManager.popBackStack()
                }
            })

        nextButton.setOnClickListener {
            val sharedPrefs = requireContext().getSharedPreferences("USER_PREFS", 0)
            val studentId = sharedPrefs.getString("studentId", "") ?: ""
            val numericGrade = sharedPrefs.getInt("grade_number", 0)
            if (numericGrade == 0) {
                Log.e("QuizTimePage", "Grade number is 0 or not set!")
                return@setOnClickListener
            }
            val gradeLevel = "grade$numericGrade"
            if (gradeLevel.isNullOrEmpty()) {
                Log.e("QuizTimePage", "Grade level is null or empty!")
                return@setOnClickListener
            }
            FirebaseDatabase.getInstance().reference
                .child("users")
                .child(studentId)
                .child("grade_level")
                .setValue(numericGrade)

            // Use gradeLevel string for Firebase path
            val quizPath = "quizzes/$gradeLevel/quiz1"
            Log.d("QuizTimePage", "Fetching quiz from $quizPath")

            FirebaseDatabase.getInstance().reference
                .child("quizzes")
                .child(gradeLevel)
                .child("quiz1")
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val quizModel = snapshot.getValue(QuizModel::class.java)
                        if (quizModel != null) {
                            val fixedQuiz = quizModel.copy(id = snapshot.key ?: "")

                            val intent = Intent(requireContext(), QuizActivity::class.java)
                            QuizActivity.questionModelList = fixedQuiz.questionList
                            QuizActivity.time = fixedQuiz.time
                            intent.putExtra("QUIZ_ID", fixedQuiz.id)
                            intent.putExtra("studentId", studentId)
                            startActivity(intent)
                            requireActivity().finish()
                        }
                    } else {
                        Log.e("QuizTimePage", "❌ No quiz found at $quizPath")
                    }
                }
                .addOnFailureListener {
                    Log.e("QuizTimePage", "Failed to fetch quiz: ${it.message}")
                }
        }
    }
}
