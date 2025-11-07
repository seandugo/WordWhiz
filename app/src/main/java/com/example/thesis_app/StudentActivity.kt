package com.example.thesis_app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.example.thesis_app.ui.fragments.bottomsheets.SettingsBottomSheet
import com.example.thesis_app.ui.fragments.spelling.SpellingFragment
import com.example.thesis_app.ui.fragments.student.DailySpellingFragment
import com.example.thesis_app.ui.fragments.student.LecturesFragment
import com.example.thesis_app.ui.fragments.student.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*

class StudentActivity : AppCompatActivity() {
    private var currentIndex = 0
    private var lastSuccessfulFragmentIndex = 0

    private var presenceRef: DatabaseReference? = null
    private var classRef: DatabaseReference? = null
    private var classListener: ValueEventListener? = null
    private var initialClassSnapshot: String? = null
    private var firstSnapshotLoaded = false

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var networkSnackbar: Snackbar? = null
    private lateinit var bottomNav: BottomNavigationView
    private var overlayView: View? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student)

        bottomNav = findViewById(R.id.bottom_navigation)
        setupUserPresence()
        monitorClassChanges()
        setupNetworkMonitoring()

        if (savedInstanceState == null) {
            replaceFragment(LecturesFragment(), 0)
            bottomNav.selectedItemId = R.id.action_lectures
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_lectures -> replaceFragment(LecturesFragment(), 0)
                R.id.action_dictionary -> checkAndHandleDictionary()
                R.id.action_profile -> replaceFragment(ProfileFragment(), 2)
                R.id.action_settings -> SettingsBottomSheet()
                    .show(supportFragmentManager, "SettingsBottomSheet")
            }
            true
        }

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmation()
                }
            })

        updateStreakAfterActivity()
    }

    // ✅ Presence
    private fun setupUserPresence() {
        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null) ?: return
        val db = FirebaseDatabase.getInstance()
        presenceRef = db.getReference("users/$studentId/presence")

        val connectedRef = db.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.getValue(Boolean::class.java) == true) {
                    presenceRef?.child("status")?.setValue("online")
                    presenceRef?.child("lastSeen")?.setValue(System.currentTimeMillis())
                    presenceRef?.child("status")?.onDisconnect()?.setValue("offline")
                    presenceRef?.child("lastSeen")?.onDisconnect()?.setValue(System.currentTimeMillis())
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ✅ Class Change Monitor
    private fun monitorClassChanges() {
        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null) ?: return
        val db = FirebaseDatabase.getInstance()
        classRef = db.getReference("users/$studentId/classes")

        classListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentState = snapshot.value?.toString() ?: "empty"
                if (!firstSnapshotLoaded) {
                    initialClassSnapshot = currentState
                    firstSnapshotLoaded = true
                    return
                }
                if (currentState != initialClassSnapshot) showSessionExpiredDialog()
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        classRef?.addValueEventListener(classListener!!)
    }

    private fun showSessionExpiredDialog() {
        runOnUiThread {
            if (!isFinishing) {
                AlertDialog.Builder(this)
                    .setTitle("Session Expired")
                    .setMessage("Your class information has changed. Please log in again.")
                    .setCancelable(false)
                    .setPositiveButton("OK") { _, _ -> logoutAndRedirect() }
                    .show()
            }
        }
    }

    private fun logoutAndRedirect() {
        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        prefs.edit().clear().apply()
        presenceRef?.child("status")?.setValue("offline")
        presenceRef?.child("lastSeen")?.setValue(System.currentTimeMillis())
        startActivity(Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    override fun onStart() { super.onStart(); setUserOnline() }
    override fun onResume() { super.onResume(); setUserOnline() }
    override fun onPause() { super.onPause(); setUserOnline() }
    override fun onDestroy() {
        super.onDestroy()
        setUserOffline()
        classListener?.let { classRef?.removeEventListener(it) }
        try { networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) } } catch (_: Exception) {}
    }

    private fun setUserOnline() {
        presenceRef?.child("status")?.setValue("online")
        presenceRef?.child("lastSeen")?.setValue(System.currentTimeMillis())
    }
    private fun setUserOffline() {
        presenceRef?.child("status")?.setValue("offline")
        presenceRef?.child("lastSeen")?.setValue(System.currentTimeMillis())
    }

    // ------------------------------
    // Dynamic Network Monitoring
    // ------------------------------
    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val rootView = findViewById(android.R.id.content) as View

        networkSnackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_INDEFINITE)
            .setAction("Retry") { checkAndHandleDictionary() }

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    dismissNetworkSnackbar()
                    hideLoadingOverlay()
                    enableNavigation(true)
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    showNetworkStatusSnackbar("No internet connection — the app may not function properly.")
                    showLoadingOverlay()
                    enableNavigation(false)
                }
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                runOnUiThread {
                    when {
                        !hasInternet -> {
                            showNetworkStatusSnackbar("No internet connection — the app may not function properly.")
                            showLoadingOverlay()
                            enableNavigation(false)
                        }
                        !isValidated -> {
                            showNetworkStatusSnackbar("Unstable internet — some features may not work properly.")
                            showLoadingOverlay()
                            enableNavigation(false)
                        }
                        else -> {
                            dismissNetworkSnackbar()
                            hideLoadingOverlay()
                            enableNavigation(true)
                        }
                    }
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager?.registerNetworkCallback(request, networkCallback!!)

        if (!isInternetAvailable()) {
            showNetworkStatusSnackbar("No internet connection — the app may not function properly.")
            showLoadingOverlay()
            enableNavigation(false)
        }
    }

    private fun showNetworkStatusSnackbar(message: String) {
        networkSnackbar?.setText(message)
        if (networkSnackbar?.isShown != true) networkSnackbar?.show()
    }

    private fun dismissNetworkSnackbar() {
        networkSnackbar?.dismiss()
    }

    private fun showLoadingOverlay() {
        if (overlayView == null) {
            val parent = findViewById<FrameLayout>(android.R.id.content)
            overlayView = LayoutInflater.from(this)
                .inflate(R.layout.loading_overlay, parent, false)
            parent.addView(overlayView)
        }
        overlayView?.visibility = View.VISIBLE
    }

    private fun hideLoadingOverlay() {
        overlayView?.visibility = View.GONE
    }

    private fun enableNavigation(enable: Boolean) {
        bottomNav.menu.children.forEach { it.isEnabled = enable }
    }

    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ------------------------------
    // Navigation + UI
    // ------------------------------
    private fun replaceFragment(fragment: Fragment, index: Int) {
        if (index == currentIndex) return
        val t = supportFragmentManager.beginTransaction()
        t.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
        t.replace(R.id.fragmentContainerView, fragment)
        t.commit()
        currentIndex = index
        lastSuccessfulFragmentIndex = index
    }

    private fun checkAndHandleDictionary() {
        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null)
        if (!studentId.isNullOrEmpty()) {
            FirebaseDatabase.getInstance().reference
                .child("users/$studentId/dailySpelling")
                .get()
                .addOnSuccessListener {
                    val hasDailySpelling = it.getValue(Boolean::class.java) ?: false
                    replaceFragment(
                        if (hasDailySpelling) DailySpellingFragment() else SpellingFragment(), 1
                    )
                }
                .addOnFailureListener {
                    showNetworkStatusSnackbar("Unstable internet — please try again later.")
                    showLoadingOverlay()
                    enableNavigation(false)
                }
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

    // ✅ Update streak system
    fun updateStreakAfterActivity() {
        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null)
        val ref = FirebaseDatabase.getInstance().getReference("users/$studentId/activityStreak")
        val today = java.time.LocalDate.now().toString()
        val yesterday = java.time.LocalDate.now().minusDays(1).toString()

        ref.get().addOnSuccessListener {
            var streak = it.child("streakCount").getValue(Int::class.java) ?: 0
            val last = it.child("lastActiveDate").getValue(String::class.java)
            if (last != today) {
                streak = if (last == yesterday) streak + 1 else 1
                ref.child("streakCount").setValue(streak)
                ref.child("lastActiveDate").setValue(today)
            }
        }
    }
}
