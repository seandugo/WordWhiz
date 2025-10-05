package com.example.thesis_app

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.thesis_app.ui.fragments.bottomsheets.SettingsBottomSheet
import com.example.thesis_app.ui.fragments.spelling.SpellingFragment
import com.example.thesis_app.ui.fragments.student.DailySpellingFragment
import com.example.thesis_app.ui.fragments.student.LecturesFragment
import com.example.thesis_app.ui.fragments.student.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.FirebaseDatabase

class StudentActivity : AppCompatActivity() {
    private var currentIndex = 0 // track current tab index

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            replaceFragment(LecturesFragment(), 0) // default = Lectures
            bottomNav.selectedItemId = R.id.action_lectures
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_lectures -> replaceFragment(LecturesFragment(), 0)

                R.id.action_dictionary -> {
                    val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                    val studentId = prefs.getString("studentId", null)

                    if (!studentId.isNullOrEmpty()) {
                        val db = FirebaseDatabase.getInstance().reference
                        db.child("users").child(studentId).child("dailySpelling")
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val hasDailySpelling = when (val value = snapshot.value) {
                                    is Boolean -> value
                                    is String -> value.equals("true", ignoreCase = true)
                                    else -> false
                                }

                                if (hasDailySpelling) {
                                    replaceFragment(DailySpellingFragment(), 1)
                                } else {
                                    replaceFragment(SpellingFragment(), 1)
                                }
                            }
                            .addOnFailureListener {
                                replaceFragment(SpellingFragment(), 1)
                            }
                    } else {
                        replaceFragment(SpellingFragment(), 1)
                    }
                }

                R.id.action_profile -> replaceFragment(ProfileFragment(), 2)

                R.id.action_settings -> {
                    val bottomSheet = SettingsBottomSheet()
                    bottomSheet.show(supportFragmentManager, "SettingsBottomSheet")
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

    private fun replaceFragment(fragment: Fragment, newIndex: Int) {
        if (newIndex == currentIndex) return // prevent reloading same fragment

        val transaction = supportFragmentManager.beginTransaction()

        if (newIndex > currentIndex) {
            transaction.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        } else {
            transaction.setCustomAnimations(
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        }

        transaction.replace(R.id.fragmentContainerView, fragment)
        transaction.commit()

        currentIndex = newIndex
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }
}
