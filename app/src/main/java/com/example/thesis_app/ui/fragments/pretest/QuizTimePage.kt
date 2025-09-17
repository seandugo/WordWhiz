package com.example.thesis_app.ui.fragments.pretest

import android.content.Intent
import android.os.Bundle
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
    private lateinit var studentId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        studentId = requireActivity().intent.getStringExtra("studentId") ?: ""
    }

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
            FirebaseDatabase.getInstance().reference
                .child("quizzes")
                .child("quiz1")
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val quizModel = snapshot.getValue(QuizModel::class.java)
                        if (quizModel != null) {
                            // Manually set the id from Firebase key
                            val fixedQuiz = quizModel.copy(id = snapshot.key ?: "")

                            val intent = Intent(requireContext(), QuizActivity::class.java)
                            QuizActivity.questionModelList = fixedQuiz.questionList
                            QuizActivity.time = fixedQuiz.time
                            intent.putExtra("QUIZ_ID", fixedQuiz.id)   // ✅ not null anymore
                            intent.putExtra("studentId", studentId)
                            startActivity(intent)
                            requireActivity().finish()
                        }
                    }
                }
        }
    }
}