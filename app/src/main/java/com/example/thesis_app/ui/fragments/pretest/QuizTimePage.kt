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
import com.example.thesis_app.models.QuizModel
import com.example.thesis_app.models.QuizPartItem
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

            val dbRef = FirebaseDatabase.getInstance().reference
                .child("quizzes")
                .child("quiz1")

            dbRef.get().addOnSuccessListener { quizSnapshot ->
                if (quizSnapshot.exists()) {
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

                    // Build QuizPartItem
                    val partItem = QuizPartItem(
                        quizId = quizSnapshot.key ?: "quiz1",
                        quizTitle = title,
                        quizSubtitle = subtitle,
                        partId = "part1",
                        questions = questions
                    )

                    // Send to QuizActivity
                    val intent = Intent(requireContext(), QuizActivity::class.java)
                    QuizActivity.questionModelList = partItem.questions
                    intent.putExtra("QUIZ_ID", partItem.quizId)      // "quiz1"
                    intent.putExtra("PART_ID", "part1")           // correct part name
                    intent.putExtra("studentId", studentId)
                    startActivity(intent)
                }
            }.addOnFailureListener {
                Log.e("QuizTimePage", "Failed to fetch pre-test quiz: ${it.message}")
            }
        }
    }
}
