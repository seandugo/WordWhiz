package com.example.thesis_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import androidx.core.content.ContextCompat
import android.widget.TextView

class QuizDetailActivity : AppCompatActivity() {

    private lateinit var appBarLayout: AppBarLayout
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var toolbar: MaterialToolbar

    private lateinit var nameTextExpanded: TextView
    private lateinit var subtitleQuiz: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quiz_review)

        // AppBar / Toolbar
        appBarLayout = findViewById(R.id.appbar)
        collapsingToolbar = findViewById(R.id.collapsingToolbar)
        toolbar = findViewById(R.id.toolbar)

        // Header Text
        nameTextExpanded = findViewById(R.id.nameTextExpanded)
        subtitleQuiz = findViewById(R.id.subtitleQuiz)

        // Back button
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Optional: set dynamic title from intent
        val quizTitle = intent.getStringExtra("levelName") ?: "Quiz Details"
        nameTextExpanded.text = quizTitle
        collapsingToolbar.title = "" // Title is hidden; we show it in expanded text

        // Scroll listener for collapsing behavior
        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            val isCollapsed = totalScrollRange + verticalOffset <= 0

            if (isCollapsed) {
                toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.black))
            } else {
                toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.white))
            }
        })
    }
}
