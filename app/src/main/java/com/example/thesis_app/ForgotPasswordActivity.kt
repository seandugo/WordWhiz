package com.example.thesis_app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var resetPasswordBtn: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        emailInput = findViewById(R.id.emailInput)
        resetPasswordBtn = findViewById(R.id.resetPasswordBtn)
        auth = FirebaseAuth.getInstance()

        resetPasswordBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable the button to prevent double-clicking
            resetPasswordBtn.isEnabled = false
            resetPasswordBtn.text = "Sending..."

            auth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener { fetchTask ->
                    if (fetchTask.isSuccessful) {
                        val signInMethods = fetchTask.result?.signInMethods
                        if (signInMethods.isNullOrEmpty()) {
                            // ❗ Email not registered
                            Snackbar.make(
                                resetPasswordBtn,
                                "This email is not registered.",
                                Snackbar.LENGTH_LONG
                            ).setAction("OK") {}.show()

                            resetPasswordBtn.isEnabled = true
                            resetPasswordBtn.text = "Reset Password"
                        } else {
                            // ✅ Email exists, proceed with password reset
                            auth.sendPasswordResetEmail(email)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(
                                            this,
                                            "Password reset email sent! Please check your inbox or Spam folder.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        finish()
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Error: ${task.exception?.message}",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        resetPasswordBtn.isEnabled = true
                                        resetPasswordBtn.text = "Reset Password"
                                    }
                                }
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Error checking email: ${fetchTask.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()

                        resetPasswordBtn.isEnabled = true
                        resetPasswordBtn.text = "Reset Password"
                    }
                }
        }
    }
}
