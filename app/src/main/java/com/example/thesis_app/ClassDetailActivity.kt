package com.example.thesis_app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.Achievement
import com.example.thesis_app.models.StudentItem
import com.example.thesis_app.models.ClassRequest
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.database.*

class ClassDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var classNameText: TextView
    private lateinit var roomNumberText: TextView
    private lateinit var classCodeText: TextView
    private lateinit var seeAllStudentRequests: TextView
    private lateinit var adapter: StudentAdapter
    private val studentList = mutableListOf<StudentItem>()

    private var className: String = "Unknown Class"
    private var classCode: String = "Unknown Code"
    private var roomNumber: String = "N/A"

    private lateinit var database: DatabaseReference
    private lateinit var appBar: AppBarLayout
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_detail)

        database = FirebaseDatabase.getInstance().reference

        // Get class info
        className = intent.getStringExtra("CLASS_NAME") ?: "Unknown Class"
        classCode = intent.getStringExtra("CLASS_CODE") ?: "Unknown Code"
        roomNumber = intent.getStringExtra("ROOM_NO") ?: "N/A"

        // Views
        recyclerView = findViewById(R.id.studentRecyclerView)
        classNameText = findViewById(R.id.headerClassName)
        roomNumberText = findViewById(R.id.headerRoomNumber)
        classCodeText = findViewById(R.id.headerClassCode)
        seeAllStudentRequests = findViewById(R.id.seeAllStudentRequests)
        toolbar = findViewById(R.id.toolbar)
        appBar = findViewById(R.id.appbar)
        collapsingToolbar = findViewById(R.id.collapsingToolbar)

        // Header setup
        classNameText.text = className
        roomNumberText.text = "Room Number: $roomNumber"
        classCodeText.text = "Class Code: $classCode"

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        setupCollapsingToolbar()

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StudentAdapter(studentList) { student ->
            TeachersStudentProgressActivity.start(
                context = this,
                name = student.name ?: "Unknown",
                className = className,
                studentId = student.studentId ?: "N/A"
            )
        }
        recyclerView.adapter = adapter

        loadStudentsFromFirebase()
        countPendingRequests()

        findViewById<CardView>(R.id.addStudentCard).setOnClickListener { showAddStudentDialog() }

        // ✅ On click → open requests dialog
        seeAllStudentRequests.setOnClickListener {
            showPendingRequestsDialog()
        }
    }

    private fun setupCollapsingToolbar() {
        collapsingToolbar.isTitleEnabled = false
        var isTitleShown = false
        var scrollRange = -1

        appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (scrollRange == -1) scrollRange = appBarLayout.totalScrollRange
            if (scrollRange + verticalOffset == 0) {
                toolbar.title = classNameText.text
                toolbar.subtitle = roomNumberText.text
                classNameText.visibility = TextView.GONE
                roomNumberText.visibility = TextView.GONE
                classCodeText.visibility = TextView.GONE
                isTitleShown = true
                toolbar.navigationIcon?.setTint(getColor(android.R.color.black))
            } else if (isTitleShown) {
                toolbar.title = ""
                toolbar.subtitle = ""
                classNameText.visibility = TextView.VISIBLE
                roomNumberText.visibility = TextView.VISIBLE
                classCodeText.visibility = TextView.VISIBLE
                isTitleShown = false
                toolbar.navigationIcon?.setTint(getColor(android.R.color.white))
            }
        })
    }

    private fun loadStudentsFromFirebase() {
        if (classCode == "Unknown Code") return
        val studentsRef = database.child("classes").child(classCode).child("students")

        studentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                studentList.clear()
                for (studentSnap in snapshot.children) {
                    val student = studentSnap.getValue(StudentItem::class.java)
                    if (student != null) {
                        val studentWithId = student.copy()
                        studentWithId.studentId = studentSnap.key ?: ""
                        studentList.add(studentWithId)
                    }
                }
                studentList.sortBy { it.order }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /**
     * ✅ Count pending join requests for this class
     */
    private fun countPendingRequests() {
        val pendingRef = database.child("classes").child(classCode).child("pendingRequests")

        pendingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pendingCount = snapshot.children.count {
                    it.child("status").getValue(String::class.java) == "pending"
                }
                seeAllStudentRequests.text = "Requests: $pendingCount"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /**
     * ✅ Show a dialog with all pending requests (Approve/Reject)
     */
    private fun showPendingRequestsDialog() {
        val pendingRef = database.child("classes").child(classCode).child("pendingRequests")

        pendingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@ClassDetailActivity, "No pending requests.", Toast.LENGTH_SHORT).show()
                    return
                }

                val pendingList = mutableListOf<ClassRequest>()
                for (snap in snapshot.children) {
                    val request = snap.getValue(ClassRequest::class.java)
                    if (request != null && request.status == "pending") {
                        pendingList.add(request)
                    }
                }

                if (pendingList.isEmpty()) {
                    Toast.makeText(this@ClassDetailActivity, "No pending requests.", Toast.LENGTH_SHORT).show()
                    return
                }

                val dialogView = LayoutInflater.from(this@ClassDetailActivity)
                    .inflate(R.layout.dialog_class_requests, null)
                val recyclerView = dialogView.findViewById<RecyclerView>(R.id.requestsRecycler)
                recyclerView.layoutManager = LinearLayoutManager(this@ClassDetailActivity)

                // ✅ Create dialog first
                val dialog = AlertDialog.Builder(this@ClassDetailActivity)
                    .setTitle("Pending Join Requests")
                    .setView(dialogView)
                    .setNegativeButton("Close", null)
                    .create()

                val adapter = RequestAdapter(
                    pendingList,
                    onApprove = { request ->
                        approveStudentRequest(request.studentId, request.studentName)
                        dialog.dismiss() // ✅ Close after approval
                    },
                    onReject = { request ->
                        rejectStudentRequest(request.studentId)
                        dialog.dismiss() // ✅ Close after rejection (optional)
                    }
                )

                recyclerView.adapter = adapter
                dialog.show()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun approveStudentRequest(studentId: String, name: String) {
        val userRef = database.child("users").child(studentId)

        // Step 1️⃣ Get current class
        userRef.child("classes").get().addOnSuccessListener { snapshot ->
            var oldClassCode: String? = null

            for (child in snapshot.children) {
                val code = child.key
                if (code != null) {
                    oldClassCode = code
                    break
                }
            }

            val updates = hashMapOf<String, Any?>()

            // Step 2️⃣ Remove from old class if needed
            if (!oldClassCode.isNullOrEmpty() && oldClassCode != classCode) {
                updates["/classes/$oldClassCode/students/$studentId"] = null
                updates["/users/$studentId/classes/$oldClassCode"] = null
            }

            // Step 3️⃣ Remove student from pending requests
            updates["/classes/$classCode/pendingRequests/$studentId"] = null
            updates["/classChangeRequests/$studentId"] = null

            // Apply initial updates
            database.updateChildren(updates).addOnSuccessListener {
                // Step 4️⃣ Now safely add new class reference (in separate call)
                val addUpdates = hashMapOf<String, Any?>()

                // Add new class under user
                addUpdates["/users/$studentId/classes/$classCode"] = true

                // Add to new class student list
                val studentData = mapOf(
                    "name" to name,
                    "studentId" to studentId,
                    "order" to studentList.size
                )
                addUpdates["/classes/$classCode/students/$studentId"] = studentData

                database.updateChildren(addUpdates).addOnSuccessListener {
                    Toast.makeText(this, "$name has been added to $className.", Toast.LENGTH_SHORT).show()
                    countPendingRequests()
                    loadStudentsFromFirebase()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to add to new class: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to fetch student data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ✅ Reject student request
     */
    private fun rejectStudentRequest(studentId: String) {
        val updates = hashMapOf<String, Any?>(
            "/classes/$classCode/pendingRequests/$studentId" to null,
            "/classChangeRequests/$studentId" to null
        )

        database.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Request rejected.", Toast.LENGTH_SHORT).show()
            countPendingRequests()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to reject: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Existing Add Student dialog (unchanged)
    private fun showAddStudentDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_student, null)
        val idInput = dialogView.findViewById<EditText>(R.id.editStudentId)

        idInput.setText("S-")
        idInput.setSelection(idInput.text.length)

        idInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.toString().startsWith("S-")) {
                    idInput.setText("S-")
                    idInput.setSelection(idInput.text.length)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Student")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val fullInput = idInput.text.toString().trim()
            val numericPart = fullInput.removePrefix("S-")

            if (numericPart.isEmpty()) {
                Toast.makeText(this, "Please enter a student ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!numericPart.matches(Regex("^\\d+$"))) {
                Toast.makeText(this, "Student ID must be numeric", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val studentId = "S$numericPart"
            val studentRef = database.child("users").child(studentId)

            // Check if the student exists
            studentRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(this@ClassDetailActivity, "No student found with that ID", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val role = snapshot.child("role").getValue(String::class.java)
                    if (role != "student") {
                        Toast.makeText(this@ClassDetailActivity, "This user is not a student", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val name = snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val email = snapshot.child("email").getValue(String::class.java)
                    val achievementsList = snapshot.child("achievements").children.mapNotNull {
                        it.getValue(Achievement::class.java)
                    }

                    // 🔍 Check if student already has a class
                    val existingClassSnap = snapshot.child("classes")
                    if (existingClassSnap.exists() && existingClassSnap.childrenCount > 0) {
                        val oldClassCode = existingClassSnap.children.first().key ?: ""

                        // ⚠️ Show confirmation dialog
                        AlertDialog.Builder(this@ClassDetailActivity)
                            .setTitle("Student Already Has a Class")
                            .setMessage("This student is currently in class $oldClassCode. Do you want to move them to $classCode?")
                            .setPositiveButton("Yes") { _, _ ->
                                moveStudentToNewClass(studentId, name, email, achievementsList, oldClassCode)
                                dialog.dismiss()
                            }
                            .setNegativeButton("No", null)
                            .show()
                    } else {
                        // Directly add since no existing class
                        addStudentToClass(studentId, name, email, achievementsList)
                        dialog.dismiss()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ClassDetailActivity, "Database error", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    /**
     * ✅ Move existing student from one class to another
     */
    private fun moveStudentToNewClass(
        studentId: String,
        name: String?,
        email: String?,
        achievementsList: List<Achievement>,
        oldClassCode: String
    ) {
        val updates = hashMapOf<String, Any?>()

        // 1️⃣ Remove from old class
        updates["/classes/$oldClassCode/students/$studentId"] = null
        updates["/users/$studentId/classes/$oldClassCode"] = null

        // 2️⃣ Add new class reference under user
        updates["/users/$studentId/classes/$classCode"] = true

        // 3️⃣ Add new student data under class
        val newStudent = StudentItem(
            name = name,
            email = email,
            order = studentList.size,
            studentId = studentId,
            achievements = achievementsList
        )
        updates["/classes/$classCode/students/$studentId"] = newStudent

        database.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Moved $name from $oldClassCode to $classCode", Toast.LENGTH_SHORT).show()
            loadStudentsFromFirebase()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ✅ Add new student directly (no old class)
     */
    private fun addStudentToClass(
        studentId: String,
        name: String?,
        email: String?,
        achievementsList: List<Achievement>
    ) {
        val newStudent = StudentItem(
            name = name,
            email = email,
            order = studentList.size,
            studentId = studentId,
            achievements = achievementsList
        )

        val updates = hashMapOf<String, Any?>(
            "/classes/$classCode/students/$studentId" to newStudent,
            "/users/$studentId/classes/$classCode" to true
        )

        database.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Added $name to $classCode", Toast.LENGTH_SHORT).show()
            loadStudentsFromFirebase()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
