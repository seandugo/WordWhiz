package com.example.thesis_app

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class AboutUsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_us)

        // Toolbar setup
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        toolbar.setNavigationOnClickListener { onBackPressed() }

        // Collapsible description
        val descriptionHeader = findViewById<LinearLayout>(R.id.descriptionHeader)
        val description = findViewById<TextView>(R.id.appDescription)
        val arrow = findViewById<ImageView>(R.id.arrowIcon)

        descriptionHeader.setOnClickListener {
            if (description.visibility == View.GONE) {
                description.visibility = View.VISIBLE
                arrow.animate().rotation(180f).setDuration(300).start()
            } else {
                description.visibility = View.GONE
                arrow.animate().rotation(0f).setDuration(300).start()
            }
        }
    }
}