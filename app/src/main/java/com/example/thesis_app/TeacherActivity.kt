package com.example.thesis_app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.thesis_app.ui.fragments.bottomsheets.SettingsBottomSheet
import com.example.thesis_app.ui.fragments.nointernet.NoInternetFragment
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
    private var pendingFragment: Fragment? = null
    private var pendingIndex: Int = 0

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 3000L // every 3 seconds

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.teacher)

        drawerLayout = findViewById(R.id.drawer_layout)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            checkAndReplaceFragment(TeacherFragment(), 0)
            bottomNavigationView.selectedItemId = R.id.nav_overview
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_overview -> checkAndReplaceFragment(TeacherFragment(), 0)
                R.id.nav_classes -> checkAndReplaceFragment(TeacherClassesFragment(), 1)
                R.id.nav_profile -> checkAndReplaceFragment(TeacherProfileFragment(), 2)
                R.id.nav_settings -> {
                    val bottomSheet = SettingsBottomSheet()
                    bottomSheet.show(supportFragmentManager, "SettingsBottomSheet")
                }
            }
            true
        }

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

        startInternetMonitor()
    }

    // ✅ Internet check same as StudentActivity
    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    // ✅ Handles auto-check and fragment display
    private fun checkAndReplaceFragment(fragment: Fragment, index: Int) {
        pendingFragment = fragment
        pendingIndex = index

        if (!isInternetAvailable()) {
            replaceFragment(NoInternetFragment(), 99)
        } else {
            replaceFragment(fragment, index)
        }
    }

    // ✅ Retry triggered from NoInternetFragment (same as StudentActivity)
    fun retry() {
        val rootView: View = findViewById(android.R.id.content)
        val retrySnackbar = Snackbar.make(rootView, "Retrying...", Snackbar.LENGTH_INDEFINITE)
        retrySnackbar.show()

        Handler(Looper.getMainLooper()).postDelayed({
            retrySnackbar.dismiss()

            if (isInternetAvailable()) {
                when (lastSuccessfulFragmentIndex) {
                    0 -> replaceFragment(TeacherFragment(), 0)
                    1 -> replaceFragment(TeacherClassesFragment(), 1)
                    2 -> replaceFragment(TeacherProfileFragment(), 2)
                    else -> replaceFragment(TeacherFragment(), 0)
                }
            } else {
                replaceFragment(NoInternetFragment(), 99)
                Snackbar.make(rootView, "No internet. Try again.", Snackbar.LENGTH_SHORT).show()
            }
        }, 3000)
    }

    // ✅ Auto monitor if internet drops (kept from your version)
    private fun startInternetMonitor() {
        handler.post(object : Runnable {
            override fun run() {
                if (!isInternetAvailable()) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerView, NoInternetFragment())
                        .commitAllowingStateLoss()
                }
                handler.postDelayed(this, checkInterval)
            }
        })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_classes -> checkAndReplaceFragment(TeacherClassesFragment(), 1)
            R.id.nav_profile -> checkAndReplaceFragment(TeacherProfileFragment(), 2)
            R.id.nav_settings -> checkAndReplaceFragment(TeacherSettingsFragment(), 3)
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun navigateToOverview() {
        checkAndReplaceFragment(TeacherFragment(), 0)
        bottomNavigationView.selectedItemId = R.id.nav_overview
    }

    private fun replaceFragment(fragment: Fragment, newIndex: Int) {
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

        // ✅ Track last successful fragment
        if (newIndex != 99) {
            lastSuccessfulFragmentIndex = newIndex
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
}
