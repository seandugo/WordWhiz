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

    // 🔹 Keep a reference to avoid duplicate listeners
    private val listeners = mutableListOf<ValueEventListener>()

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
        database = FirebaseDatabase.getInstance().getReference("classes")

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

    // ✅ Realtime version: actively listens for updates in students' presence/activity
    private fun loadClassStatisticsRealtime() {
        val teacherUid = FirebaseAuth.getInstance().currentUser?.uid
        if (teacherUid == null) {
            recycler.visibility = View.GONE
            emptyImage.visibility = View.VISIBLE
            return
        }

        val teacherClassesRef = FirebaseDatabase.getInstance().getReference("users/$teacherUid/classes")

        teacherClassesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(classesSnapshot: DataSnapshot) {
                val classStatsList = mutableListOf<ClassStats>()
                val currentDate = Calendar.getInstance().time

                if (!classesSnapshot.exists() || classesSnapshot.childrenCount == 0L) {
                    recycler.visibility = View.GONE
                    emptyImage.visibility = View.VISIBLE
                    return
                }

                recycler.visibility = View.VISIBLE
                emptyImage.visibility = View.GONE

                var processedClasses = 0
                val totalClasses = classesSnapshot.childrenCount.toInt()

                for (classSnap in classesSnapshot.children) {
                    val classCode = classSnap.key ?: continue
                    val classRef = FirebaseDatabase.getInstance().getReference("classes/$classCode")

                    classRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(classDataSnap: DataSnapshot) {
                            val className = classDataSnap.child("className").getValue(String::class.java) ?: "Unnamed Class"
                            val studentIds = classDataSnap.child("students").children.mapNotNull { it.key }

                            // 🔹 Listen for live changes in each class’s student stats
                            getRealtimeClassStats(studentIds, currentDate) { active, inLecture, inactive ->
                                classStatsList.add(
                                    ClassStats(
                                        className = className,
                                        activeCount = active,
                                        inactiveCount = inactive,
                                        totalStudents = active + inLecture + inactive,
                                        inLectureCount = inLecture // add this field to ClassStats
                                    )
                                )
                                processedClasses++
                                if (processedClasses == totalClasses) {
                                    recycler.adapter = ClassStatsAdapter(classStatsList)
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("TeacherFragment", "Error fetching class data: ${error.message}")
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                recycler.visibility = View.GONE
                emptyImage.visibility = View.VISIBLE
                Log.e("TeacherFragment", "Realtime listener cancelled: ${error.message}")
            }
        })
    }

    // ✅ Real-time tracker: updates instantly when any student’s presence or activity changes
    private fun getRealtimeClassStats(
        studentIds: List<String>,
        currentDate: Date,
        callback: (activeCount: Int, inLectureCount: Int, inactiveCount: Int) -> Unit
    ) {
        if (studentIds.isEmpty()) {
            callback(0, 0, 0)
            return
        }

        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        val listener = usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var activeCount = 0
                var inLectureCount = 0
                var inactiveCount = 0

                for (studentId in studentIds) {
                    val studentSnap = snapshot.child(studentId)

                    // 🟢 Check real-time presence
                    val status = studentSnap.child("presence/status").getValue(String::class.java)

                    when (status) {
                        "online" -> activeCount++
                        "in_lecture" -> inLectureCount++
                        else -> {
                            val lastActiveDateStr =
                                studentSnap.child("activityStreak/lastActiveDate").getValue(String::class.java)
                            if (lastActiveDateStr != null) {
                                try {
                                    val lastActiveDate = dateFormat.parse(lastActiveDateStr)
                                    val diffDays =
                                        (currentDate.time - (lastActiveDate?.time ?: 0)) / (1000 * 60 * 60 * 24)
                                    if (diffDays <= 1) activeCount++ else inactiveCount++
                                } catch (e: Exception) {
                                    Log.e("TeacherFragment", "Date parse error for $studentId: ${e.message}")
                                    inactiveCount++
                                }
                            } else inactiveCount++
                        }
                    }
                }

                callback(activeCount, inLectureCount, inactiveCount)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TeacherFragment", "getRealtimeClassStats cancelled: ${error.message}")
                callback(0, 0, studentIds.size)
            }
        })

        listeners.add(listener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ✅ Clean up all real-time listeners
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        listeners.forEach { usersRef.removeEventListener(it) }
        listeners.clear()
    }
}
