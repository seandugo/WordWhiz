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

            FirebaseDatabase.getInstance().reference
                .child("quizzes")
                .child("quiz1")
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val quizModel = snapshot.getValue(QuizModel::class.java)
                        if (quizModel != null) {
                            val fixedQuiz = quizModel.copy(id = snapshot.key ?: "")

                            val intent = Intent(requireContext(), QuizActivity::class.java)
                            QuizActivity.questionModelList = fixedQuiz.questionList
                            intent.putExtra("QUIZ_ID", fixedQuiz.id)
                            intent.putExtra("studentId", studentId)
                            startActivity(intent)
                            requireActivity().finish()
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("QuizTimePage", "Failed to fetch quiz: ${it.message}")
                }
        }
    }
}
