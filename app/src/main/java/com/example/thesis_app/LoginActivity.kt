package com.example.thesis_app

import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doAfterTextChanged
import com.example.thesis_app.databinding.LoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : ComponentActivity() {

    private lateinit var binding: LoginBinding
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back press confirmation
        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmation()
                }
            })

        // Initially disable login button
        binding.LoginButton.isEnabled = false
        binding.LoginButton.alpha = 0.5f

        // Setup live validation for email and password
        setupLiveValidation()

        // Button listeners
        binding.LoginButton.setOnClickListener { attemptLogin() }
        binding.textView9.setOnClickListener { goToSignup() }

        // Auto-login if user already signed in
        autoLoginIfAvailable()
    }

    private fun setupLiveValidation() {
        val yellowColor = Color.parseColor("#FFC007")
        val redColor = Color.RED

        // Email live validation
        binding.editEmail.doAfterTextChanged { text ->
            val email = text?.toString()?.trim() ?: ""
            when {
                email.isEmpty() -> {
                    binding.emailLayout.error = "Email cannot be empty"
                    binding.emailLayout.boxStrokeColor = redColor
                }
                else -> {
                    binding.emailLayout.error = null
                    binding.emailLayout.helperText = null
                    binding.emailLayout.boxStrokeColor = yellowColor
                }
            }
            enableLoginIfValid()
        }

        // Password live validation
        binding.Password.doAfterTextChanged { text ->
            val password = text?.toString() ?: ""
            if (password.isEmpty()) {
                binding.passwordLayout.error = "Password cannot be empty"
                binding.passwordLayout.boxStrokeColor = redColor
            } else {
                binding.passwordLayout.error = null
                binding.passwordLayout.helperText = null
                binding.passwordLayout.boxStrokeColor = yellowColor
            }
            enableLoginIfValid()
        }
    }

    private fun enableLoginIfValid() {
        val emailValid = binding.editEmail.text?.isNotEmpty() == true
        val passwordValid = binding.Password.text?.isNotEmpty() == true

        val enable = emailValid && passwordValid
        binding.LoginButton.isEnabled = enable
        binding.LoginButton.alpha = if (enable) 1f else 0.5f
    }

    private fun attemptLogin() {
        val email = binding.editEmail.text.toString().trim()
        val password = binding.Password.text.toString()

        // Safety check before calling Firebase
        if (email.isEmpty() || password.isEmpty()) {
            if (email.isEmpty()) binding.emailLayout.error = "Email cannot be empty"
            if (password.isEmpty()) binding.passwordLayout.error = "Password cannot be empty"
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        binding.LoginButton.isEnabled = false
        binding.loginProgress.visibility = android.widget.ProgressBar.VISIBLE

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.loginProgress.visibility = android.widget.ProgressBar.GONE
                binding.LoginButton.isEnabled = true

                if (task.isSuccessful) {
                    // Clear any previous error states on success
                    binding.emailLayout.error = null
                    binding.passwordLayout.error = null
                    binding.emailLayout.helperText = null
                    binding.passwordLayout.helperText = null
                    binding.emailLayout.boxStrokeColor = Color.parseColor("#FFC007")
                    binding.passwordLayout.boxStrokeColor = Color.parseColor("#FFC007")

                    fetchUserData(email)
                    binding.editEmail.text?.clear()
                    binding.Password.text?.clear()
                } else {
                    // Red boxes for incorrect credentials
                    binding.emailLayout.error = "Email or password is incorrect"
                    binding.passwordLayout.error = "Email or password is incorrect"
                    binding.emailLayout.boxStrokeColor = Color.RED
                    binding.passwordLayout.boxStrokeColor = Color.RED
                    Toast.makeText(this, "Email or password is incorrect", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun fetchUserData(email: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
        dbRef.orderByChild("email").equalTo(email).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val role = child.child("role").getValue(String::class.java)
                        val emailDb = child.child("email").getValue(String::class.java)
                        val studentId = child.child("studentID").getValue(String::class.java)

                        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("role", role)
                            putString("email", emailDb)
                            putString("studentId", studentId)
                            apply()
                        }

                        val intent = android.content.Intent(this, LoadingActivity::class.java).apply {
                            putExtra("mode", "login")
                            putExtra("role", role)
                            putExtra("email", emailDb)
                            putExtra("studentId", studentId)
                        }
                        startActivity(intent)
                        finish()
                        break
                    }
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                android.util.Log.e("FirebaseError", "Failed to fetch user data: ${it.message}")
            }
    }

    private fun goToSignup() {
        val intent = android.content.Intent(this, LoadingActivity::class.java)
        intent.putExtra("mode", "signup")
        startActivity(intent)
    }

    private fun autoLoginIfAvailable() {
        firebaseAuth.currentUser?.email?.let { fetchUserData(it) }
    }

    private fun showExitConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }
}