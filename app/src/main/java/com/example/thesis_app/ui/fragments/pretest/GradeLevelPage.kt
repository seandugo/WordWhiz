package com.example.thesis_app.ui.fragments.pretest

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.thesis_app.PreAssessmentActivity
import com.example.thesis_app.R
import android.widget.AutoCompleteTextView

class GradeLevelPage : Fragment(R.layout.grade_level) {
    private lateinit var nextButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nextButton = view.findViewById(R.id.button)
        val items = listOf("Grade 7", "Grade 8", "Grade 9","Grade 10")
        val autoCompleteTextView =
            view.findViewById<AutoCompleteTextView>(R.id.autoCompleteTextView)

        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        autoCompleteTextView.setAdapter(adapter)

        // Optional: Default value
        autoCompleteTextView.setText(items[0], false)

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