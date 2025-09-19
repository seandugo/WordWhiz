package com.example.thesis_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.google.firebase.database.*

class LoadingActivity : ComponentActivity() {
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_loading)

        database = FirebaseDatabase.getInstance().reference

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                }
            })

        Handler(Looper.getMainLooper()).postDelayed({
            val mode = intent.getStringExtra("mode")
            val role = intent.getStringExtra("role")
            val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
            when (mode) {
                "login" -> {
                    if (role == "teacher") {
                        startActivity(Intent(this, TeacherActivity::class.java))
                        finish()
                    } else {
                        val studentId = intent.getStringExtra("studentId") ?: ""
                        val gradeNumber = intent.getIntExtra("grade_number", 0)  // guaranteed to be correct now

                        if (studentId.isEmpty() || gradeNumber == 0) {
                            // fallback if missing
                            startActivity(Intent(this, PreAssessmentActivity::class.java))
                            finish()
                            return@postDelayed
                        }
                        checkPretestStatus(studentId)
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
            }
        }, 3000)
    }

    private fun checkPretestStatus(studentId: String?) {
        if (studentId.isNullOrEmpty()) {
            // fallback → go to PreAssessment
            startActivity(Intent(this, PreAssessmentActivity::class.java))
            finish()
            return
        }

        database.child("users").child(studentId).child("pretestCompleted")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val completed = snapshot.getValue(Boolean::class.java) ?: false

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

}
