package com.example.thesis_app

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.thesis_app.ui.fragments.student.DictionaryFragment
import com.example.thesis_app.ui.fragments.student.LecturesFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.bottomnavigation.BottomNavigationView

class StudentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student)
        // Setup bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Load default fragment
        if (savedInstanceState == null) {
            replaceFragment(LecturesFragment()) // change to your default fragment
        }

        // Handle bottom nav item clicks
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_lectures -> replaceFragment(LecturesFragment())
                R.id.action_dictionary -> replaceFragment(DictionaryFragment())
                R.id.action_profile -> null
                R.id.action_settings -> null
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
