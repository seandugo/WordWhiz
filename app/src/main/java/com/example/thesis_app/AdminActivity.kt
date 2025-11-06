package com.example.thesis_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var studentCount: TextView
    private lateinit var teacherCount: TextView
    private lateinit var classCount: TextView
    private lateinit var lecturesCount: TextView

    private var usersListener: ValueEventListener? = null
    private var classesListener: ValueEventListener? = null
    private var quizzesListener: ValueEventListener? = null

    private val TAG = "🔥 AdminLiveStats"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        database = FirebaseDatabase.getInstance().reference

        studentCount = findViewById(R.id.studentCount)
        teacherCount = findViewById(R.id.teacherCount)
        classCount = findViewById(R.id.classCount)
        lecturesCount = findViewById(R.id.lecturesCount)

        val toolbar = findViewById<MaterialToolbar>(R.id.adminToolbar)
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor))
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.black))

        findViewById<MaterialButton>(R.id.manageLecturesBtn).setOnClickListener {
            val intent = Intent(this, AdminManageLecturesActivity::class.java)
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.logoutBtn).setOnClickListener {
            confirmLogout()
        }

        // ✅ Intercept physical back button — confirm logout
        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    confirmLogout()
                }
            })

        loadStats()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Log Out & Exit")
            .setMessage("Are you sure you want to log out and exit the admin panel?")
            .setPositiveButton("Yes") { _, _ -> performLogout() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun performLogout() {
        try {
            // ✅ Remove listeners before signing out
            try {
                usersListener?.let {
                    database.child("users").removeEventListener(it)
                    usersListener = null
                }
                classesListener?.let {
                    database.child("classes").removeEventListener(it)
                    classesListener = null
                }
                quizzesListener?.let {
                    database.child("quizzes").removeEventListener(it)
                    quizzesListener = null
                }
                Log.d(TAG, "🧹 Listeners detached before logout.")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Listener cleanup failed: ${e.message}")
            }

            // ✅ Delay slightly so Firebase detaches fully before signout
            Handler(Looper.getMainLooper()).postDelayed({
                FirebaseAuth.getInstance().signOut()
                Log.d(TAG, "✅ Admin successfully signed out.")

                // ✅ Clear session data
                val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE).edit()
                prefs.clear()
                prefs.apply()

                // ✅ Redirect to login
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }, 300)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during logout: ${e.message}")
            Toast.makeText(this, "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadStats() {
        // ==========================
        // 👩‍🎓 USERS
        // ==========================
        usersListener = database.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    studentCount.text = "0"
                    teacherCount.text = "0"
                    Log.w(TAG, "⚠️ /users node empty.")
                    return
                }

                var students = 0L
                var teachers = 0L

                for (userSnap in snapshot.children) {
                    val role = userSnap.child("role").getValue(String::class.java)
                    when (role?.lowercase()) {
                        "student" -> students++
                        "teacher" -> teachers++
                    }
                }

                studentCount.text = students.toString()
                teacherCount.text = teachers.toString()
                Log.d(TAG, "✅ Live Users: Students=$students, Teachers=$teachers")
            }

            override fun onCancelled(error: DatabaseError) {
                // Ignore harmless permission denied (happens on logout)
                if (error.code != DatabaseError.PERMISSION_DENIED) {
                    Log.e(TAG, "❌ Users listener cancelled: ${error.message}")
                }
            }
        })

        // ==========================
        // 🏫 CLASSES
        // ==========================
        classesListener = database.child("classes").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    classCount.text = "0"
                    Log.w(TAG, "⚠️ /classes empty.")
                    return
                }
                val total = snapshot.childrenCount
                classCount.text = total.toString()
                Log.d(TAG, "🏫 Live Classes: $total")
            }

            override fun onCancelled(error: DatabaseError) {
                if (error.code != DatabaseError.PERMISSION_DENIED) {
                    Log.e(TAG, "❌ Classes listener cancelled: ${error.message}")
                }
            }
        })

        // ==========================
        // 📚 QUIZZES / LECTURES
        // ==========================
        quizzesListener = database.child("quizzes").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    lecturesCount.text = "0"
                    Log.w(TAG, "⚠️ /quizzes empty.")
                    return
                }
                val total = snapshot.childrenCount
                lecturesCount.text = total.toString()
                Log.d(TAG, "📖 Live Lectures: $total")
            }

            override fun onCancelled(error: DatabaseError) {
                if (error.code != DatabaseError.PERMISSION_DENIED) {
                    Log.e(TAG, "❌ Quizzes listener cancelled: ${error.message}")
                }
            }
        })
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            performLogout()
        }
    }
}
