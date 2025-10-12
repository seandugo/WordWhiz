package com.example.thesis_app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.activity.OnBackPressedCallback
import android.view.MenuItem
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.thesis_app.ui.fragments.bottomsheets.SettingsBottomSheet
import com.example.thesis_app.ui.fragments.teacher.TeacherClassesFragment
import com.example.thesis_app.ui.fragments.teacher.TeacherProfileFragment
import com.example.thesis_app.ui.fragments.teacher.TeacherSettingsFragment
import com.example.thesis_app.ui.fragments.teacher.TeacherFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class TeacherActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var bottomNavigationView: BottomNavigationView

    private var currentIndex = 0 // Track current fragment index

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.teacher)

        drawerLayout = findViewById(R.id.drawer_layout)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            replaceFragment(TeacherFragment(), 0)
            bottomNavigationView.selectedItemId = R.id.nav_overview
        }

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
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_classes -> replaceFragment(TeacherFragment(), 1)
            R.id.nav_profile -> replaceFragment(TeacherProfileFragment(), 2)
            R.id.nav_settings -> replaceFragment(TeacherSettingsFragment(), 3)
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun navigateToOverview() {
        replaceFragment(TeacherFragment(), 0)
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
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                prefs.edit().clear().apply()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
}