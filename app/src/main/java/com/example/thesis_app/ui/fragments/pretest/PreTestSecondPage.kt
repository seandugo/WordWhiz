package com.example.thesis_app.ui.fragments.pretest

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.thesis_app.PreAssessmentActivity
import com.example.thesis_app.R

class PreTestSecondPage : Fragment(R.layout.pre_test_second_page) {
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
            val secondPageFragment = ClassCodePage()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView2, secondPageFragment)
                .addToBackStack(null)
                .commit()
            (activity as? PreAssessmentActivity)?.nextStep()
        }
    }

}