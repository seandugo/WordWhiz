package com.example.thesis_app

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.thesis_app.ui.fragments.bottomsheets.SettingsBottomSheet
import com.example.thesis_app.ui.fragments.nointernet.NoInternetFragment
import com.example.thesis_app.ui.fragments.spelling.SpellingFragment
import com.example.thesis_app.ui.fragments.student.DailySpellingFragment
import com.example.thesis_app.ui.fragments.student.LecturesFragment
import com.example.thesis_app.ui.fragments.student.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class StudentActivity : AppCompatActivity() {
    private var currentIndex = 0
    private var pendingFragment: Fragment? = null
    private var pendingIndex: Int = 0
    private var lastSuccessfulFragmentIndex: Int = 0

    // ✅ Firebase presence reference (now grouped)
    private var presenceRef: DatabaseReference? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // ✅ Initialize user presence tracking
        setupUserPresence()

        // Default fragment
        if (savedInstanceState == null) {
            checkAndReplaceFragment(LecturesFragment(), 0)
            bottomNav.selectedItemId = R.id.action_lectures
        }

        // Bottom navigation handling
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_lectures -> checkAndReplaceFragment(LecturesFragment(), 0)
                R.id.action_dictionary -> checkAndHandleDictionary()
                R.id.action_profile -> checkAndReplaceFragment(ProfileFragment(), 2)
                R.id.action_settings -> {
                    val bottomSheet = SettingsBottomSheet()
                    bottomSheet.show(supportFragmentManager, "SettingsBottomSheet")
                }
            }
            true
        }

        // Back press handler
        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmation()
                }
            })

        updateStreakAfterActivity()
    }

    // ✅ Setup presence tracking (status + lastSeen)
    private fun setupUserPresence() {
        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null) ?: return

        val database = FirebaseDatabase.getInstance()
        val presencePath = "users/$studentId/presence"
        presenceRef = database.getReference(presencePath)

        // Automatically go offline when disconnected
        presenceRef?.child("status")?.onDisconnect()?.setValue("offline")
        presenceRef?.child("lastSeen")?.onDisconnect()?.setValue(System.currentTimeMillis())

        // Mark as online when app starts
        presenceRef?.child("status")?.setValue("online")
        presenceRef?.child("lastSeen")?.setValue(System.currentTimeMillis())
    }

    // ✅ Update online status when activity starts
    override fun onStart() {
        super.onStart()
        presenceRef?.child("status")?.setValue("online")
        presenceRef?.child("lastSeen")?.setValue(System.currentTimeMillis())
    }

    // ✅ Update offline + last seen when activity stops
    override fun onStop() {
        super.onStop()
        presenceRef?.child("status")?.setValue("offline")
        presenceRef?.child("lastSeen")?.setValue(System.currentTimeMillis())
    }

    fun updateStreakAfterActivity() {
        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null)
        val userRef = FirebaseDatabase.getInstance()
            .getReference("users/$studentId/activityStreak")
        val today = java.time.LocalDate.now().toString()
        val yesterday = java.time.LocalDate.now().minusDays(1).toString()

        userRef.get().addOnSuccessListener { snapshot ->
            var streakCount = snapshot.child("streakCount").getValue(Int::class.java) ?: 0
            val lastActiveDate = snapshot.child("lastActiveDate").getValue(String::class.java)

            if (lastActiveDate != today) {
                if (lastActiveDate == yesterday) {
                    streakCount += 1
                } else {
                    streakCount = 1
                }
                userRef.child("streakCount").setValue(streakCount)
                userRef.child("lastActiveDate").setValue(today)
            }
        }
    }

    private fun checkAndReplaceFragment(fragment: Fragment, index: Int) {
        pendingFragment = fragment
        pendingIndex = index

        if (!isInternetAvailable()) {
            replaceFragment(NoInternetFragment(), 99)
        } else {
            replaceFragment(fragment, index)
        }
    }

    private fun checkAndHandleDictionary() {
        pendingIndex = 1

        if (!isInternetAvailable()) {
            replaceFragment(NoInternetFragment(), 99)
            return
        }

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
                    replaceFragment(NoInternetFragment(), 99)
                }
        } else {
            replaceFragment(SpellingFragment(), 1)
        }
    }

    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun replaceFragment(fragment: Fragment, newIndex: Int) {
        pendingFragment = fragment
        pendingIndex = newIndex
        if (newIndex == currentIndex) return

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

        if (newIndex != 99) {
            lastSuccessfulFragmentIndex = newIndex
        }
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
