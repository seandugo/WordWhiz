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

class LoginActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        val login = findViewById<Button>(R.id.LoginButton)
        val signup = findViewById<TextView>(R.id.textView9)
        val teacherEmail = findViewById<TextInputEditText>(R.id.editEmail)
        val teacherPassword = findViewById<TextInputEditText>(R.id.Password)

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmation()
                }
            })

        login.setOnClickListener {
            login.isEnabled = false // turns gray

            val email = teacherEmail.text.toString().trim()
            val password = teacherPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter teacher email and password", Toast.LENGTH_SHORT).show()
                login.isEnabled = true // enable back
                return@setOnClickListener
            }

            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    Handler(Looper.getMainLooper()).postDelayed({
                        login.isEnabled = true
                    }, 1000)

                    if (task.isSuccessful) {
                        fetchUserData(email) // pass email instead of userId
                        teacherEmail.text?.clear()
                        teacherPassword.text?.clear()
                    } else {
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        signup.setOnClickListener {
            val intent = Intent(this, LoadingActivity::class.java)
            intent.putExtra("mode", "signup")
            startActivity(intent)
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
                        val studentId = child.child("studentID").getValue(String::class.java) // ✅ fetch studentId

                        // 🔹 Save user info into SharedPreferences
                        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("role", role)
                            putString("email", emailDb)
                            putString("studentId", studentId)   // ✅ persist studentId
                            apply()
                        }

                        // 🔹 Proceed to LoadingActivity
                        val intent = Intent(this, LoadingActivity::class.java)
                        intent.putExtra("mode", "login")
                        intent.putExtra("role", role)
                        intent.putExtra("email", emailDb)
                        intent.putExtra("studentId", studentId)
                        startActivity(intent)
                        finish()
                        break
                    }
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Log.e("FirebaseError", "Failed to fetch user data: ${it.message}")
            }
    }

    override fun onStart() {
        super.onStart()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val email = currentUser.email
            if (email != null) {
                val dbRef = FirebaseDatabase.getInstance().getReference("users")
                dbRef.orderByChild("email").equalTo(email).get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            for (child in snapshot.children) {
                                val name = child.child("name").getValue(String::class.java) // ✅ fetch name
                                val emailDb = child.child("email").getValue(String::class.java)
                                val role = child.child("role").getValue(String::class.java)

                                if (!role.isNullOrEmpty()) {
                                    val intent = Intent(this, LoadingActivity::class.java)
                                    intent.putExtra("mode", "login")
                                    intent.putExtra("role", role)
                                    intent.putExtra("email", emailDb)
                                    intent.putExtra("name", name)

                                    // Only add studentId if the role is "student"
                                    if (role == "student") {
                                        val studentId = child.child("studentID").getValue(String::class.java)

                                        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                                        prefs.edit().apply {
                                            putString("role", role)
                                            putString("email", emailDb)
                                            putString("studentId", studentId)   // ✅ save again here
                                            apply()
                                        }

                                        intent.putExtra("studentId", studentId)
                                    }

                                    startActivity(intent)
                                    finish()
                                }

                            }
                        } else {
                            Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Failed to load user role: ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
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
