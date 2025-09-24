package com.example.thesis_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.appcompat.widget.Toolbar

class StudentProgressActivity : AppCompatActivity() {

    private lateinit var studentName: TextView
    private lateinit var studentClass: TextView
    private lateinit var studentCode: TextView
    private lateinit var progressText: TextView
    private lateinit var achievementText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student_progress)

        // ✅ Setup Toolbar with back button
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true) // show back arrow
            setDisplayShowTitleEnabled(false) // keep title from XML
        }

        // ✅ Initialize views
        studentName = findViewById(R.id.headerStudentName)
        studentClass = findViewById(R.id.headerStudentClass)
        studentCode = findViewById(R.id.headerStudentCode)
        progressText = findViewById(R.id.progressText)
        achievementText = findViewById(R.id.achievementText)

        // ✅ Get intent extras safely
        val name = intent.getStringExtra(EXTRA_NAME) ?: "Unknown"
        val className = intent.getStringExtra(EXTRA_CLASS) ?: "N/A"
        val code = intent.getStringExtra(EXTRA_CODE) ?: "N/A"
        val progress = intent.getStringExtra(EXTRA_PROGRESS) ?: "0%"
        val achievement = intent.getStringExtra(EXTRA_ACHIEVEMENT) ?: "None yet"

        // ✅ Bind data
        studentName.text = name
        studentClass.text = "Class: $className"
        studentCode.text = "Code: $code"
        progressText.text = "Progress: $progress"
        achievementText.text = "Achievement: $achievement"
    }

    // ✅ Handle toolbar back button
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_CLASS = "extra_class"
        private const val EXTRA_CODE = "extra_code"
        private const val EXTRA_PROGRESS = "extra_progress"
        private const val EXTRA_ACHIEVEMENT = "extra_achievement"

        fun start(
            context: Context,
            name: String,
            className: String,
            code: String,
            progress: String,
            achievement: String
        ) {
            val intent = Intent(context, StudentProgressActivity::class.java).apply {
                putExtra(EXTRA_NAME, name)
                putExtra(EXTRA_CLASS, className)
                putExtra(EXTRA_CODE, code)
                putExtra(EXTRA_PROGRESS, progress)
                putExtra(EXTRA_ACHIEVEMENT, achievement)
            }
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(context as AppCompatActivity)
            context.startActivity(intent, options.toBundle())
        }
    }

//    override fun finish() {
//        super.finish()
//        // ✅ Reverse slide transition when going back
//        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
//    }
}
