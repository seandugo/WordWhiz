package com.example.thesis_app

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.database.FirebaseDatabase

class SignupActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbarTitle: MaterialTextView
    private var currentStep = 1
    private val totalSteps = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_signup)

        progressBar = findViewById(R.id.pageProgressBar)
        toolbarTitle = findViewById(R.id.toolbar_title)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Initialize first title
        updateToolbarTitle()

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmation()
                }
            })
    }

    fun nextStep() {
        if (currentStep < totalSteps) {
            currentStep++
            updateProgress()
            updateToolbarTitle()
        }
    }

    fun previousStep() {
        if (currentStep > 1) {
            currentStep--
            updateProgress()
            updateToolbarTitle()
        }
    }

    private fun updateProgress() {
        val target = ((currentStep - 1) * 100) / (totalSteps - 1)
        ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, target).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun updateToolbarTitle() {
        val title = when (currentStep) {
            1 -> "1. Select User Type"
            2 -> "2. Fill Up Details"
            3 -> "3. Accept Terms and Conditions"
            else -> ""
        }
        toolbarTitle.text = title
    }

    fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Signup")
            .setMessage("Are you sure you want to cancel signup? All unsaved data will be removed.")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }
}
