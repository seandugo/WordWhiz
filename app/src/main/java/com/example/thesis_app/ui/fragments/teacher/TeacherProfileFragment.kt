package com.example.thesis_app.ui.fragments.teacher

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class TeacherProfileFragment : Fragment() {

    private lateinit var activeCountText: TextView
    private lateinit var inactiveCountText: TextView
    private lateinit var totalCountText: TextView
    private lateinit var barGraphView: BarGraphView
    private lateinit var classTextExpanded: TextView
    private lateinit var nameTextExpanded: TextView
    private lateinit var seeAllClasses: TextView

    @SuppressLint("SimpleDateFormat")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.teacher_profile, container, false)

        val collapsingToolbar = view.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        activeCountText = view.findViewById(R.id.activeCount)
        inactiveCountText = view.findViewById(R.id.inactiveCount)
        totalCountText = view.findViewById(R.id.studentsTotal)
        barGraphView = view.findViewById(R.id.accuracyPercentage)
        classTextExpanded = view.findViewById(R.id.classTextExpanded)
        nameTextExpanded = view.findViewById(R.id.nameTextExpanded)
        seeAllClasses = view.findViewById(R.id.seeAllClasses)

        // 🔧 Properly set up toolbar and collapsing behavior
        setupAppBarToolbar(collapsingToolbar, toolbar)

        // 🧠 Get teacher info from SharedPreferences (email)
        val prefs = requireActivity().getSharedPreferences("USER_PREFS", android.content.Context.MODE_PRIVATE)
        val teacherEmail = prefs.getString("email", "Unknown")
        classTextExpanded.text = teacherEmail

        // 🧾 Get teacher name from Firebase using current user ID
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid ?: "")
        userRef.child("name").get().addOnSuccessListener { snapshot ->
            val teacherName = snapshot.getValue(String::class.java) ?: "Teacher"
            collapsingToolbar.title = teacherName
            nameTextExpanded.text = teacherName
        }.addOnFailureListener {
            collapsingToolbar.title = "Teacher"
            nameTextExpanded.text = "Teacher"
        }

        // 🧮 Load statistics from Firebase
        loadClassStatistics()

        seeAllClasses.setOnClickListener {
            (activity as? TeacherActivity)?.navigateToOverview()
        }

        return view
    }

    @SuppressLint("SimpleDateFormat")
    private fun loadClassStatistics() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val teacherClassesRef = FirebaseDatabase.getInstance().getReference("users/$uid/classes")
        val dbUsers = FirebaseDatabase.getInstance().getReference("users")
        val dbClasses = FirebaseDatabase.getInstance().getReference("classes")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val now = Date()

        teacherClassesRef.get().addOnSuccessListener { classesSnapshot ->
            if (!classesSnapshot.exists() || classesSnapshot.childrenCount == 0L) {
                updateBarGraph(0, 0, 0)
                return@addOnSuccessListener
            }

            val studentIds = mutableListOf<String>()

            // Fetch students only from this teacher's classes
            val classCodes = classesSnapshot.children.mapNotNull { it.key }
            var processedClasses = 0

            for (classCode in classCodes) {
                dbClasses.child(classCode).child("students").get()
                    .addOnSuccessListener { studentsSnap ->
                        for (studentSnap in studentsSnap.children) {
                            studentSnap.key?.let { studentIds.add(it) }
                        }
                        processedClasses++
                        if (processedClasses == classCodes.size) {
                            // All classes processed, now calculate active/inactive
                            calculateActivityCounts(studentIds, dbUsers, sdf, now)
                        }
                    }
                    .addOnFailureListener {
                        processedClasses++
                        if (processedClasses == classCodes.size) {
                            calculateActivityCounts(studentIds, dbUsers, sdf, now)
                        }
                    }
            }
        }.addOnFailureListener {
            updateBarGraph(0, 0, 0)
        }
    }

    private fun calculateActivityCounts(
        studentIds: List<String>,
        dbUsers: DatabaseReference,
        sdf: SimpleDateFormat,
        now: Date
    ) {
        var totalStudents = studentIds.size
        var activeCount = 0
        var inactiveCount = 0

        if (totalStudents == 0) {
            updateBarGraph(0, 0, 0)
            return
        }

        var processed = 0
        for (studentId in studentIds) {
            dbUsers.child(studentId).child("activityStreak").get()
                .addOnSuccessListener { activitySnap ->
                    val lastActiveDateStr = activitySnap.child("lastActiveDate").getValue(String::class.java)
                    if (lastActiveDateStr != null) {
                        try {
                            val lastActive = sdf.parse(lastActiveDateStr)
                            val diffDays = ((now.time - lastActive.time) / (1000 * 60 * 60 * 24))
                            if (diffDays <= 3) activeCount++ else inactiveCount++
                        } catch (e: Exception) {
                            inactiveCount++
                        }
                    } else {
                        inactiveCount++
                    }
                    processed++
                    if (processed == totalStudents) {
                        updateBarGraph(activeCount, inactiveCount, totalStudents)
                    }
                }
                .addOnFailureListener {
                    inactiveCount++
                    processed++
                    if (processed == totalStudents) {
                        updateBarGraph(activeCount, inactiveCount, totalStudents)
                    }
                }
        }
    }

    // Helper function to update UI & bar graph
    private fun updateBarGraph(active: Int, inactive: Int, total: Int) {
        activeCountText.text = active.toString()
        inactiveCountText.text = inactive.toString()
        totalCountText.text = total.toString()

        barGraphView.activeCount = active.toFloat()
        barGraphView.inactiveCount = inactive.toFloat()
        barGraphView.totalStudents = total.toFloat()
        barGraphView.invalidate()
    }


    private fun setupAppBarToolbar(
        collapsing: CollapsingToolbarLayout,
        toolbar: MaterialToolbar
    ) {
        collapsing.isTitleEnabled = false
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), android.R.color.white))
        toolbar.setNavigationOnClickListener {
            onSettingsIconClicked()
        }
    }

    private fun onSettingsIconClicked() {
        val bottomSheet = ProfileBottomSheet()
        bottomSheet.show(childFragmentManager, "ProfileBottomSheet")
    }
}
