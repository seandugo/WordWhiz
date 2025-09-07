package com.example.thesis_app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.widget.Toolbar
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.activity.OnBackPressedCallback
import android.view.MenuItem
import androidx.core.view.GravityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import java.util.Collections

data class ClassItem(
    val className: String = "",
    val roomNo: String = "",
    var order: Int = 0
)

fun generateCode(length: Int = 6): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..length)
        .map { chars.random() }
        .joinToString("")
}

class TeacherActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var teacherRef: DatabaseReference
    private lateinit var adapter: ClassAdapter
    private val classList = mutableListOf<ClassItem>()
    private lateinit var itemTouchHelper: ItemTouchHelper
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private var pendingUpdateRunnable: Runnable? = null
    private var isOrderChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.teacher)

        // Set system bar colors (optional)
        window.navigationBarColor = getColor(R.color.my_nav_color)
        window.statusBarColor = getColor(R.color.my_nav_color)

        recyclerView = findViewById(R.id.classRecyclerView)
        adapter = ClassAdapter(classList) { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.itemAnimator = DefaultItemAnimator() // Enable smooth animations

        setupDragAndDrop()
        setupAddClassCard()
        setupTeacherRef()

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        toggle.drawerArrowDrawable.color = getColor(android.R.color.black)
        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        showExitConfirmation()
                    }
                }
            })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // Handle Home
            }
            R.id.nav_settings -> {
                // Handle Settings
            }
            R.id.nav_logout -> {
                showExitConfirmation()
            }
        }
        return true
    }

    private fun setupTeacherRef() {
        val uid = auth.currentUser!!.uid
        teacherRef = database.getReference("users").child(uid).child("classes")
        loadClassesFromFirebase()
    }

    private fun generateUniqueCode(onCodeGenerated: (String) -> Unit) {
        val newCode = generateCode()
        teacherRef.child(newCode).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    generateUniqueCode(onCodeGenerated)
                } else {
                    onCodeGenerated(newCode)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadClassesFromFirebase() {
        teacherRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                classList.clear()
                for (classSnap in snapshot.children) {
                    val classItem = classSnap.getValue(ClassItem::class.java)
                    if (classItem != null) {
                        classList.add(classItem)
                    }
                }
                classList.sortBy { it.order }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun setupDragAndDrop() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                adapter.swapItems(fromPos, toPos)
                isOrderChanged = true
                scheduleFirebaseUpdate()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled(): Boolean = false
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun scheduleFirebaseUpdate() {
        // Cancel any pending updates
        pendingUpdateRunnable?.let { handler.removeCallbacks(it) }

        // Schedule new update with 500ms debounce
        pendingUpdateRunnable = Runnable {
            if (isOrderChanged) {
                updateOrderInFirebase()
                isOrderChanged = false
            }
        }
        handler.postDelayed(pendingUpdateRunnable!!, 500)
    }

    private fun updateOrderInFirebase() {
        classList.forEachIndexed { index, classItem ->
            teacherRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (classSnap in snapshot.children) {
                        val item = classSnap.getValue(ClassItem::class.java)
                        if (item?.className == classItem.className && item?.roomNo == classItem.roomNo) {
                            teacherRef.child(classSnap.key!!).child("order").setValue(index)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error (e.g., show a Toast to the user)
                }
            })
        }
    }

    private fun setupAddClassCard() {
        val addCard = findViewById<CardView>(R.id.addClassCard)
        addCard.setOnClickListener {
            val dialogLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 30, 40, 10)
            }

            val classInput = EditText(this).apply { hint = "Enter Class Name" }
            val roomInput = EditText(this).apply {
                hint = "Room number (e.g., 101)"
                inputType = InputType.TYPE_CLASS_NUMBER
                filters = arrayOf(InputFilter.LengthFilter(5))
            }

            dialogLayout.addView(classInput)
            dialogLayout.addView(roomInput)

            val dialog = AlertDialog.Builder(this)
                .setTitle("Add New Class")
                .setView(dialogLayout)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create()

            dialog.setOnShowListener {
                val addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                addButton.setOnClickListener {
                    val className = classInput.text.toString().ifEmpty { "New Class" }
                    val roomText = roomInput.text.toString().trim()

                    if (roomText.isEmpty() || !roomText.matches(Regex("\\d+"))) {
                        roomInput.error = "Enter numbers only"
                        roomInput.requestFocus()
                        return@setOnClickListener
                    }

                    val roomNo = "Rm $roomText"

                    teacherRef.orderByChild("roomNo").equalTo(roomNo)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    roomInput.error = "Room already exists"
                                    roomInput.requestFocus()
                                } else {
                                    generateUniqueCode { classCode ->
                                        val newClass = ClassItem(className, roomNo, classList.size)
                                        teacherRef.child(classCode).setValue(newClass)
                                        dialog.dismiss()
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }

            dialog.show()
        }
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

    override fun onDestroy() {
        super.onDestroy()
        // Clean up pending updates
        pendingUpdateRunnable?.let { handler.removeCallbacks(it) }
    }
}