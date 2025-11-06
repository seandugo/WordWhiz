package com.example.thesis_app.ui.fragments.teacher

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.thesis_app.R
import com.example.thesis_app.TeacherActivity
import com.example.thesis_app.models.BarGraphView
import com.example.thesis_app.ui.fragments.bottomsheets.ProfileBottomSheet
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class TeacherProfileFragment : Fragment() {

    private lateinit var activeCountText: TextView
    private lateinit var inactiveCountText: TextView
    private lateinit var inLectureCountText: TextView
    private lateinit var totalCountText: TextView
    private lateinit var barGraphView: BarGraphView
    private lateinit var classTextExpanded: TextView
    private lateinit var nameTextExpanded: TextView
    private lateinit var seeAllClasses: TextView
    private lateinit var detailsContainer: LinearLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBarLayout: AppBarLayout

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val db = FirebaseDatabase.getInstance()
    private val listeners = mutableListOf<ValueEventListener>()

    @SuppressLint("SimpleDateFormat")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.teacher_profile, container, false)

        val collapsingToolbar = view.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
        toolbar = view.findViewById(R.id.toolbar)
        appBarLayout = view.findViewById(R.id.appbar)
        activeCountText = view.findViewById(R.id.activeCount)
        inactiveCountText = view.findViewById(R.id.inactiveCount)
        inLectureCountText = view.findViewById(R.id.inLectureCount)
        totalCountText = view.findViewById(R.id.studentsTotal)
        barGraphView = view.findViewById(R.id.accuracyPercentage)
        classTextExpanded = view.findViewById(R.id.classTextExpanded)
        nameTextExpanded = view.findViewById(R.id.nameTextExpanded)
        seeAllClasses = view.findViewById(R.id.seeAllClasses)
        detailsContainer = view.findViewById(R.id.detailsContainer)

        setupAppBarToolbar(collapsingToolbar, toolbar)

        // 🧠 Load teacher info
        val prefs = requireActivity().getSharedPreferences("USER_PREFS", android.content.Context.MODE_PRIVATE)
        val teacherEmail = prefs.getString("email", "Unknown") ?: "Unknown"
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val userRef = db.getReference("users").child(uid ?: "")

        userRef.child("name").get().addOnSuccessListener { snapshot ->
            val teacherName = snapshot.getValue(String::class.java) ?: "Teacher"
            collapsingToolbar.title = teacherName
            nameTextExpanded.text = teacherName
            classTextExpanded.text = teacherEmail
            setupToolbarCollapseListener(teacherName, teacherEmail)
        }

        // 🔁 Start overall realtime update
        loadOverallStudentStatisticsRealtime()

        seeAllClasses.setOnClickListener {
            (activity as? TeacherActivity)?.navigateToOverview()
        }

        return view
    }

    // ✅ Get all class codes for this teacher, then listen for changes to their students
    private fun loadOverallStudentStatisticsRealtime() {
        val teacherUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val teacherClassesRef = db.getReference("users/$teacherUid/classes")

        teacherClassesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(classesSnapshot: DataSnapshot) {
                if (!classesSnapshot.exists()) {
                    updateOverallUI(0, 0, 0, 0)
                    return
                }

                clearAllListeners() // remove old listeners
                val allStudentIds = mutableSetOf<String>()

                for (classSnap in classesSnapshot.children) {
                    val classCode = classSnap.key ?: continue
                    val classRef = db.getReference("classes/$classCode/students")

                    classRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(studentsSnap: DataSnapshot) {
                            for (studentSnap in studentsSnap.children) {
                                val studentId = studentSnap.key ?: continue
                                allStudentIds.add(studentId)
                            }
                            listenToOverallRealtime(allStudentIds)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("TeacherProfileFragment", "Failed to load students: ${error.message}")
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TeacherProfileFragment", "Classes listener cancelled: ${error.message}")
            }
        })
    }

    // ✅ Listen to overall status updates for all students combined
    private fun listenToOverallRealtime(studentIds: Set<String>) {
        val usersRef = db.getReference("users")
        val currentDate = Calendar.getInstance().time

        val listener = usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var activeCount = 0
                var inLectureCount = 0
                var inactiveCount = 0
                var totalStudents = 0

                for (studentId in studentIds) {
                    val studentSnap = snapshot.child(studentId)
                    if (!studentSnap.exists()) continue

                    val status = studentSnap.child("presence/status").getValue(String::class.java)
                    val lastActiveDateStr = studentSnap.child("activityStreak/lastActiveDate").getValue(String::class.java)

                    when (status) {
                        "online" -> activeCount++
                        "in_lecture" -> inLectureCount++
                        else -> {
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

                totalStudents = activeCount + inLectureCount + inactiveCount
                updateOverallUI(activeCount, inactiveCount, totalStudents, inLectureCount)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        listeners.add(listener)
    }

    // ✅ Update the TextViews + Graph
    // ✅ Update the TextViews + Graph
    private fun updateOverallUI(
        active: Int,
        inactive: Int,
        total: Int,
        inLecture: Int
    ) {
        activeCountText.text = active.toString()
        inactiveCountText.text = inactive.toString()
        inLectureCountText.text = inLecture.toString()
        totalCountText.text = total.toString()

        // 👇 include inLecture in the bar graph
        barGraphView.activeCount = active.toFloat()
        barGraphView.inactiveCount = inactive.toFloat()
        barGraphView.inLectureCount = inLecture.toFloat() // ✅ NEW
        barGraphView.totalStudents = total.toFloat()
        barGraphView.invalidate()
    }

    private fun setupAppBarToolbar(collapsing: CollapsingToolbarLayout, toolbar: MaterialToolbar) {
        collapsing.isTitleEnabled = false
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), android.R.color.white))
        toolbar.setNavigationOnClickListener { onSettingsIconClicked() }
    }

    private fun setupToolbarCollapseListener(name: String, email: String) {
        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, offset ->
            val collapsed = Math.abs(offset) >= appBarLayout.totalScrollRange
            toolbar.title = if (collapsed) name else ""
            toolbar.subtitle = if (collapsed) email else ""
        })
    }

    private fun onSettingsIconClicked() {
        val sheet = ProfileBottomSheet()
        sheet.show(childFragmentManager, "ProfileBottomSheet")
    }

    private fun clearAllListeners() {
        val usersRef = db.getReference("users")
        listeners.forEach { usersRef.removeEventListener(it) }
        listeners.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clearAllListeners()
    }
}
