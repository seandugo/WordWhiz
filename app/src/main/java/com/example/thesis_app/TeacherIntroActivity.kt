package com.example.thesis_app

import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class TeacherIntroActivity : AppCompatActivity() {
    private lateinit var progressBar: LinearProgressIndicator
    private var currentStep = 1
    private val totalSteps = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.teacher_intro)

        progressBar = findViewById(R.id.linearProgress)
        progressBar.max = 100
        updateProgress()
    }

    fun nextStep() {
        if (currentStep < totalSteps) {
            currentStep++
            updateProgress()
        } else {
            markIntroCompleted()
        }
    }

    fun previousStep() {
        if (currentStep > 1) {
            currentStep--
            updateProgress()
        }
    }

    private fun updateProgress() {
        val target = ((currentStep - 1) * 100) / (totalSteps - 1)
        progressBar.setProgressCompat(target, true)
    }

    fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit? All unsaved data will be lost.")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun markIntroCompleted() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        if (!uid.isNullOrEmpty()) {
            val dbRef = FirebaseDatabase.getInstance().reference
            dbRef.child("users").child(uid).child("introCompleted").setValue(true)
        }
    }
}
