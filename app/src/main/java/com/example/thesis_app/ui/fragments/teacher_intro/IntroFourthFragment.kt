package com.example.thesis_app.ui.fragments.teacher_intro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.thesis_app.PreAssessmentActivity
import com.example.thesis_app.R
import com.example.thesis_app.TeacherActivity
import com.example.thesis_app.TeacherIntroActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class IntroFourthFragment : Fragment(R.layout.intro_teacher_fourth_page) {
    private lateinit var nextButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nextButton = view.findViewById(R.id.button)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    (activity as? TeacherIntroActivity)?.previousStep()
                    parentFragmentManager.popBackStack()
                }
            })

        nextButton.setOnClickListener {
            markIntroCompletedAndProceed()
        }
    }

    private fun markIntroCompletedAndProceed() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        if (!uid.isNullOrEmpty()) {
            val dbRef = FirebaseDatabase.getInstance().reference
            dbRef.child("users").child(uid).child("introCompleted").setValue(true)
                .addOnSuccessListener {
                    // Once saved, go to TeacherActivity
                    val intent = Intent(requireContext(), TeacherActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
        } else {
            val intent = Intent(requireContext(), TeacherActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }
}
