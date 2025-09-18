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

        // Restrict input length for name (max 50)
        editName.filters = arrayOf(InputFilter.LengthFilter(50))

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
            val password = it.toString()
            updatePasswordStrength(password)
            showPasswordRequirements(password)
            validateInputs()
        }
        editConfirmPassword.addTextChangedListener {
            validatePasswordMatch()
            validateInputs()
        }
    }

    private fun validateName() {
        val name = editName.text?.toString()?.trim() ?: ""
        // Regex: FirstName [MiddleInitial.] LastName
        val nameValid = name.matches(Regex("^[A-Za-z]+(\\s[A-Z]\\.)?\\s[A-Za-z]+\$"))

        if (!nameValid && name.isNotEmpty()) {
            layoutName.error = "Name (F.N./M.I./Surname)"
        } else {
            layoutName.error = null
        }
    }

    private fun validateEmail() {
        val email = editEmail.text?.toString()?.trim() ?: ""
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() && email.isNotEmpty()) {
            layoutEmail.error = "Enter a valid email"
        } else {
            layoutEmail.error = null
        }
    }

    private fun validateInputs() {
        val nameValid = layoutName.error == null && editName.text?.isNotEmpty() == true
        val emailValid = layoutEmail.error == null && editEmail.text?.isNotEmpty() == true
        val passwordValid = isStrongPassword(editPassword.text?.toString() ?: "")
        val passwordsMatch = editPassword.text.toString() == editConfirmPassword.text.toString() &&
                editPassword.text.toString().isNotEmpty()

        val enable = nameValid && emailValid && passwordValid && passwordsMatch
        btnSignUp.isEnabled = enable
        btnSignUp.alpha = if (enable) 1f else 0.5f
    }

    private fun validatePasswordMatch() {
        val password = editPassword.text?.toString() ?: ""
        val confirmPassword = editConfirmPassword.text?.toString() ?: ""
        if (confirmPassword.isNotEmpty()) {
            if (password == confirmPassword) {
                layoutConfirmPassword.helperText = "✔ Passwords match"
                layoutConfirmPassword.setHelperTextColor(ColorStateList.valueOf(Color.parseColor("#FFC007")))
                layoutConfirmPassword.error = null
            } else {
                layoutConfirmPassword.helperText = "Passwords do not match"
                layoutConfirmPassword.setHelperTextColor(ColorStateList.valueOf(Color.RED))
            }
        } else {
            layoutConfirmPassword.helperText = null
        }
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
        btnSignUp.isEnabled = false
        btnSignUp.alpha = 0.5f
    }

    private fun validateInputsAndProceed() {
        validateInputs()
        if (!btnSignUp.isEnabled) return

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
        val score = calculatePasswordScore(password)
        val progress = score * 20
        passwordStrengthBar.max = 100

        ObjectAnimator.ofInt(passwordStrengthBar, "progress", passwordStrengthBar.progress, progress).apply {
            duration = 250
            start()
        }

        when (score) {
            0, 1, 2 -> {
                passwordStrengthText.text = "Weak"
                passwordStrengthText.setTextColor(Color.RED)
                passwordStrengthBar.progressTintList = ColorStateList.valueOf(Color.RED)
            }
            3, 4 -> {
                passwordStrengthText.text = "Medium"
                passwordStrengthText.setTextColor(Color.parseColor("#FFA500"))
                passwordStrengthBar.progressTintList = ColorStateList.valueOf(Color.parseColor("#FFA500"))
            }
            5 -> {
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
        val lengthValid = password.length in 5..8

        val requirements = mutableListOf<String>()
        if (!lengthValid) requirements.add("5-8 characters")
        if (!upperCase) requirements.add("1 uppercase")
        if (!lowerCase) requirements.add("1 lowercase")
        if (!number) requirements.add("1 number")
        if (!special) requirements.add("1 special character")

        if (requirements.isEmpty()) {
            layoutPassword.helperText = "✔ Strong password"
            layoutPassword.setHelperTextColor(ColorStateList.valueOf(Color.parseColor("#FFC007")))
        } else {
            layoutPassword.helperText = "Missing: ${requirements.joinToString(", ")}"
            layoutPassword.setHelperTextColor(ColorStateList.valueOf(Color.RED))
        }
    }

    private fun isStrongPassword(password: String): Boolean {
        return password.length in 5..8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() } &&
                password.any { "!@#\$%^&*()-_=+[]{}|;:'\",.<>?/`~".contains(it) }
    }

    private fun calculatePasswordScore(password: String): Int {
        var score = 0
        if (password.length in 5..8) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { "!@#\$%^&*()-_=+[]{}|;:'\",.<>?/`~".contains(it) }) score++
        return score
    }

    private fun disableNextButton() {
        btnSignUp.isEnabled = false
        btnSignUp.alpha = 0.5f
    }
}