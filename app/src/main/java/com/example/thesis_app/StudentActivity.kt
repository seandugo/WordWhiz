package com.example.thesis_app

import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class StudentActivity : AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student) // Your login layout XML
        window.navigationBarColor = getColor(R.color.my_nav_color)
        window.statusBarColor = getColor(R.color.my_nav_color)

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                        showExitConfirmation()
                    }
            })
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