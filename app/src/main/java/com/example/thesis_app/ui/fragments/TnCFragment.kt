package com.example.thesis_app.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.CheckBox
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.thesis_app.LoadingActivity
import com.example.thesis_app.R
import com.example.thesis_app.SignupActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class TnCFragment : Fragment(R.layout.tnc_page_signup) {
    private lateinit var btnSignup: MaterialButton
    private lateinit var checkBox: CheckBox

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnSignup = view.findViewById(R.id.btnSignUp)
        checkBox = view.findViewById(R.id.checkBox2)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    (activity as? SignupActivity)?.previousStep()
                    parentFragmentManager.popBackStack()
                }
            })

        btnSignup.setOnClickListener {
            btnSignup.isEnabled = false
            val role = arguments?.getString("role")
            val email = arguments?.getString("email")
            val password = arguments?.getString("password")
            val auth = FirebaseAuth.getInstance()

            if (role.isNullOrEmpty() || email.isNullOrEmpty() || password.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Missing signup information", Toast.LENGTH_SHORT).show()
                btnSignup.isEnabled = true
                return@setOnClickListener
            }

            if (!checkBox.isChecked) {
                Toast.makeText(requireContext(), "You must agree to the Terms & Conditions", Toast.LENGTH_SHORT).show()
                btnSignup.isEnabled = true
                return@setOnClickListener
            }

            auth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            btnSignup.isEnabled = true
                        }, 1000)

                        val signInMethods = task.result.signInMethods
                        if (signInMethods.isNullOrEmpty()) {
                            // ✅ Email not registered yet → create account
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { signupTask ->
                                    if (signupTask.isSuccessful) {
                                        val userId = signupTask.result.user?.uid
                                        val userData = mapOf(
                                            "role" to role,
                                            "email" to email
                                        )

                                        FirebaseDatabase.getInstance().reference
                                            .child("users")
                                            .child(userId!!)
                                            .setValue(userData)
                                            .addOnSuccessListener {
                                                val intent = Intent(requireContext(), LoadingActivity::class.java)
                                                intent.putExtra("mode", "createAccount")
                                                startActivity(intent)
                                                requireActivity().finish()
                                            }
                                    } else {
                                        Toast.makeText(requireContext(), signupTask.exception?.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            // ⚠️ Email already exists
                            Toast.makeText(requireContext(), "Account already exists. Please log in.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

        }
    }
}