package com.example.thesis_app.ui.fragments.teacher

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
    private lateinit var totalCountText: TextView
    private lateinit var barGraphView: BarGraphView
    private lateinit var classTextExpanded: TextView
    private lateinit var nameTextExpanded: TextView
    private lateinit var seeAllClasses: TextView
    private lateinit var detailsContainer: LinearLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBarLayout: AppBarLayout

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var usersListener: ValueEventListener? = null
    private val usersRef = FirebaseDatabase.getInstance().getReference("users")

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
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid ?: "")

        userRef.child("name").get().addOnSuccessListener { snapshot ->
            val teacherName = snapshot.getValue(String::class.java) ?: "Teacher"
            collapsingToolbar.title = teacherName
            nameTextExpanded.text = teacherName
            classTextExpanded.text = teacherEmail
            setupToolbarCollapseListener(teacherName, teacherEmail)
        }

        // 🔁 Start real-time overall update
        listenToOverallStudentsRealtime()

        seeAllClasses.setOnClickListener {
            (activity as? TeacherActivity)?.navigateToOverview()
        }

        return view
    }

    // ✅ Real-time listener for overall student data
    private fun listenToOverallStudentsRealtime() {
        val currentDate = Calendar.getInstance().time

        usersListener = usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalStudents = 0
                var activeCount = 0
                var inactiveCount = 0

                for (userSnap in snapshot.children) {
                    val role = userSnap.child("role").getValue(String::class.java)
                    if (role != "student") continue
                    totalStudents++

                    val status = userSnap.child("presence/status").getValue(String::class.java)
                    val lastActiveDateStr = userSnap.child("activityStreak/lastActiveDate").getValue(String::class.java)

                    when (status) {
                        "online", "in_lecture" -> activeCount++
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

                updateOverallUI(activeCount, inactiveCount, totalStudents)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ✅ Update text views + bar graph in real-time
    private fun updateOverallUI(active: Int, inactive: Int, total: Int) {
        activeCountText.text = active.toString()
        inactiveCountText.text = inactive.toString()
        totalCountText.text = total.toString()

        barGraphView.activeCount = active.toFloat()
        barGraphView.inactiveCount = inactive.toFloat()
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

    override fun onDestroyView() {
        super.onDestroyView()
        usersListener?.let { usersRef.removeEventListener(it) }
    }
}
