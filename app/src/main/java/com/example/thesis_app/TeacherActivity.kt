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
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.children
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.thesis_app.ui.fragments.bottomsheets.SettingsBottomSheet
import com.example.thesis_app.ui.fragments.teacher.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class TeacherActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var bottomNavigationView: BottomNavigationView
    private var currentIndex = 0
    private var lastSuccessfulFragmentIndex = 0

    // ✅ Network monitoring components
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var networkSnackbar: Snackbar? = null
    private var overlayView: View? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.teacher)

        drawerLayout = findViewById(R.id.drawer_layout)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // ✅ Default fragment
        if (savedInstanceState == null) {
            replaceFragment(TeacherFragment(), 0)
            bottomNavigationView.selectedItemId = R.id.nav_overview
        }

        // ✅ Bottom navigation handling
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_overview -> replaceFragment(TeacherFragment(), 0)
                R.id.nav_classes -> replaceFragment(TeacherClassesFragment(), 1)
                R.id.nav_profile -> replaceFragment(TeacherProfileFragment(), 2)
                R.id.nav_settings -> {
                    val bottomSheet = SettingsBottomSheet()
                    bottomSheet.show(supportFragmentManager, "SettingsBottomSheet")
                }
            }
            true
        }

        // ✅ Back press confirmation
        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        showLogoutConfirmation()
                    }
                }
            })

        // ✅ Network monitoring
        setupNetworkMonitoring()
    }

    // ------------------------------
    // Network Monitoring (Dynamic)
    // ------------------------------
    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val rootView = findViewById(android.R.id.content) as View

        // Persistent Snackbar setup
        networkSnackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_INDEFINITE)
            .setAction("Retry") { checkCurrentFragment() }

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
                    showNetworkStatusSnackbar("No internet connection — app may not function properly.")
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
                            showNetworkStatusSnackbar("No internet connection — app may not function properly.")
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
            showNetworkStatusSnackbar("No internet connection — app may not function properly.")
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
        bottomNavigationView.menu.children.forEach { it.isEnabled = enable }
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
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_classes -> replaceFragment(TeacherClassesFragment(), 1)
            R.id.nav_profile -> replaceFragment(TeacherProfileFragment(), 2)
            R.id.nav_settings -> replaceFragment(TeacherSettingsFragment(), 3)
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun replaceFragment(fragment: Fragment, newIndex: Int) {
        if (newIndex == currentIndex) return
        val transaction = supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(
            if (newIndex > currentIndex) R.anim.slide_in_right else R.anim.slide_in_left,
            if (newIndex > currentIndex) R.anim.slide_out_left else R.anim.slide_out_right
        )
        transaction.replace(R.id.fragmentContainerView, fragment)
        transaction.commit()
        currentIndex = newIndex
        lastSuccessfulFragmentIndex = newIndex
    }

    private fun checkCurrentFragment() {
        when (lastSuccessfulFragmentIndex) {
            0 -> replaceFragment(TeacherFragment(), 0)
            1 -> replaceFragment(TeacherClassesFragment(), 1)
            2 -> replaceFragment(TeacherProfileFragment(), 2)
            else -> replaceFragment(TeacherFragment(), 0)
        }
    }

    fun navigateToOverview() {
        replaceFragment(TeacherFragment(), 0)
        bottomNavigationView.selectedItemId = R.id.nav_overview
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) } } catch (_: Exception) {}
    }
}
