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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseReference

class LoginActivity : ComponentActivity() {

    private lateinit var login: Button
    private lateinit var signup: TextView
    private lateinit var teacherEmail: TextInputEditText
    private lateinit var teacherPassword: TextInputEditText
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var snackbar: Snackbar? = null
    private var hasAutoLoggedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        // Check for fresh install or cleared data
        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("isFirstRun", true)
        val savedEmail = prefs.getString("email", null)
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (isFirstRun || (currentUser != null && savedEmail == null)) {
            FirebaseAuth.getInstance().signOut()
            prefs.edit().putBoolean("isFirstRun", false).apply()
        }

        login = findViewById(R.id.LoginButton)
        signup = findViewById(R.id.textView9)
        teacherEmail = findViewById(R.id.editEmail)
        teacherPassword = findViewById(R.id.Password)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        checkInternetAndSetUI()

        swipeRefresh.setOnRefreshListener {
            checkInternetAndSetUI(autoLogin = true)
            swipeRefresh.isRefreshing = false
        }

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmation()
                }
            })

        login.setOnClickListener { handleLogin() }
        signup.setOnClickListener {
            if (!isInternetAvailable()) {
                Snackbar.make(findViewById(android.R.id.content), "No internet connection. Please try again later.", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            handleSignup()
        }
    }

    override fun onStart() {
        super.onStart()

        if (hasAutoLoggedIn) return
        hasAutoLoggedIn = true

        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("isFirstRun", true)

        if (isFirstRun || !isInternetAvailable()) {
            Snackbar.make(findViewById(android.R.id.content), "No internet connection or first run. Please log in.", Snackbar.LENGTH_LONG).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val email = currentUser.email ?: return
        val savedRole = prefs.getString("role", null)
        val savedStudentId = prefs.getString("studentId", null)

        if (savedRole != null) {
            if (savedRole == "student" && savedStudentId != null) {
                checkInternetAndSetUI(autoLogin = true)
                val userRef = FirebaseDatabase.getInstance().getReference("users").child(savedStudentId)
                userRef.get().addOnSuccessListener { snapshot ->
                    val isNewAccount = snapshot.child("newAccount").getValue(Boolean::class.java) ?: true
                    if (isNewAccount) userRef.child("newAccount").setValue(true)

                    val hasProgress = snapshot.hasChild("progress")
                    if (isNewAccount || !hasProgress) initializeStudentProgress(savedStudentId, userRef)

                    // Always update class info for returning students
                    val classCode = snapshot.child("classes").children.firstOrNull()?.key
                    if (classCode != null) {
                        FirebaseDatabase.getInstance().getReference("classes/$classCode/className")
                            .get()
                            .addOnSuccessListener { classSnap ->
                                val className = classSnap.getValue(String::class.java) ?: "No Class"
                                val studentName = snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                                savePrefs(savedRole, snapshot.child("email").getValue(String::class.java) ?: email, savedStudentId, studentName, className)
                                goToLoading(savedRole, savedStudentId)
                            }
                            .addOnFailureListener {
                                goToLoading(savedRole, savedStudentId)
                            }
                    } else {
                        goToLoading(savedRole, savedStudentId)
                    }
                }.addOnFailureListener {
                    Log.e("Firebase", "❌ Failed to verify progress: ${it.message}")
                    goToLoading(savedRole, savedStudentId)
                }
            } else {
                checkInternetAndSetUI(autoLogin = false)
                goToLoading(savedRole, savedStudentId)
            }
        } else {
            fetchUserData(email)
        }
    }

    private fun checkInternetAndSetUI(autoLogin: Boolean = false) {
        swipeRefresh.isRefreshing = true

        if (!isInternetAvailable()) {
            login.isEnabled = false
            signup.isEnabled = false
            swipeRefresh.isRefreshing = false

            snackbar = Snackbar.make(
                findViewById(android.R.id.content),
                "No internet connection",
                Snackbar.LENGTH_INDEFINITE
            ).setAction("Retry") {
                checkInternetAndSetUI(autoLogin)
            }
            snackbar?.show()
        } else {
            login.isEnabled = true
            signup.isEnabled = true
            snackbar?.dismiss()
            swipeRefresh.isRefreshing = false

            if (autoLogin) {
                attemptAutoLogin()
            }
        }
    }

    private fun attemptAutoLogin() {
        if (hasAutoLoggedIn) return

        disableButtons()

        val prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        val email = prefs.getString("email", null) ?: return
        val studentId = prefs.getString("studentId", null) ?: return
        val role = prefs.getString("role", null) ?: return

        FirebaseAuth.getInstance().currentUser?.let { user ->
            goToLoading(role, studentId)
        } ?: run {
            enableButtons()
        }
    }

    private fun handleLogin() {
        // 1️⃣ Check if user is already logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            Toast.makeText(this, "You are already logged in", Toast.LENGTH_SHORT).show()
            goToLoading(
                getSharedPreferences("USER_PREFS", MODE_PRIVATE).getString("role", null),
                getSharedPreferences("USER_PREFS", MODE_PRIVATE).getString("studentId", null)
            )
            return
        }

        // 2️⃣ Check internet connectivity
        if (!isInternetAvailable()) {
            Toast.makeText(this, "No internet connection. Please try again later.", Toast.LENGTH_SHORT).show()
            return
        }

        val email = teacherEmail.text.toString().trim()
        val password = teacherPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter teacher email and password", Toast.LENGTH_SHORT).show()
            return
        }

        disableButtons()
        Snackbar.make(findViewById(android.R.id.content), "Logging in…", Snackbar.LENGTH_SHORT).show()

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                Handler(Looper.getMainLooper()).postDelayed({ enableButtons() }, 1000)
                if (task.isSuccessful) {
                    teacherEmail.text?.clear()
                    teacherPassword.text?.clear()
                    fetchUserData(email)
                } else {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleSignup() {
        if (!login.isEnabled) return
        val intent = Intent(this, LoadingActivity::class.java)
        intent.putExtra("mode", "signup")
        startActivity(intent)
    }

    private fun fetchUserData(email: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
        dbRef.orderByChild("email").equalTo(email).get()
            .addOnSuccessListener { snapshot ->
                enableButtons()
                if (!snapshot.exists()) {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val child = snapshot.children.first()
                val role = child.child("role").getValue(String::class.java)
                val emailDb = child.child("email").getValue(String::class.java)
                val studentId = child.child("studentID").getValue(String::class.java)
                val studentName = child.child("name").getValue(String::class.java) ?: "Unknown"

                if (role == "student" && studentId != null) {
                    val userRef = dbRef.child(studentId)

                    val isNewAccount = child.child("newAccount").getValue(Boolean::class.java) ?: true
                    if (isNewAccount) {
                        // ✅ Mark new account and initialize full progress
                        userRef.child("newAccount").setValue(true)
                        initializeStudentProgress(studentId, userRef)
                    }

                    val classCode = child.child("classes").children.firstOrNull()?.key
                    if (classCode != null) {
                        FirebaseDatabase.getInstance().getReference("classes/$classCode/className")
                            .get()
                            .addOnSuccessListener { classSnap ->
                                val className = classSnap.getValue(String::class.java) ?: "No Class"
                                savePrefs(role, emailDb ?: email, studentId, studentName, className)
                                goToLoading(role, studentId)
                            }
                            .addOnFailureListener {
                                savePrefs(role, emailDb ?: email, studentId, studentName, "No Class")
                                goToLoading(role, studentId)
                            }
                    } else {
                        savePrefs(role, emailDb ?: email, studentId, studentName, "No Class")
                        goToLoading(role, studentId)
                    }
                } else {
                    savePrefs(role, emailDb ?: email, null, null, null)
                    goToLoading(role, null)
                }
            }
            .addOnFailureListener {
                enableButtons()
                Log.e("FirebaseError", "Failed to fetch user data: ${it.message}")
            }
    }

    private fun savePrefs(
        role: String?,
        email: String,
        studentId: String?,
        studentName: String?,
        studentClass: String?
    ) {
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

    private var isNavigatingToLoading = false

    private fun goToLoading(role: String?, studentId: String?) {
        if (isNavigatingToLoading) return
        isNavigatingToLoading = true
        val intent = Intent(this, LoadingActivity::class.java)
        intent.putExtra("mode", "login")
        intent.putExtra("role", role)
        if (role == "student" && studentId != null) intent.putExtra("studentId", studentId)
        startActivity(intent)
        finish()
    }

    private fun initializeStudentProgress(studentId: String, userRef: DatabaseReference) {
        val quizzesRef = FirebaseDatabase.getInstance().getReference("quizzes")
        val studentProgressRef = userRef.child("progress")

        // ✅ Fetch classCode asynchronously
        userRef.child("classes").get().addOnSuccessListener { classSnap ->
            val classCode = classSnap.children.firstOrNull()?.key

            // Fetch quizzes
            quizzesRef.get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) return@addOnSuccessListener

                val progressData = mutableMapOf<String, Any>()

                for (quizSnap in snapshot.children) {
                    val quizCode = quizSnap.key ?: continue
                    val partsData = mutableMapOf<String, Any>()
                    for (partSnap in quizSnap.children) {
                        val partKey = partSnap.key ?: continue
                        if (partKey.startsWith("part") || partKey == "post-test") {
                            partsData[partKey] = mapOf(
                                "answeredCount" to 0,
                                "isCompleted" to false
                            )
                        }
                    }
                    if (partsData.isNotEmpty()) {
                        partsData["isCompleted"] = false
                        progressData[quizCode] = partsData
                    }
                }

                // Save user progress
                studentProgressRef.setValue(progressData)
                    .addOnSuccessListener {
                        Log.d("Firebase", "✅ Progress initialized for $studentId (user)")

                        // Save user progress
                        studentProgressRef.setValue(progressData)
                            .addOnSuccessListener {
                                Log.d("Firebase", "✅ Progress initialized for $studentId (user)")

                                // Mark newAccount false
                                userRef.child("newAccount").setValue(false)

                                updateProgressWithOrder(studentId, userRef)
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firebase", "❌ Failed to initialize user progress: ${e.message}")
                            }

                        // Mark newAccount false
                        userRef.child("newAccount").setValue(false)

                        updateProgressWithOrder(studentId, userRef)
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firebase", "❌ Failed to initialize user progress: ${e.message}")
                    }
            }.addOnFailureListener { e ->
                Log.e("Firebase", "❌ Failed to fetch quizzes: ${e.message}")
            }
        }.addOnFailureListener { e ->
            Log.e("Firebase", "❌ Failed to fetch classCode: ${e.message}")
        }
    }

    private fun updateProgressWithOrder(studentId: String, userRef: DatabaseReference) {
        val progressRef = userRef.child("progress")
        val quizzesRef = FirebaseDatabase.getInstance().getReference("quizzes")

        progressRef.get().addOnSuccessListener { progressSnap ->
            if (!progressSnap.exists()) return@addOnSuccessListener
            for (quizSnap in progressSnap.children) {
                val quizCode = quizSnap.key ?: continue
                quizzesRef.child(quizCode).child("order").get().addOnSuccessListener { orderSnap ->
                    val order = orderSnap.getValue(Int::class.java) ?: 0
                    progressRef.child(quizCode).child("order").setValue(order)
                        .addOnSuccessListener {
                            Log.d("Firebase", "✅ Order $order added to quiz $quizCode for $studentId")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firebase", "❌ Failed to add order for $quizCode: ${e.message}")
                        }
                }
            }
        }.addOnFailureListener { e ->
            Log.e("Firebase", "❌ Failed to read progress for $studentId: ${e.message}")
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
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