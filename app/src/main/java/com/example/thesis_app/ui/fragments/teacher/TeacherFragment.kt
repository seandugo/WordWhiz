package com.example.thesis_app.ui.fragments.teacher

import com.example.thesis_app.SignupActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.thesis_app.R
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.ClassAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.thesis_app.models.ClassItem

class TeacherFragment : Fragment(R.layout.teachers) {
    private lateinit var teacherButton: CardView
    private lateinit var studentButton: CardView
    private lateinit var recyclerView: RecyclerView
    private lateinit var teacherRef: DatabaseReference
    private lateinit var adapter: ClassAdapter
    private val classList = mutableListOf<ClassItem>()
    private lateinit var itemTouchHelper: ItemTouchHelper
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private var isOrderChanged = false
    private var pendingUpdateRunnable: Runnable? = null // ✅ You forgot to declare this

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        teacherButton = view.findViewById(R.id.teacher)
        studentButton = view.findViewById(R.id.student)

        // ✅ Use `view.findViewById`, not just `findViewById`
        recyclerView = view.findViewById(R.id.classRecyclerView)
        adapter = ClassAdapter(classList) { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext()) // ✅ pass context
        recyclerView.itemAnimator = DefaultItemAnimator()

        setupDragAndDrop()
        setupAddClassCard(view) // ✅ pass the fragment's root view
        setupTeacherRef()

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    (activity as? SignupActivity)?.showExitConfirmation()
                }
            })
    }

    private fun setupTeacherRef() {
        val uid = auth.currentUser!!.uid
        teacherRef = database.getReference("users").child(uid).child("classes")
        loadClassesFromFirebase()
    }

    fun generateCode(length: Int = 6): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
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

            override fun onCancelled(error: DatabaseError) {}
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
        pendingUpdateRunnable?.let { handler.removeCallbacks(it) }

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
                        if (item?.className == classItem.className && item.roomNo == classItem.roomNo) {
                            teacherRef.child(classSnap.key!!).child("order").setValue(index)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    // ✅ Pass root view to access findViewById
    private fun setupAddClassCard(rootView: View) {
        val addCard = rootView.findViewById<CardView>(R.id.addClassCard)
        addCard.setOnClickListener {
            val context = requireContext()

            val dialogLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 30, 40, 10)
            }

            val classInput = EditText(context).apply { hint = "Enter Class Name" }
            val roomInput = EditText(context).apply {
                hint = "Room number (e.g., 101)"
                inputType = InputType.TYPE_CLASS_NUMBER
                filters = arrayOf(InputFilter.LengthFilter(5))
            }

            dialogLayout.addView(classInput)
            dialogLayout.addView(roomInput)

            val dialog = AlertDialog.Builder(context)
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

    override fun onDestroy() {
        super.onDestroy()
        // Clean up pending updates
        pendingUpdateRunnable?.let { handler.removeCallbacks(it) }
    }
}
