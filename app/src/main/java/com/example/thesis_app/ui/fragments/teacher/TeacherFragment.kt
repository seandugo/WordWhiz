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
import com.google.android.material.appbar.AppBarLayout
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
    private lateinit var emptyImage: ImageView // add this
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔧 Setup collapsing toolbar
        val appBar = view.findViewById<AppBarLayout>(R.id.appBarLayout)
        val collapsingToolbar = view.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        nameTextExpanded = view.findViewById(R.id.nameTextExpanded)
        subtitleQuiz = view.findViewById(R.id.subtitleQuiz)
        recycler = view.findViewById(R.id.resultRecycler)
        emptyImage = view.findViewById(R.id.emptyImage) // reference it

        setupAppBarToolbar(appBar, collapsingToolbar, toolbar)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        database = FirebaseDatabase.getInstance().getReference("classes")

        loadClassStatistics()
    }

    private fun setupAppBarToolbar(
        appBar: AppBarLayout,
        collapsing: CollapsingToolbarLayout,
        toolbar: MaterialToolbar
    ) {
        collapsing.isTitleEnabled = true
        collapsing.title = "" // start empty
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScroll = appBarLayout.totalScrollRange
            if (totalScroll + verticalOffset == 0) {
                // Fully collapsed
                collapsing.title = "Welcome Teacher"
                toolbar.subtitle = "Students results are in!"
            } else {
                // Expanded
                collapsing.title = ""
                toolbar.subtitle = ""
            }
        })
    }

    private fun loadClassStatistics() {
        val teacherUid = FirebaseAuth.getInstance().currentUser?.uid
        if (teacherUid == null) {
            recycler.visibility = View.GONE
            emptyImage.visibility = View.VISIBLE
            return
        }

        // Reference to the teacher's classes
        val teacherClassesRef = FirebaseDatabase.getInstance().getReference("users/$teacherUid/classes")
        teacherClassesRef.get().addOnSuccessListener { classesSnapshot ->
            val classStatsList = mutableListOf<ClassStats>()
            val currentDate = Calendar.getInstance().time

            if (!classesSnapshot.exists() || classesSnapshot.childrenCount == 0L) {
                recycler.visibility = View.GONE
                emptyImage.visibility = View.VISIBLE
                return@addOnSuccessListener
            }

            recycler.visibility = View.VISIBLE
            emptyImage.visibility = View.GONE

            var processedClasses = 0
            val totalClasses = classesSnapshot.childrenCount.toInt()

            for (classSnap in classesSnapshot.children) {
                val classCode = classSnap.key ?: continue

                // Now fetch the class data from "classes" node using classCode
                val classRef = FirebaseDatabase.getInstance().getReference("classes/$classCode")
                classRef.get().addOnSuccessListener { classDataSnap ->
                    val className = classDataSnap.child("className").getValue(String::class.java) ?: "Unnamed Class"
                    val studentsSnapshot = classDataSnap.child("students")
                    val studentIds = studentsSnapshot.children.mapNotNull { it.key }

                    getClassStatsForStudents(studentIds, currentDate) { activeCount, inactiveCount ->
                        classStatsList.add(
                            ClassStats(
                                className = className,
                                activeCount = activeCount,
                                inactiveCount = inactiveCount,
                                totalStudents = activeCount + inactiveCount
                            )
                        )
                        processedClasses++
                        if (processedClasses == totalClasses) {
                            recycler.adapter = ClassStatsAdapter(classStatsList)
                        }
                    }
                }.addOnFailureListener {
                    processedClasses++
                    if (processedClasses == totalClasses) recycler.adapter = ClassStatsAdapter(classStatsList)
                }
            }
        }.addOnFailureListener {
            recycler.visibility = View.GONE
            emptyImage.visibility = View.VISIBLE
        }
    }

    private fun getClassStatsForStudents(
        studentIds: List<String>,
        currentDate: Date,
        callback: (activeCount: Int, inactiveCount: Int) -> Unit
    ) {
        if (studentIds.isEmpty()) {
            callback(0, 0)
            return
        }

        var activeCount = 0
        var inactiveCount = 0
        var processedStudents = 0

        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        for (studentId in studentIds) {
            usersRef.child(studentId).child("activityStreak").get()
                .addOnSuccessListener { activitySnapshot ->
                    val lastActiveDateStr = activitySnapshot.child("lastActiveDate").getValue(String::class.java)
                    if (lastActiveDateStr != null) {
                        try {
                            val lastActiveDate = dateFormat.parse(lastActiveDateStr)
                            val diff = (currentDate.time - (lastActiveDate?.time ?: 0)) / (1000 * 60 * 60 * 24)
                            if (diff <= 3) activeCount++ else inactiveCount++
                        } catch (e: Exception) {
                            Log.e("TeacherFragment", "Date parse error for $studentId: $e")
                            inactiveCount++
                        }
                    } else {
                        inactiveCount++
                    }
                    processedStudents++
                    if (processedStudents == studentIds.size) callback(activeCount, inactiveCount)
                }
                .addOnFailureListener {
                    Log.e("TeacherFragment", "Error fetching student $studentId activity: $it")
                    inactiveCount++
                    processedStudents++
                    if (processedStudents == studentIds.size) callback(activeCount, inactiveCount)
                }
        }
    }
}
