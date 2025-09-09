package com.example.thesis_app.ui.fragments.teacher

import android.content.Intent
import com.example.thesis_app.SignupActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.example.thesis_app.ClassDetailActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.thesis_app.models.ClassItem

class TeacherFragment : Fragment(R.layout.teachers) {
    private lateinit var recyclerView: RecyclerView
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

        // ✅ Use `view.findViewById`, not just `findViewById`
        recyclerView = view.findViewById(R.id.classRecyclerView)
        adapter = ClassAdapter(
            classList,
            { viewHolder -> itemTouchHelper.startDrag(viewHolder) }
        ) { classItem ->
            val intent = Intent(requireContext(), ClassDetailActivity::class.java).apply {
                putExtra("CLASS_NAME", classItem.className)
                putExtra("ROOM_NO", classItem.roomNo)
                putExtra("CLASS_CODE", classItem.classCode)
            }
            startActivity(intent)
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
        val classesRef = database.getReference("classes")

        classesRef.orderByChild("teacherId").equalTo(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    classList.clear()
                    for (classSnap in snapshot.children) {
                        val classItem = classSnap.getValue(ClassItem::class.java)
                        if (classItem != null) {
                            val itemWithCode = classItem.copy()
                            itemWithCode.classCode = classSnap.key ?: ""
                            classList.add(itemWithCode)
                        }
                    }
                    classList.sortBy { it.order }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun generateCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun generateUniqueCode(onCodeGenerated: (String) -> Unit) {
        val newCode = generateCode()
        val classesRef = database.getReference("classes")

        classesRef.child(newCode).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    generateUniqueCode(onCodeGenerated) // try again
                } else {
                    onCodeGenerated(newCode)
                }
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
        val classesRef = database.getReference("classes")
        classList.forEachIndexed { index, classItem ->
            if (classItem.classCode.isNotEmpty()) {
                classesRef.child(classItem.classCode).child("order").setValue(index)
            }
        }
    }


    private fun setupAddClassCard(rootView: View) {
        val addCard = rootView.findViewById<CardView>(R.id.addClassCard)
        addCard.setOnClickListener {
            val context = requireContext()

            val dialogLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 30, 40, 10)
            }

            val classInput = EditText(context).apply { hint = "Enter Class Name" }
            val roomInput = EditText(context).apply { hint = "Enter Room Number" }

            dialogLayout.addView(classInput)
            dialogLayout.addView(roomInput)

            AlertDialog.Builder(context)
                .setTitle("Add Class")
                .setView(dialogLayout)
                .setPositiveButton("Create") { dialog, _ ->
                    val className = classInput.text.toString().trim()
                    val roomNo = roomInput.text.toString().trim()

                    if (className.isEmpty()) {
                        classInput.error = "Required"
                        classInput.requestFocus()
                        return@setPositiveButton
                    }
                    if (roomNo.isEmpty()) {
                        roomInput.error = "Required"
                        roomInput.requestFocus()
                        return@setPositiveButton
                    }

                    // 🔹 Prevent duplicate room numbers
                    database.getReference("classes")
                        .orderByChild("roomNo").equalTo(roomNo)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    roomInput.error = "Room already exists"
                                    roomInput.requestFocus()
                                } else {
                                    generateUniqueCode { classCode ->
                                        val newClass = ClassItem(className, roomNo, classList.size)

                                        // Save globally
                                        val globalClassRef =
                                            database.getReference("classes").child(classCode)
                                        val classData = mapOf(
                                            "className" to className,
                                            "roomNo" to roomNo,
                                            "order" to classList.size,
                                            "teacherId" to auth.currentUser!!.uid
                                        )
                                        globalClassRef.setValue(classData)

                                        // Save pointer in teacher profile
                                        database.getReference("users")
                                            .child(auth.currentUser!!.uid)
                                            .child("classes")
                                            .child(classCode)
                                            .setValue(true)

                                        adapter.addItemAtTop(newClass.copy(classCode = classCode))
                                        recyclerView.scrollToPosition(0)
                                        dialog.dismiss()
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up pending updates
        pendingUpdateRunnable?.let { handler.removeCallbacks(it) }
    }
}
