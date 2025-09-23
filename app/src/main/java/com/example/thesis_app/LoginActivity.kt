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
                            prefs.putString("studentId", studentId)
                            prefs.apply()
                        }

                        prefs.apply()

                        val intent = Intent(this, LoadingActivity::class.java)
                        intent.putExtra("mode", "login")
                        intent.putExtra("role", role)
                        if (role == "student") {
                            intent.putExtra("studentId", child.child("studentID").getValue(String::class.java))
                        }
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
                val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                val savedRole = prefs.getString("role", null)
                val savedStudentId = prefs.getString("studentId", null)

                if (savedRole != null) {
                    // Fast auto-login with cached prefs
                    val intent = Intent(this, LoadingActivity::class.java)
                    intent.putExtra("mode", "login")
                    intent.putExtra("role", savedRole)
                    if (savedRole == "student") {
                        intent.putExtra("studentId", savedStudentId)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    // Fetch fresh from DB
                    val dbRef = FirebaseDatabase.getInstance().getReference("users")
                    dbRef.orderByChild("email").equalTo(email).get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                for (child in snapshot.children) {
                                    val role = child.child("role").getValue(String::class.java)
                                    if (!role.isNullOrEmpty()) {
                                        val editor = prefs.edit()
                                        editor.putString("role", role)
                                        editor.putString("email", email)
                                        if (role == "student") {
                                            val studentId = child.child("studentID")
                                                .getValue(String::class.java)
                                            editor.putString("studentId", studentId)
                                        }
                                        editor.apply()

                                        val intent = Intent(this, LoadingActivity::class.java)
                                        intent.putExtra("mode", "login")
                                        intent.putExtra("role", role)
                                        if (role == "student") {
                                            intent.putExtra("studentId",
                                                child.child("studentID")
                                                    .getValue(String::class.java)
                                            )
                                        }
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                            } else {
                                Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
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