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
import com.google.android.material.card.MaterialCardView
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
    private lateinit var detailsContainer: LinearLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBarLayout: AppBarLayout

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

        // 🔧 Properly set up toolbar and collapsing behavior
        setupAppBarToolbar(collapsingToolbar, toolbar)

        // 🧠 Get teacher info from SharedPreferences (email)
        val prefs = requireActivity().getSharedPreferences("USER_PREFS", android.content.Context.MODE_PRIVATE)
        val teacherEmail = prefs.getString("email", "Unknown") ?: "Unknown"

        // 🧾 Get teacher name from Firebase using current user ID
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid ?: "")
        userRef.child("name").get().addOnSuccessListener { snapshot ->
            val teacherName = snapshot.getValue(String::class.java) ?: "Teacher"
            // Set expanded text
            collapsingToolbar.title = teacherName
            nameTextExpanded.text = teacherName
            classTextExpanded.text = teacherEmail
            // Set toolbar title and subtitle when collapsed
            setupToolbarCollapseListener(teacherName, teacherEmail)
        }.addOnFailureListener {
            collapsingToolbar.title = "Teacher"
            nameTextExpanded.text = "Teacher"
            classTextExpanded.text = teacherEmail
            setupToolbarCollapseListener("Teacher", teacherEmail)
        }

        // 🧮 Load statistics from Firebase
        loadClassStatistics()

        // Add dynamic class content to ensure scrollable content
        loadDynamicClassContent()

        seeAllClasses.setOnClickListener {
            (activity as? TeacherActivity)?.navigateToOverview()
        }

        return view
    }

    private fun setupToolbarCollapseListener(teacherName: String, teacherEmail: String) {
        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val isCollapsed = Math.abs(verticalOffset) >= appBarLayout.totalScrollRange
            if (isCollapsed) {
                toolbar.title = teacherName
                toolbar.subtitle = teacherEmail
            } else {
                toolbar.title = ""
                toolbar.subtitle = ""
            }
        })
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
                addPlaceholderCard()
                return@addOnSuccessListener
            }

            val studentIds = mutableListOf<String>()
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
            addPlaceholderCard()
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

    private fun updateBarGraph(active: Int, inactive: Int, total: Int) {
        activeCountText.text = active.toString()
        inactiveCountText.text = inactive.toString()
        totalCountText.text = total.toString()

        barGraphView.activeCount = active.toFloat()
        barGraphView.inactiveCount = inactive.toFloat()
        barGraphView.totalStudents = total.toFloat()
        barGraphView.invalidate()
    }

    private fun loadDynamicClassContent() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val teacherClassesRef = FirebaseDatabase.getInstance().getReference("users/$uid/classes")

        teacherClassesRef.get().addOnSuccessListener { classesSnapshot ->
            if (!classesSnapshot.exists() || classesSnapshot.childrenCount == 0L) {
                addPlaceholderCard()
                return@addOnSuccessListener
            }

            val classCodes = classesSnapshot.children.mapNotNull { it.key }
            for (classCode in classCodes) {
                val classRef = FirebaseDatabase.getInstance().getReference("classes/$classCode")
                classRef.get().addOnSuccessListener { classSnap ->
                    val className = classSnap.child("className").getValue(String::class.java) ?: "Unnamed Class"
                    val studentCount = classSnap.child("students").childrenCount.toInt()
                    addClassCard(classCode, className, studentCount)
                }.addOnFailureListener {
                    addClassCard(classCode, "Unnamed Class", 0)
                }
            }
        }.addOnFailureListener {
            addPlaceholderCard()
        }
    }

    private fun addClassCard(classCode: String, className: String, studentCount: Int) {
        val cardView = MaterialCardView(requireContext()).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(48, 48, 48, 48) // 16dp margins
            }
            cardElevation = 6f
            radius = 16f
            setStrokeWidth(1)
            setStrokeColor(ContextCompat.getColor(requireContext(), R.color.gray))
        }

        val cardContent = LinearLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60) // 20dp padding
        }

        val classTitle = TextView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = "Class: $className"
            textSize = 16f
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            typeface = ResourcesCompat.getFont(requireContext(), R.font.pixel)
        }

        val classCodeText = TextView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = "Code: $classCode"
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            typeface = ResourcesCompat.getFont(requireContext(), R.font.pixel)
            setPadding(0, 8, 0, 0)
        }

        val studentCountText = TextView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = "Students: $studentCount"
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            typeface = ResourcesCompat.getFont(requireContext(), R.font.pixel)
            setPadding(0, 12, 0, 0)
        }

        cardContent.addView(classTitle)
        cardContent.addView(classCodeText)
        cardContent.addView(studentCountText)
        cardView.addView(cardContent)
        detailsContainer.addView(cardView)
    }

    private fun addPlaceholderCard() {
        val cardView = MaterialCardView(requireContext()).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(48, 48, 48, 48)
            }
            cardElevation = 6f
            radius = 16f
            setStrokeWidth(1)
            setStrokeColor(ContextCompat.getColor(requireContext(), R.color.gray))
        }

        val cardContent = LinearLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
        }

        val placeholderText = TextView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = "No classes available"
            textSize = 16f
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            typeface = ResourcesCompat.getFont(requireContext(), R.font.pixel)
        }

        cardContent.addView(placeholderText)
        cardView.addView(cardContent)
        detailsContainer.addView(cardView)
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