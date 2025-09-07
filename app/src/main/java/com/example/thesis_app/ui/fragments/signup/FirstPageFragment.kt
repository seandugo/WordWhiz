package com.example.thesis_app.ui.fragments.signup

import com.example.thesis_app.SignupActivity
import android.os.Bundle
import android.view.View
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.thesis_app.R
import androidx.activity.OnBackPressedCallback

class FirstPageFragment : Fragment(R.layout.first_page_signup) {
    private lateinit var teacherButton: CardView
    private lateinit var studentButton: CardView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        teacherButton = view.findViewById(R.id.teacher)
        studentButton = view.findViewById(R.id.student)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    (activity as? SignupActivity)?.showExitConfirmation()
                }
            })

        teacherButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("role", "teacher")
            val secondPageFragment = SecondPageFragment()
            secondPageFragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, secondPageFragment) // container in your activity layout
                .addToBackStack(null) // so back button goes back to FirstPageFragment
                .commit()
            (activity as? SignupActivity)?.nextStep()
        }

        studentButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("role", "student")
            val secondStudentPageFragment = SecondStudentPageFragment()
            secondStudentPageFragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, secondStudentPageFragment) // container in your activity layout
                .addToBackStack(null) // so back button goes back to FirstPageFragment
                .commit()

            (activity as? SignupActivity)?.nextStep()
        }
    }

}
