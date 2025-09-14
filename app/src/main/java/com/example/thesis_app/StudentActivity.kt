package com.example.thesis_app

import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.thesis_app.ui.fragments.student.DictionaryFragment
import com.example.thesis_app.ui.fragments.student.LecturesFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.bottomnavigation.BottomNavigationView

class StudentActivity : AppCompatActivity() {

    private lateinit var toolbarTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student)
        // Setup bottom navigation
        toolbarTitle = findViewById(R.id.toolbar_title)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val studentId = intent.getStringExtra("studentId") ?: ""
        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        prefs.edit().putString("studentId", studentId).apply()
        // Load default fragment
        if (savedInstanceState == null) {
            replaceFragment(LecturesFragment()) // change to your default fragment
        }

        // Handle bottom nav item clicks
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_lectures -> {
                    replaceFragment(LecturesFragment())
                    toolbarTitle.text = getString(R.string.app_name).uppercase()
                }
                R.id.action_dictionary -> {
                    replaceFragment(DictionaryFragment())
                    toolbarTitle.text = "Dictionary"
                }
                R.id.action_profile -> {
                    toolbarTitle.text = "Profile"
                }
                R.id.action_settings -> {
                    toolbarTitle.text = "Settings"
                }
            }
            true
        }

        // Handle back press with confirmation
        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmation()
                }
            })
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, fragment)
            .commit()
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
}
