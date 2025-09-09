package com.example.thesis_app.ui.fragments.signup

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.thesis_app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.example.thesis_app.SignupActivity

class SecondPageFragment : Fragment(R.layout.signup) {

    private lateinit var editEmail: TextInputEditText
    private lateinit var editPassword: TextInputEditText
    private lateinit var editTeacherName: TextInputEditText
    private lateinit var editConfirmPassword: TextInputEditText
    private lateinit var btnSignUp: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views from fragment's layout
        editEmail = view.findViewById(R.id.editEmail)
        editTeacherName = view.findViewById(R.id.editName)
        editPassword = view.findViewById(R.id.editPassword)
        editConfirmPassword = view.findViewById(R.id.editConfirmPassword)
        btnSignUp = view.findViewById(R.id.btnSignUp)

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
            val name = editTeacherName.text.toString()
            val password = editPassword.text.toString()
            val confirmPassword = editConfirmPassword.text.toString()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val bundle = Bundle()
            bundle.putString("role", arguments?.getString("role"))
            bundle.putString("name", name)
            bundle.putString("email", email)
            bundle.putString("password", password)

            val fragment = TnCFragment()
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, fragment)
                .addToBackStack(null)
                .commit()

            (activity as? SignupActivity)?.nextStep()
        }
    }
}
