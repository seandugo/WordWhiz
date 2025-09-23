package com.example.thesis_app.ui.fragments.teacher

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.ClassAdapter
import com.example.thesis_app.ClassDetailActivity
import com.example.thesis_app.R
import com.example.thesis_app.SignupActivity
import com.example.thesis_app.models.ClassItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TeacherFragment : Fragment(R.layout.teachers) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClassAdapter
    private val classList = mutableListOf<ClassItem>()
    private lateinit var itemTouchHelper: ItemTouchHelper
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private var isOrderChanged = false
    private var pendingUpdateRunnable: Runnable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.classRecyclerView)

        adapter = ClassAdapter(
            classList,
            onStartDrag = { vh -> itemTouchHelper.startDrag(vh) },
            onItemClick = { classItem ->
                val intent = Intent(requireContext(), ClassDetailActivity::class.java).apply {
                    putExtra("CLASS_NAME", classItem.className)
                    putExtra("ROOM_NO", classItem.roomNo)
                    putExtra("CLASS_CODE", classItem.classCode)
                }
                startActivity(intent)
            },
            onEditClick = { showEditDialog(it) },
            onDeleteClick = { showDeleteDialog(it) }
        )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.itemAnimator = DefaultItemAnimator()

        setupDragAndDrop()
        setupAddClassCard(view)
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
        val teacherId = auth.currentUser!!.uid
        val teacherClassesRef = database.getReference("users").child(teacherId).child("classes")

        teacherClassesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                classList.clear()
                for (classSnap in snapshot.children) {
                    val classItem = classSnap.getValue(ClassItem::class.java)
                    if (classItem != null) {
                        classList.add(classItem.copy(classCode = classSnap.key ?: ""))
                    }
                }
                classList.sortByDescending { it.order } // newest on top
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
                if (snapshot.exists()) generateUniqueCode(onCodeGenerated)
                else onCodeGenerated(newCode)
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
                classesRef.child(classItem.classCode).child("order")
                    .setValue(classList.size - 1 - index)
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

            // Class Name Input
            val classNameInput = EditText(context).apply {
                hint = "Class Name"
                inputType = InputType.TYPE_CLASS_TEXT
                filters = arrayOf(InputFilter.LengthFilter(20))
            }

            // Room Input (numeric only, max 12 digits)
            val roomInput = EditText(context).apply {
                hint = "Room No. (Optional)"
                inputType = InputType.TYPE_CLASS_NUMBER
                filters = arrayOf(InputFilter.LengthFilter(12))
            }

            dialogLayout.addView(TextView(context).apply { text = "Class Name:" })
            dialogLayout.addView(classNameInput)
            dialogLayout.addView(TextView(context).apply { text = "Room No. (Optional):" })
            dialogLayout.addView(roomInput)

            val dialog = AlertDialog.Builder(context)
                .setTitle("Add Class")
                .setView(dialogLayout)
                .setPositiveButton("Create", null)
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                var className = classNameInput.text.toString().trim().uppercase() // force UPPERCASE
                val roomNo = roomInput.text.toString().trim()

                // Validation
                if (className.isEmpty()) {
                    classNameInput.error = "Class Name is required"
                    classNameInput.requestFocus()
                    return@setOnClickListener
                }

                // Optional: enforce only letters/numbers/spaces
                if (!className.matches(Regex("^[A-Z0-9 ]+$"))) {
                    classNameInput.error = "Only letters, numbers, and spaces allowed"
                    classNameInput.requestFocus()
                    return@setOnClickListener
                }

                val teacherClassesRef = database.getReference("users")
                    .child(auth.currentUser!!.uid)
                    .child("classes")

                // Check for duplicates
                teacherClassesRef.orderByChild("className").equalTo(className)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                classNameInput.error = "This class already exists"
                                classNameInput.requestFocus()
                            } else {
                                generateUniqueCode { classCode ->
                                    val newOrder = (classList.maxOfOrNull { it.order } ?: -1) + 1
                                    val newClass = ClassItem(className, roomNo, newOrder, classCode)

                                    teacherClassesRef.child(classCode).setValue(newClass)
                                    database.getReference("classes").child(classCode)
                                        .setValue(
                                            mapOf(
                                                "className" to className,
                                                "roomNo" to roomNo,
                                                "order" to newOrder
                                            )
                                        )

                                    adapter.addItemAtTop(newClass)
                                    recyclerView.scrollToPosition(0)
                                    dialog.dismiss()
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        }
    }

    private fun showEditDialog(classItem: ClassItem) {
        val context = requireContext()
        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 10)
        }

        val classNameInput = EditText(context).apply {
            hint = "Class Name"
            setText(classItem.className)
            inputType = InputType.TYPE_CLASS_TEXT
            filters = arrayOf(InputFilter.LengthFilter(20))
            // Removed the TextWatcher that forced uppercase
        }

        val roomInput = EditText(context).apply {
            hint = "Room No."
            setText(classItem.roomNo)
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(12))
        }

        dialogLayout.addView(TextView(context).apply { text = "Class Name:" })
        dialogLayout.addView(classNameInput)
        dialogLayout.addView(TextView(context).apply { text = "Room No.:" })
        dialogLayout.addView(roomInput)

        AlertDialog.Builder(context)
            .setTitle("Edit Class")
            .setView(dialogLayout)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create().also { dialog ->
                dialog.show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    // Convert to uppercase only on save
                    val className = classNameInput.text.toString().trim().uppercase()
                    val roomNo = roomInput.text.toString().trim()

                    if (className.isEmpty()) {
                        classNameInput.error = "Class Name is required"
                        classNameInput.requestFocus()
                        return@setOnClickListener
                    }

                    if (!className.matches(Regex("^[A-Z0-9 ]+$"))) {
                        classNameInput.error = "Only letters, numbers, and spaces allowed"
                        classNameInput.requestFocus()
                        return@setOnClickListener
                    }

                    val newOrder = (classList.maxOfOrNull { it.order } ?: -1) + 1
                    classItem.className = className
                    classItem.roomNo = roomNo
                    classItem.order = newOrder

                    database.getReference("users")
                        .child(auth.currentUser!!.uid)
                        .child("classes")
                        .child(classItem.classCode)
                        .setValue(classItem)

                    database.getReference("classes")
                        .child(classItem.classCode)
                        .updateChildren(
                            mapOf(
                                "className" to className,
                                "roomNo" to roomNo,
                                "order" to newOrder
                            )
                        )

                    adapter.removeItem(classItem)
                    adapter.addItemAtTop(classItem)
                    recyclerView.scrollToPosition(0)

                    Toast.makeText(requireContext(), "Class updated successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
    }

    private fun showDeleteDialog(classItem: ClassItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Class")
            .setMessage("Are you sure you want to delete ${classItem.className}?")
            .setPositiveButton("Delete"){_,_ ->
                database.getReference("users").child(auth.currentUser!!.uid)
                    .child("classes").child(classItem.classCode).removeValue()
                database.getReference("classes").child(classItem.classCode).removeValue()
                adapter.removeItem(classItem)
                Toast.makeText(requireContext(), "Class deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}