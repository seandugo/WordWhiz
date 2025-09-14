package com.example.thesis_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class LoadingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_loading)

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                }
            })

        Handler(Looper.getMainLooper()).postDelayed({
            val mode = intent.getStringExtra("mode")
            val role = intent.getStringExtra("role")
            val studentId = intent.getStringExtra("studentId")

            when (mode) {
                "login" -> {
                    if (role == "teacher") {
                        startActivity(Intent(this, TeacherActivity::class.java))
                    } else {
                        val intent = Intent(this, StudentActivity::class.java)
                        intent.putExtra("studentId", studentId)
                        startActivity(intent)
                    }
                    finish()
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
}
