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

class LoginActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        window.navigationBarColor = getColor(R.color.my_nav_color)
        window.statusBarColor = getColor(R.color.my_nav_color)

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
                        val userId = task.result.user?.uid ?: return@addOnCompleteListener
                        fetchUserData(userId)
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

    private fun fetchUserData(userId: String) {
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val role = snapshot.child("role").value?.toString()
                    val emailDb = snapshot.child("email").value?.toString()

                    val intent = Intent(this, LoadingActivity::class.java)
                    intent.putExtra("mode", "login")
                    intent.putExtra("role", role)
                    intent.putExtra("email", emailDb)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch user data: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStart() {
        super.onStart()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val databaseRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

            databaseRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val role = snapshot.child("role").getValue(String::class.java)
                    when (role) {
                        "teacher" -> {
                            val intent = Intent(this, LoadingActivity::class.java)
                            intent.putExtra("mode", "login")
                            startActivity(intent)
                        }
                        "student" -> {
                            val intent = Intent(this, LoadingActivity::class.java)
                            intent.putExtra("mode", "login")
                            startActivity(intent)
                        }
                        else -> {
                            Toast.makeText(this, "Role not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load user role: ${it.message}", Toast.LENGTH_SHORT).show()
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
