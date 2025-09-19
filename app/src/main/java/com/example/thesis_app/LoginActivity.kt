package com.example.thesis_app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.material.snackbar.Snackbar

class LoginActivity : ComponentActivity() {

    private lateinit var login: Button
    private lateinit var signup: TextView
    private lateinit var teacherEmail: TextInputEditText
    private lateinit var teacherPassword: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        login = findViewById(R.id.LoginButton)
        signup = findViewById(R.id.textView9)
        teacherEmail = findViewById(R.id.editEmail)
        teacherPassword = findViewById(R.id.Password)

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmation()
                }
            })

        login.setOnClickListener {
            disableButtons()
            Snackbar.make(findViewById(android.R.id.content), "Logging in…", Snackbar.LENGTH_SHORT).show()

            val email = teacherEmail.text.toString().trim()
            val password = teacherPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter teacher email and password", Toast.LENGTH_SHORT).show()
                enableButtons()
                return@setOnClickListener
            }

            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    Handler(Looper.getMainLooper()).postDelayed({
                        enableButtons()
                    }, 1000)

                    if (task.isSuccessful) {
                        fetchUserData(email)
                        teacherEmail.text?.clear()
                        teacherPassword.text?.clear()
                    } else {
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        signup.setOnClickListener {
            if (!login.isEnabled) return@setOnClickListener
            val intent = Intent(this, LoadingActivity::class.java)
            intent.putExtra("mode", "signup")
            startActivity(intent)
        }
    }

    private fun fetchUserData(email: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
        dbRef.orderByChild("email").equalTo(email).get()
            .addOnSuccessListener { snapshot ->
                enableButtons()
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val role = child.child("role").getValue(String::class.java)
                        val emailDb = child.child("email").getValue(String::class.java)

                        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE).edit()
                        prefs.putString("role", role)
                        prefs.putString("email", emailDb ?: email)

                        if (role == "student") {
                            val studentId = child.child("studentID").getValue(String::class.java)
                            var gradeValue = child.child("grade_level").value
                            var gradeNumber = when (gradeValue) {
                                is Long -> gradeValue.toInt()
                                is Int -> gradeValue
                                is String -> gradeValue.filter { it.isDigit() }.toIntOrNull() ?: 0
                                else -> 0
                            }

                            if (gradeNumber == 0) {
                                // Firebase has 0 or missing → set a default or prompt for correct grade
                                gradeNumber = 7  // for example, default to grade 7
                                FirebaseDatabase.getInstance().reference
                                    .child("users")
                                    .child(studentId ?: "")
                                    .child("grade_level")
                                    .setValue(gradeNumber)
                            }

                            prefs.putInt("grade_number", gradeNumber)
                            prefs.putString("grade_level", "grade$gradeNumber")
                            prefs.putString("studentId", studentId)
                            prefs.apply()
                        }

                        prefs.apply()

                        val intent = Intent(this, LoadingActivity::class.java)
                        intent.putExtra("mode", "login")
                        intent.putExtra("role", role)
                        startActivity(intent)
                        finish()
                        break
                    }
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                    enableButtons()
                }
            }
            .addOnFailureListener {
                Log.e("FirebaseError", "Failed to fetch user data: ${it.message}")
                enableButtons()
            }
    }

    override fun onStart() {
        super.onStart()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val email = currentUser.email
            if (email != null) {
                disableButtons()
                Snackbar.make(findViewById(android.R.id.content), "Logging in…", Snackbar.LENGTH_SHORT).show()

                val dbRef = FirebaseDatabase.getInstance().getReference("users")
                dbRef.orderByChild("email").equalTo(email).get()
                    .addOnSuccessListener { snapshot ->
                        enableButtons()
                        if (snapshot.exists()) {
                            for (child in snapshot.children) {
                                val name = child.child("name").getValue(String::class.java)
                                val emailDb = child.child("email").getValue(String::class.java)
                                val role = child.child("role").getValue(String::class.java)

                                if (!role.isNullOrEmpty()) {
                                    val intent = Intent(this, LoadingActivity::class.java)
                                    intent.putExtra("mode", "login")
                                    intent.putExtra("role", role)
                                    intent.putExtra("email", emailDb)
                                    intent.putExtra("name", name)

                                    if (role == "student") {
                                        val studentId = child.child("studentID").getValue(String::class.java)
                                        val gradeValue = child.child("grade_level").value
                                        val gradeNumber = when (gradeValue) {
                                            is Long -> gradeValue.toInt()
                                            is Int -> gradeValue
                                            is String -> gradeValue.filter { it.isDigit() }.toIntOrNull() ?: 0
                                            else -> 0
                                        }

                                        // Save to SharedPreferences
                                        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE).edit()
                                        prefs.putInt("grade_number", gradeNumber)
                                        prefs.putString("grade_level", "grade$gradeNumber")
                                        prefs.putString("studentId", studentId)
                                        prefs.putString("role", role)
                                        prefs.putString("email", emailDb ?: email)
                                        prefs.apply()  // Make sure prefs are written BEFORE launching next activity

                                        // Pass important values via Intent
                                        val intent = Intent(this, LoadingActivity::class.java)
                                        intent.putExtra("mode", "login")
                                        intent.putExtra("role", role)
                                        intent.putExtra("studentId", studentId)
                                        intent.putExtra("grade_number", gradeNumber)
                                        startActivity(intent)
                                        finish()
                                    }
                                }

                            }
                        } else {
                            Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                            enableButtons()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Failed to load user role: ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        enableButtons()
                    }
            }
        }
    }

    private fun disableButtons() {
        login.isEnabled = false
        signup.isEnabled = false
    }

    private fun enableButtons() {
        login.isEnabled = true
        signup.isEnabled = true
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }
}