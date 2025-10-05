package com.example.thesis_app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseReference

class LoginActivity : ComponentActivity() {

    private lateinit var login: Button
    private lateinit var signup: TextView
    private lateinit var teacherEmail: TextInputEditText
    private lateinit var teacherPassword: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        login = findViewById(R.id.LoginButton)
        signup = findViewById(R.id.textView9)
        teacherEmail = findViewById(R.id.editEmail)
        teacherPassword = findViewById(R.id.Password)

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmation()
                }
            })

        login.setOnClickListener {
            disableButtons()
            Snackbar.make(findViewById(android.R.id.content), "Logging in…", Snackbar.LENGTH_SHORT).show()

            val email = teacherEmail.text.toString().trim()
            val password = teacherPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter teacher email and password", Toast.LENGTH_SHORT).show()
                enableButtons()
                return@setOnClickListener
            }

            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    Handler(Looper.getMainLooper()).postDelayed({
                        enableButtons()
                    }, 1000)

                    if (task.isSuccessful) {
                        fetchUserData(email)
                        teacherEmail.text?.clear()
                        teacherPassword.text?.clear()
                    } else {
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        signup.setOnClickListener {
            if (!login.isEnabled) return@setOnClickListener
            val intent = Intent(this, LoadingActivity::class.java)
            intent.putExtra("mode", "signup")
            startActivity(intent)
        }
    }

    private fun fetchUserData(email: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
        dbRef.orderByChild("email").equalTo(email).get()
            .addOnSuccessListener { snapshot ->
                enableButtons()
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val role = child.child("role").getValue(String::class.java)
                        val emailDb = child.child("email").getValue(String::class.java)
                        val studentId = child.child("studentID").getValue(String::class.java)
                        val studentName = child.child("name").getValue(String::class.java) ?: "Unknown"

                        // ✅ Add newAccount = true if it doesn't exist
                        val isNewAccount = if (!child.hasChild("newAccount")) {
                            child.ref.child("newAccount").setValue(true)
                            true
                        } else {
                            child.child("newAccount").getValue(Boolean::class.java) ?: false
                        }

                        // ✅ Initialize progress if newAccount is true and role is student
                        if (role == "student" && studentId != null && isNewAccount) {
                            initializeStudentProgress(studentId, child.ref)
                        }

                        if (role == "student" && studentId != null) {
                            // ✅ Get first classCode under users -> studentId -> classes
                            val classCode = child.child("classes").children.firstOrNull()?.key

                            if (classCode != null) {
                                // ✅ Fetch className from classes -> classCode -> className
                                FirebaseDatabase.getInstance().getReference("classes")
                                    .child(classCode)
                                    .child("className")
                                    .get()
                                    .addOnSuccessListener { classSnap ->
                                        val className = classSnap.getValue(String::class.java) ?: "No Class"

                                        savePrefs(role, emailDb ?: email, studentId, studentName, className)

                                        goToLoading(role, studentId)
                                    }
                            } else {
                                // fallback if no class found
                                savePrefs(role, emailDb ?: email, studentId, studentName, "No Class")
                                goToLoading(role, studentId)
                            }
                        } else {
                            // teacher or other roles
                            savePrefs(role, emailDb ?: email, null, null, null)
                            goToLoading(role, null)
                        }
                        break
                    }
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                    enableButtons()
                }
            }
            .addOnFailureListener {
                Log.e("FirebaseError", "Failed to fetch user data: ${it.message}")
                enableButtons()
            }
    }

    private fun savePrefs(role: String?, email: String, studentId: String?, studentName: String?, studentClass: String?) {
        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE).edit()
        prefs.putString("role", role)
        prefs.putString("email", email)
        if (role == "student") {
            prefs.putString("studentId", studentId)
            prefs.putString("studentName", studentName)
            prefs.putString("studentClass", studentClass)
        }
        prefs.apply()
    }

    private fun goToLoading(role: String?, studentId: String?) {
        val intent = Intent(this, LoadingActivity::class.java)
        intent.putExtra("mode", "login")
        intent.putExtra("role", role)
        if (role == "student" && studentId != null) {
            intent.putExtra("studentId", studentId)
        }
        startActivity(intent)
        finish()
    }

    private fun initializeStudentProgress(studentId: String, userRef: DatabaseReference) {
        val quizzesRef = FirebaseDatabase.getInstance().reference.child("quizzes")
        val studentProgressRef = userRef.child("progress")

        quizzesRef.get().addOnSuccessListener { snapshot ->
            val progressData = mutableMapOf<String, Any>()

            for (quizSnap in snapshot.children) {
                val quizId = quizSnap.key ?: continue
                val partsData = mutableMapOf<String, Any>()

                // Only include nodes that are actual parts (skip title/subtitle)
                quizSnap.children.forEach { partSnap ->
                    val partId = partSnap.key ?: return@forEach
                    if (!partId.startsWith("part")) return@forEach  // skip non-part nodes

                    partsData[partId] = mapOf(
                        "answeredCount" to 0,
                        "isCompleted" to false
                    )
                }

                // Add per-quiz isCompleted node
                if (partsData.isNotEmpty()) {
                    partsData["isCompleted"] = false
                    progressData[quizId] = partsData
                }
            }

            studentProgressRef.setValue(progressData)
                .addOnSuccessListener {
                    println("Student progress initialized successfully for $studentId")
                    userRef.child("newAccount").setValue(false)
                }
                .addOnFailureListener { e -> e.printStackTrace() }
        }.addOnFailureListener { e -> e.printStackTrace() }
    }

    override fun onStart() {
        super.onStart()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val email = currentUser.email
            if (email != null) {
                val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                val savedRole = prefs.getString("role", null)
                val savedStudentId = prefs.getString("studentId", null)

                if (savedRole != null) {
                    // Fast auto-login with cached prefs
                    if (savedRole == "student" && savedStudentId != null) {
                        checkAndInitializeProgress(savedStudentId)
                    }

                    goToLoading(savedRole, savedStudentId)
                } else {
                    // Fetch fresh from DB
                    val dbRef = FirebaseDatabase.getInstance().getReference("users")
                    dbRef.orderByChild("email").equalTo(email).get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                for (child in snapshot.children) {
                                    val role = child.child("role").getValue(String::class.java)
                                    val studentId = child.child("studentID").getValue(String::class.java)
                                    val studentName = child.child("name").getValue(String::class.java) ?: "Unknown"

                                    if (!role.isNullOrEmpty()) {
                                        if (role == "student" && studentId != null) {
                                            val classCode = child.child("classes").children.firstOrNull()?.key
                                            if (classCode != null) {
                                                FirebaseDatabase.getInstance().getReference("classes")
                                                    .child(classCode)
                                                    .child("className")
                                                    .get()
                                                    .addOnSuccessListener { classSnap ->
                                                        val className = classSnap.getValue(String::class.java) ?: "No Class"

                                                        savePrefs(role, email, studentId, studentName, className)

                                                        // ✅ Check and initialize progress if newAccount = true
                                                        val isNewAccount = child.child("newAccount").getValue(Boolean::class.java) ?: false
                                                        if (isNewAccount) {
                                                            initializeStudentProgress(studentId, child.ref)
                                                        }

                                                        goToLoading(role, studentId)
                                                    }
                                            } else {
                                                savePrefs(role, email, studentId, studentName, "No Class")
                                                goToLoading(role, studentId)
                                            }
                                        } else {
                                            savePrefs(role, email, null, null, null)
                                            goToLoading(role, null)
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                }
            }
        }
    }

    // Helper function for cached prefs fast-login
    private fun checkAndInitializeProgress(studentId: String) {
        val userRef = FirebaseDatabase.getInstance().reference.child("users").child(studentId)
        userRef.get().addOnSuccessListener { snapshot ->
            val isNewAccount = snapshot.child("newAccount").getValue(Boolean::class.java) ?: false
            if (isNewAccount) {
                initializeStudentProgress(studentId, userRef)
            }
        }.addOnFailureListener { it.printStackTrace() }
    }

    private fun disableButtons() {
        login.isEnabled = false
        signup.isEnabled = false
    }

    private fun enableButtons() {
        login.isEnabled = true
        signup.isEnabled = true
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
