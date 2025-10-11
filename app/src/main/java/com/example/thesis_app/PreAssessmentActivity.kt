package com.example.thesis_app

import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.database.FirebaseDatabase

class PreAssessmentActivity : AppCompatActivity() {
    private lateinit var progressBar: LinearProgressIndicator
    private var currentStep = 1
    private val totalSteps = 5
    private var studentId: String? = null  // ✅ store studentId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pre_test)

        // ✅ Get studentId from intent
        studentId = intent.getStringExtra("studentId")

        progressBar = findViewById(R.id.linearProgress)
        progressBar.max = 100
        updateProgress()
    }

    fun nextStep() {
        if (currentStep < totalSteps) {
            currentStep++
            updateProgress()
        } else {
            // ✅ If last step, mark as completed in Firebase
            markPretestCompleted()
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

    private fun markPretestCompleted() {
        val id = studentId ?: getSharedPreferences("USER_PREFS", MODE_PRIVATE)
            .getString("studentId", null)

        if (!id.isNullOrEmpty()) {
            val dbRef = FirebaseDatabase.getInstance().reference
            dbRef.child("users").child(id).child("pretestCompleted").setValue(true)
        }
    }
}