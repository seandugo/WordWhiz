package com.example.thesis_app.ui.fragments.teacher_intro

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.thesis_app.PreAssessmentActivity
import com.example.thesis_app.R
import com.example.thesis_app.TeacherIntroActivity

class IntroSecondFragment : Fragment(R.layout.intro_teacher_second_page) {
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
            val secondPageFragment = IntroThirdFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView2, secondPageFragment)
                .addToBackStack(null)
                .commit()
            (activity as? TeacherIntroActivity)?.nextStep()
        }
    }

}