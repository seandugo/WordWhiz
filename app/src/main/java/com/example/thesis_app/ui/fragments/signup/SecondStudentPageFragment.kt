package com.example.thesis_app.ui.fragments.signup

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.thesis_app.R
import com.example.thesis_app.SignupActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SecondStudentPageFragment : Fragment(R.layout.student_signup) {

    private lateinit var btnSignUp: MaterialButton
    private lateinit var editEmail: TextInputEditText
    private lateinit var editPassword: TextInputEditText
    private lateinit var editStudentId: TextInputEditText
    private lateinit var editConfirmPassword: TextInputEditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnSignUp = view.findViewById(R.id.btnSignUp)
        editEmail = view.findViewById(R.id.editEmail)
        editStudentId = view.findViewById(R.id.editStudentId)
        editPassword = view.findViewById(R.id.editPassword)
        editConfirmPassword = view.findViewById(R.id.editConfirmPassword)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    (activity as? SignupActivity)?.previousStep()
                    parentFragmentManager.popBackStack()
                }
            })

        btnSignUp.setOnClickListener {
            val email = editEmail.text.toString()
            val studentID = editStudentId.text.toString()
            val password = editPassword.text.toString()
            val confirmPassword = editConfirmPassword.text.toString()

            if (email.isNotEmpty() && password == confirmPassword) {
                val bundle = Bundle()
                bundle.putString("role", arguments?.getString("role"))
                bundle.putString("email", email)
                bundle.putString("password", password)
                bundle.putString("studentID", studentID)

                val fragment = TnCFragment()
                fragment.arguments = bundle
                (activity as? SignupActivity)?.nextStep()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerView, fragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                Toast.makeText(requireContext(), "Invalid inputs", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
