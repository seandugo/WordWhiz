package com.example.thesis_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LoadingActivity : ComponentActivity() {
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_loading)

        database = FirebaseDatabase.getInstance().reference

        // Disable back button
        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { }
            })

        Handler(Looper.getMainLooper()).postDelayed({
            val mode = intent.getStringExtra("mode")
            val role = intent.getStringExtra("role")
            val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)

            when (mode) {
                "login" -> {
                    when (role) {
                        "teacher" -> {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            val uid = currentUser?.uid

                            if (uid.isNullOrEmpty()) {
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            } else {
                                checkIntroStatus(uid)
                            }
                        }

                        "student" -> {
                            val studentId = intent.getStringExtra("studentId")
                                ?: prefs.getString("studentId", null)

                            if (studentId.isNullOrEmpty()) {
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            } else {
                                checkPretestStatus(studentId)
                            }
                        }

                        // ✅ NEW: Admin role redirect
                        "admin" -> {
                            val intent = Intent(this, AdminActivity::class.java)
                            startActivity(intent)
                            finish()
                        }

                        else -> {
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                    }
                }

                "signup" -> {
                    startActivity(Intent(this, SignupActivity::class.java))
                    finish()
                }

                "createAccount" -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }

                else -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
        }, 3000)
    }

    private fun checkPretestStatus(studentId: String) {
        database.child("users").child(studentId).child("pretestCompleted")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Handle both Boolean and String cases
                    val completed: Boolean = when (val value = snapshot.value) {
                        is Boolean -> value
                        is String -> value.equals("true", ignoreCase = true)
                        else -> false
                    }

                    val nextActivity = if (completed) StudentActivity::class.java
                    else PreAssessmentActivity::class.java

                    startActivity(Intent(this@LoadingActivity, nextActivity))
                    finish()
                }

                override fun onCancelled(error: DatabaseError) {
                    startActivity(Intent(this@LoadingActivity, PreAssessmentActivity::class.java))
                    finish()
                }
            })
    }

    private fun checkIntroStatus(teacherId: String) {
        database.child("users").child(teacherId).child("introCompleted")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val completed: Boolean = when (val value = snapshot.value) {
                        is Boolean -> value
                        is String -> value.equals("true", ignoreCase = true)
                        else -> false
                    }

                    val nextActivity = if (completed) TeacherActivity::class.java
                    else TeacherIntroActivity::class.java

                    startActivity(Intent(this@LoadingActivity, nextActivity))
                    finish()
                }

                override fun onCancelled(error: DatabaseError) {
                    startActivity(Intent(this@LoadingActivity, TeacherIntroActivity::class.java))
                    finish()
                }
            })
    }
}
