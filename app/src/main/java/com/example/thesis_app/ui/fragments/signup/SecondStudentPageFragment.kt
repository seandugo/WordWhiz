package com.example.thesis_app.ui.fragments.signup

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.util.Patterns
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.thesis_app.R
import com.example.thesis_app.SignupActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.ProgressBar
import android.widget.TextView

class SecondStudentPageFragment : Fragment(R.layout.student_signup) {

    private lateinit var layoutName: TextInputLayout
    private lateinit var layoutEmail: TextInputLayout
    private lateinit var layoutPassword: TextInputLayout
    private lateinit var layoutConfirmPassword: TextInputLayout

    private lateinit var editName: TextInputEditText
    private lateinit var editEmail: TextInputEditText
    private lateinit var editPassword: TextInputEditText
    private lateinit var editConfirmPassword: TextInputEditText
    private lateinit var btnSignUp: MaterialButton

    private lateinit var passwordStrengthBar: ProgressBar
    private lateinit var passwordStrengthText: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutName = view.findViewById(R.id.layoutName)
        layoutEmail = view.findViewById(R.id.layoutEmail)
        layoutPassword = view.findViewById(R.id.layoutPassword)
        layoutConfirmPassword = view.findViewById(R.id.layoutConfirmPassword)

        editName = view.findViewById(R.id.editStudentName)
        editEmail = view.findViewById(R.id.editEmail)
        editPassword = view.findViewById(R.id.editPassword)
        editConfirmPassword = view.findViewById(R.id.editConfirmPassword)

        passwordStrengthBar = view.findViewById(R.id.passwordStrengthBar)
        passwordStrengthText = view.findViewById(R.id.passwordStrengthText)

        btnSignUp = view.findViewById(R.id.btnSignUp)

        // Restrict input length for name only
        editName.filters = arrayOf(InputFilter.LengthFilter(50))

        // Allow unlimited characters for passwords
        editPassword.filters = arrayOf<InputFilter>()
        editConfirmPassword.filters = arrayOf<InputFilter>()

        val yellowColor = Color.parseColor("#FFC007")
        layoutPassword.setEndIconTintList(ColorStateList.valueOf(yellowColor))
        layoutConfirmPassword.setEndIconTintList(ColorStateList.valueOf(yellowColor))

        btnSignUp.isEnabled = false
        btnSignUp.alpha = 0.5f

