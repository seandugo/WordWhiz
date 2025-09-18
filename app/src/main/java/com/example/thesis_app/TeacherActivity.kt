package com.example.thesis_app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.widget.Toolbar
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.activity.OnBackPressedCallback
import android.view.MenuItem
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.thesis_app.ui.fragments.profile.TeacherProfileFragment
import com.example.thesis_app.ui.fragments.student.SettingsFragment
import com.example.thesis_app.ui.fragments.student.TeacherSettingsFragment
import com.example.thesis_app.ui.fragments.teacher.TeacherFragment
import com.google.firebase.auth.FirebaseAuth

class TeacherActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.teacher)

        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        toggle.drawerArrowDrawable.color = getColor(android.R.color.black)

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
            R.id.nav_classes -> {
                replaceFragment(TeacherFragment())
            }
            R.id.nav_profile -> {
                replaceFragment(TeacherProfileFragment())
            }
            R.id.nav_settings -> {
                replaceFragment(TeacherSettingsFragment())
            }
            R.id.nav_logout -> {
                showLogoutConfirmation()
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, fragment)
            .commit()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                FirebaseAuth.getInstance().signOut()

                // Clear any stored user info in SharedPreferences
                val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                prefs.edit().clear().apply()

                // Go to LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
}