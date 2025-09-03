package com.example.thesis_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.google.firebase.auth.FirebaseAuth

class StudentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.teacher) // Your login layout XML
        window.navigationBarColor = getColor(R.color.my_nav_color)
        window.statusBarColor = getColor(R.color.my_nav_color)

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    FirebaseAuth.getInstance().signOut()
                    finish()
                }
            })
    }
}