package com.example.thesis_app.ui.fragments.teacher

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.ClassStats
import com.example.thesis_app.ClassStatsAdapter
import com.example.thesis_app.R
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class TeacherFragment : Fragment(R.layout.teacher_overview) {

    private lateinit var recycler: RecyclerView
    private lateinit var database: DatabaseReference
    private lateinit var nameTextExpanded: TextView
    private lateinit var subtitleQuiz: TextView
    private lateinit var emptyImage: ImageView
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val classStatsList = mutableListOf<ClassStats>()
    private lateinit var adapter: ClassStatsAdapter
    private val listeners = mutableMapOf<String, ValueEventListener>() // ✅ map by classCode

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val collapsingToolbar = view.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        nameTextExpanded = view.findViewById(R.id.nameTextExpanded)
        subtitleQuiz = view.findViewById(R.id.subtitleQuiz)
        recycler = view.findViewById(R.id.resultRecycler)
        emptyImage = view.findViewById(R.id.emptyImage)

        setupAppBarToolbar(collapsingToolbar, toolbar)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = ClassStatsAdapter(classStatsList)
        recycler.adapter = adapter

        loadClassStatisticsRealtime()
    }

    private fun setupAppBarToolbar(
        collapsing: CollapsingToolbarLayout,
        toolbar: MaterialToolbar
    ) {
        collapsing.isTitleEnabled = true
        collapsing.title = ""
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    // ✅ Real-time listener for teacher’s classes
    private fun loadClassStatisticsRealtime() {
        val teacherUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val teacherClassesRef = FirebaseDatabase.getInstance().getReference("users/$teacherUid/classes")

        teacherClassesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(classesSnapshot: DataSnapshot) {
                if (!classesSnapshot.exists()) {
                    recycler.visibility = View.GONE
                    emptyImage.visibility = View.VISIBLE
                    return
                }

                recycler.visibility = View.VISIBLE
                emptyImage.visibility = View.GONE

                // clear old listeners
                clearAllListeners()

                classStatsList.clear()

                for (classSnap in classesSnapshot.children) {
                    val classCode = classSnap.key ?: continue
                    val classRef = FirebaseDatabase.getInstance().getReference("classes/$classCode")

                    classRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(classDataSnap: DataSnapshot) {
                            val className = classDataSnap.child("className").getValue(String::class.java) ?: "Unnamed Class"
                            val studentIds = classDataSnap.child("students").children.mapNotNull { it.key }

                            // Add placeholder item
                            val classStats = ClassStats(className, 0, 0, 0, 0)
                            classStatsList.add(classStats)
                            adapter.notifyItemInserted(classStatsList.size - 1)

                            // Start live updates for this class
                            listenToClassRealtime(classCode, studentIds, classStats)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("TeacherFragment", "Error fetching class: ${error.message}")
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TeacherFragment", "Classes listener cancelled: ${error.message}")
            }
        })
    }

    // ✅ One listener per class that updates in real-time
    private fun listenToClassRealtime(
        classCode: String,
        studentIds: List<String>,
        classStats: ClassStats
    ) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        val currentDate = Calendar.getInstance().time

        val listener = usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var activeCount = 0
                var inLectureCount = 0
                var inactiveCount = 0

                for (studentId in studentIds) {
                    val studentSnap = snapshot.child(studentId)
                    val status = studentSnap.child("presence/status").getValue(String::class.java)

                    when (status) {
                        "online" -> activeCount++
                        "in_lecture" -> inLectureCount++
                        else -> {
                            val lastActiveDateStr = studentSnap.child("activityStreak/lastActiveDate").getValue(String::class.java)
                            if (lastActiveDateStr != null) {
                                try {
                                    val lastActiveDate = dateFormat.parse(lastActiveDateStr)
                                    val diffDays = (currentDate.time - (lastActiveDate?.time ?: 0)) / (1000 * 60 * 60 * 24)
                                    if (diffDays <= 1) activeCount++ else inactiveCount++
                                } catch (e: Exception) {
                                    inactiveCount++
                                }
                            } else inactiveCount++
                        }
                    }
                }

                // ✅ Update existing item and refresh adapter row
                val index = classStatsList.indexOfFirst { it.className == classStats.className }
                if (index != -1) {
                    classStatsList[index] = ClassStats(
                        className = classStats.className,
                        activeCount = activeCount,
                        inLectureCount = inLectureCount,
                        inactiveCount = inactiveCount,
                        totalStudents = activeCount + inLectureCount + inactiveCount
                    )
                    adapter.notifyItemChanged(index)
                    updateOverallCounts()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TeacherFragment", "Listener cancelled for $classCode: ${error.message}")
            }
        })

        listeners[classCode] = listener
    }

    // ✅ Updates the four count TextViews based on totals across all classes
    private fun updateOverallCounts() {
        var totalActive = 0
        var totalInLecture = 0
        var totalInactive = 0
        var totalStudents = 0

        for (stats in classStatsList) {
            totalActive += stats.activeCount
            totalInLecture += stats.inLectureCount
            totalInactive += stats.inactiveCount
            totalStudents += stats.totalStudents
        }
    }


    private fun clearAllListeners() {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        listeners.forEach { (_, l) -> usersRef.removeEventListener(l) }
        listeners.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clearAllListeners()
    }
}