        addLiveValidation()

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmationDialog()
                }
            })

        btnSignUp.setOnClickListener { validateInputsAndProceed() }
    }

    private fun addLiveValidation() {
        editName.addTextChangedListener {
            validateName()
            validateInputs()
        }
        editEmail.addTextChangedListener {
            validateEmail()
            validateInputs()
        }
        editPassword.addTextChangedListener {
            val pwd = it.toString()
            updatePasswordStrength(pwd)
            showPasswordRequirements(pwd)
            validatePasswordMatch()
            validateInputs()
        }
        editConfirmPassword.addTextChangedListener {
            validatePasswordMatch()
            validateInputs()
        }
    }

    private fun validateName() {
        val name = editName.text?.toString()?.trim() ?: ""
        val nameValid = name.matches(Regex("^[A-Za-z]+(\\s[A-Z]\\.)?\\s[A-Za-z]+$"))
        layoutName.error = if (!nameValid && name.isNotEmpty()) "Name (F.N./M.I./Surname)" else null
    }

    private fun validateEmail() {
        val email = editEmail.text?.toString()?.trim() ?: ""
        layoutEmail.error =
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() && email.isNotEmpty()) "Enter a valid email"
            else null
    }

    private fun validatePasswordMatch() {
        val password = editPassword.text?.toString() ?: ""
        val confirmPassword = editConfirmPassword.text?.toString() ?: ""
        if (confirmPassword.isNotEmpty()) {
            if (password == confirmPassword) {
                layoutConfirmPassword.helperText = "✔ Passwords match"
                layoutConfirmPassword.setHelperTextColor(
                    ColorStateList.valueOf(Color.parseColor("#FFC007"))
                )
                layoutConfirmPassword.error = null
            } else {
                layoutConfirmPassword.helperText = "Passwords do not match"
                layoutConfirmPassword.setHelperTextColor(ColorStateList.valueOf(Color.RED))
            }
        } else layoutConfirmPassword.helperText = null
    }

    private fun validateInputs() {
        val nameValid = layoutName.error == null && editName.text?.isNotEmpty() == true
        val emailValid = layoutEmail.error == null && editEmail.text?.isNotEmpty() == true
        val passwordValid = editPassword.text?.isNotEmpty() == true
        val passwordsMatch = editPassword.text.toString() == editConfirmPassword.text.toString() &&
                editPassword.text.toString().isNotEmpty()

        val enable = nameValid && emailValid && passwordValid && passwordsMatch
        btnSignUp.isEnabled = enable
        btnSignUp.alpha = if (enable) 1f else 0.5f
    }

    private fun showExitConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restart Signup?")
            .setMessage("If you go back now, all entered data will be lost. Are you sure?")
            .setPositiveButton("Yes, Restart") { _, _ ->
                clearFormFields()
                (activity as? SignupActivity)?.previousStep()
                parentFragmentManager.popBackStack()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearFormFields() {
        editName.setText("")
        editEmail.setText("")
        editPassword.setText("")
        editConfirmPassword.setText("")
        layoutName.error = null
        layoutEmail.error = null
        layoutPassword.error = null
        layoutConfirmPassword.error = null
        passwordStrengthBar.progress = 0
        passwordStrengthText.text = ""
        layoutPassword.helperText = null
        layoutConfirmPassword.helperText = null
        btnSignUp.isEnabled = false
        btnSignUp.alpha = 0.5f
    }

    private fun validateInputsAndProceed() {
        val bundle = Bundle().apply {
            putString("role", arguments?.getString("role"))
            putString("name", editName.text.toString().trim())
            putString("email", editEmail.text.toString().trim())
            putString("password", editPassword.text.toString())
        }

        val fragment = TnCFragment()
        fragment.arguments = bundle
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, fragment)
            .addToBackStack(null)
            .commit()

        (activity as? SignupActivity)?.nextStep()
    }

    private fun updatePasswordStrength(password: String) {
        // Simple strength scoring
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { it.isLowerCase() } && password.any { it.isUpperCase() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        val progress = (score * 25).coerceAtMost(100)
        passwordStrengthBar.max = 100

        ObjectAnimator.ofInt(passwordStrengthBar, "progress", passwordStrengthBar.progress, progress).apply {
            duration = 250
            start()
        }

        when {
            score <= 1 -> {
                passwordStrengthText.text = "Weak"
                passwordStrengthText.setTextColor(Color.RED)
                passwordStrengthBar.progressTintList = ColorStateList.valueOf(Color.RED)
            }
            score in 2..3 -> {
                passwordStrengthText.text = "Medium"
                passwordStrengthText.setTextColor(Color.parseColor("#FFA500"))
                passwordStrengthBar.progressTintList = ColorStateList.valueOf(Color.parseColor("#FFA500"))
            }
            else -> {
                passwordStrengthText.text = "Strong"
                passwordStrengthText.setTextColor(Color.parseColor("#008000"))
                passwordStrengthBar.progressTintList = ColorStateList.valueOf(Color.parseColor("#008000"))
            }
        }
    }

    private fun showPasswordRequirements(password: String) {
        val upperCase = password.any { it.isUpperCase() }
        val lowerCase = password.any { it.isLowerCase() }
        val number = password.any { it.isDigit() }
        val special = password.any { "!@#\$%^&*()-_=+[]{}|;:'\",.<>?/`~".contains(it) }

        val requirements = mutableListOf<String>()
        if (!upperCase) requirements.add("1 uppercase")
        if (!lowerCase) requirements.add("1 lowercase")
        if (!number) requirements.add("1 number")
        if (!special) requirements.add("1 special character")

        layoutPassword.helperText = if (requirements.isEmpty()) {
            "✔ Strong password"
        } else "Missing: ${requirements.joinToString(", ")}"

        layoutPassword.setHelperTextColor(ColorStateList.valueOf(if (requirements.isEmpty()) Color.parseColor("#FFC007") else Color.RED))
    }
}