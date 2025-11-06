package com.example.thesis_app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
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
import com.google.firebase.database.*

class StudentActivity : AppCompatActivity() {
    private var currentIndex = 0
    private var pendingFragment: Fragment? = null
    private var pendingIndex: Int = 0
    private var lastSuccessfulFragmentIndex: Int = 0

    // ✅ Firebase presence reference
    private var presenceRef: DatabaseReference? = null

    // 🆕 Firebase listener for class changes
    private var classRef: DatabaseReference? = null
    private var classListener: ValueEventListener? = null
    // 🧠 Used to detect actual class data changes (not first load)
    private var initialClassSnapshot: String? = null
    private var firstSnapshotLoaded = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // ✅ Initialize user presence tracking
        setupUserPresence()

        // 🆕 Start monitoring class data changes
        monitorClassChanges()

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

        val connectedRef = database.getReference(".info/connected")

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    presenceRef?.child("status")?.setValue("online")
                    presenceRef?.child("lastSeen")?.setValue(System.currentTimeMillis())
                    presenceRef?.child("status")?.onDisconnect()?.setValue("offline")
                    presenceRef?.child("lastSeen")?.onDisconnect()?.setValue(System.currentTimeMillis())
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🆕 Monitor class data changes
    // 🧠 Only trigger session expired if actual data inside "classes" changes
    private fun monitorClassChanges() {
        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null)

        // 🚧 Safety check — stop if prefs missing or invalid
        if (studentId.isNullOrEmpty()) {
            android.util.Log.w("StudentActivity", "⚠️ monitorClassChanges skipped — studentId not found in prefs")
            return
        }

        val database = FirebaseDatabase.getInstance()
        classRef = database.getReference("users/$studentId/classes")

        classListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentState = snapshot.value?.toString() ?: "empty"

                // 🧷 Store the initial state safely
                if (!firstSnapshotLoaded) {
                    initialClassSnapshot = currentState
                    firstSnapshotLoaded = true
                    android.util.Log.d("StudentActivity", "✅ Initial class snapshot set for $studentId: $currentState")
                    return
                }

                // 🧨 Only trigger if the data truly changes
                if (currentState != initialClassSnapshot) {
                    android.util.Log.w("StudentActivity", "⚠️ Class data changed for $studentId: $currentState")
                    showSessionExpiredDialog()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("StudentActivity", "❌ Class monitor cancelled: ${error.message}")
            }
        }

        try {
            classRef?.addValueEventListener(classListener!!)
        } catch (e: Exception) {
            android.util.Log.e("StudentActivity", "❌ Failed to attach listener: ${e.message}")
        }
    }

    // 🆕 Show session expired dialog
    private fun showSessionExpiredDialog() {
        runOnUiThread {
            if (!isFinishing) {
                AlertDialog.Builder(this)
                    .setTitle("Session Expired")
                    .setMessage("Your class information has changed. Please log in again.")
                    .setCancelable(false)
                    .setPositiveButton("OK") { _, _ ->
                        logoutAndRedirect()
                    }
                    .show()
            }
        }
    }

    // 🆕 Logout and redirect to LoginActivity
    private fun logoutAndRedirect() {
        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        prefs.edit().clear().apply()

        presenceRef?.child("status")?.setValue("offline")
        presenceRef?.child("lastSeen")?.setValue(System.currentTimeMillis())

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // ✅ Update online status when activity starts/resumes
    override fun onStart() {
        super.onStart()
        setUserOnline()
    }

    override fun onResume() {
        super.onResume()
        setUserOnline()
    }

    override fun onPause() {
        super.onPause()
        setUserOnline()
    }

    override fun onDestroy() {
        super.onDestroy()
        setUserOffline()

        // 🆕 Remove listener to prevent leaks
        classListener?.let { classRef?.removeEventListener(it) }
    }

    private fun setUserOnline() {
        presenceRef?.child("status")?.setValue("online")
        presenceRef?.child("lastSeen")?.setValue(System.currentTimeMillis())
    }

    private fun setUserOffline() {
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