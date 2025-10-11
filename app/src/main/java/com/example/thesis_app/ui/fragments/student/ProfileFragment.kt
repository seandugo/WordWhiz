package com.example.thesis_app.ui.fragments.student

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.ProgressListActivity
import com.example.thesis_app.R
import com.example.thesis_app.ui.CarouselAdapter
import com.example.thesis_app.ProgressListAdapter
import com.example.thesis_app.QuizDetailActivity
import com.example.thesis_app.SpellingAchievementsActivity
import com.example.thesis_app.models.ProgressItem
import com.example.thesis_app.ui.fragments.bottomsheets.ProfileBottomSheet
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.database.FirebaseDatabase

class ProfileFragment : Fragment() {

    private lateinit var progressCarousel: RecyclerView
    private lateinit var spellingCarousel: RecyclerView
    private lateinit var progressAdapter: ProgressListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.profile, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireActivity().getSharedPreferences("USER_PREFS", AppCompatActivity.MODE_PRIVATE)
        val studentName = prefs.getString("studentName", "Unknown")
        val studentClass = prefs.getString("studentClass", "No Class")
        val studentId = prefs.getString("studentId", "No Student Id") ?: ""

        val appBar = view.findViewById<AppBarLayout>(R.id.appbar)
        val collapsing = view.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val nameExpanded = view.findViewById<TextView>(R.id.nameTextExpanded)
        val classExpanded = view.findViewById<TextView>(R.id.classTextExpanded)
        val studentIdExpanded = view.findViewById<TextView>(R.id.studentIdExpanded)
        val lecturesCount = view.findViewById<TextView>(R.id.lecturesCount)
        val daysStreak = view.findViewById<TextView>(R.id.daysStreak)
        val spellingCount = view.findViewById<TextView>(R.id.spellingCount)

        progressCarousel = view.findViewById(R.id.progressCarousel)
        spellingCarousel = view.findViewById(R.id.achievementsCarousel)

        nameExpanded.text = studentName
        classExpanded.text = "Class: $studentClass"
        studentIdExpanded.text = "Id: $studentId"

        progressCarousel.isNestedScrollingEnabled = false
        spellingCarousel.isNestedScrollingEnabled = false

        setupAchievementsCarousel(studentId)
        setupAppBarToolbar(appBar, collapsing, toolbar, nameExpanded, classExpanded)

        progressAdapter = ProgressListAdapter { part ->
            val intent = Intent(requireContext(), QuizDetailActivity::class.java)
            intent.putExtra("levelName", part.levelName)
            intent.putExtra("studentId", studentId)
            intent.putExtra("quizId","129503")
            startActivity(intent)
        }
        progressCarousel.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        progressCarousel.adapter = progressAdapter


        val seeAllProgress = view.findViewById<TextView>(R.id.seeAllProgress)
        val seeAllSpelling = view.findViewById<TextView>(R.id.seeAllSpelling)
        seeAllProgress.setOnClickListener {
            val intent = Intent(requireContext(), ProgressListActivity::class.java)
            intent.putExtra("studentId", studentId)
            startActivity(intent)
        }
        seeAllSpelling.setOnClickListener {
            val intent = Intent(requireContext(), SpellingAchievementsActivity::class.java)
            intent.putExtra("studentId", studentId)
            startActivity(intent)
        }

        fetchLecturesCount(studentId)
        fetchDaysStreak(studentId)
        fetchSpellingCount(studentId)
        fetchProgressData(studentId)
    }

    private fun fetchLecturesCount(studentId: String) {
        val progressRef = FirebaseDatabase.getInstance()
            .getReference("users/$studentId/progress")

        progressRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                view?.findViewById<TextView>(R.id.lecturesCount)?.text = "0"
                return@addOnSuccessListener
            }

            var completedCount = 0

            snapshot.children.forEach { quizSnapshot ->
                val quizId = quizSnapshot.key ?: return@forEach
                if (quizId == "835247") return@forEach // 🚫 Skip this quiz

                val isCompleted = quizSnapshot.child("isCompleted").getValue(Boolean::class.java) ?: false
                if (isCompleted) completedCount++
            }

            view?.findViewById<TextView>(R.id.lecturesCount)?.text = completedCount.toString()
        }.addOnFailureListener {
            view?.findViewById<TextView>(R.id.lecturesCount)?.text = "0"
        }
    }

    private fun fetchSpellingCount(studentId: String) {
        val progressRef = FirebaseDatabase.getInstance()
            .getReference("users/$studentId/spellingActivity/savedWords")

        progressRef.get().addOnSuccessListener { snapshot ->
            val count = snapshot.children.count {
                val skipped = it.child("skipped").getValue(Boolean::class.java) ?: false
                !skipped
            }
            view?.findViewById<TextView>(R.id.spellingCount)?.text = "$count"
        }.addOnFailureListener {
            view?.findViewById<TextView>(R.id.spellingCount)?.text = "0"
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchDaysStreak(studentId: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("users/$studentId/activityStreak")
        val today = java.time.LocalDate.now().toString()

        userRef.get().addOnSuccessListener { snapshot ->
            var streakCount = snapshot.child("streakCount").getValue(Int::class.java) ?: 0
            val lastActiveDate = snapshot.child("lastActiveDate").getValue(String::class.java)

            val yesterday = java.time.LocalDate.now().minusDays(1).toString()

            if (lastActiveDate == today) {
                // Already active today → do nothing
            } else if (lastActiveDate == yesterday) {
                // Continued streak
                streakCount += 1
                userRef.child("streakCount").setValue(streakCount)
                userRef.child("lastActiveDate").setValue(today)
            } else {
                // Reset streak
                streakCount = 1
                userRef.child("streakCount").setValue(1)
                userRef.child("lastActiveDate").setValue(today)
            }

            view?.findViewById<TextView>(R.id.daysStreak)?.text = "$streakCount"
        }.addOnFailureListener {
            view?.findViewById<TextView>(R.id.daysStreak)?.text = "0"
        }
    }


    private fun fetchProgressData(studentId: String) {
        val quizId = "129503" // Only this quiz
        val quizzesRef = FirebaseDatabase.getInstance().getReference("quizzes/$quizId")
        val userProgressRef = FirebaseDatabase.getInstance().getReference("users/$studentId/progress/$quizId")

        quizzesRef.get().addOnSuccessListener { quizSnapshot ->
            val quizTitle = quizSnapshot.child("title").getValue(String::class.java) ?: "Untitled Quiz"

            userProgressRef.get().addOnSuccessListener { progressSnapshot ->
                val newItems = mutableListOf<ProgressItem.Part>()

                if (progressSnapshot.exists()) {
                    // Calculate completed parts
                    val partKeys = progressSnapshot.children.mapNotNull { it.key }
                        .filter { it.startsWith("part") || it == "post-test" }

                    val completedParts = progressSnapshot.children.count { partSnapshot ->
                        partSnapshot.child("isCompleted").getValue(Boolean::class.java) == true
                    }

                    newItems.add(
                        ProgressItem.Part(
                            levelName = quizTitle,
                            totalParts = partKeys.size,
                            completedParts = completedParts,
                            quizId = quizId
                        )
                    )
                }

                // Update the adapter
                progressAdapter.updateData(newItems)
            }.addOnFailureListener {
                it.printStackTrace()
                progressAdapter.updateData(emptyList())
            }
        }.addOnFailureListener {
            it.printStackTrace()
            progressAdapter.updateData(emptyList())
        }
    }

    private fun setupAchievementsCarousel(studentId: String) {
        val progressRef = FirebaseDatabase.getInstance()
            .getReference("users/$studentId/spellingActivity/savedWords")

        progressRef.get().addOnSuccessListener { snapshot ->
            // Count only words where "skipped" == false
            val count = snapshot.children.count { child ->
                val skipped = child.child("skipped").getValue(Boolean::class.java) ?: false
                !skipped
            }

            val emptySpellingText = view?.findViewById<TextView>(R.id.emptySpellingText)

            if (count > 0) {
                // Pick drawable based on count
                val drawableRes = when (count) {
                    in 1..5 -> R.drawable.spelling
                    in 6..10 -> R.drawable.five_spells
                    in 11..15 -> R.drawable.ten_spells
                    in 16..20 -> R.drawable.fifteen_spells
                    in 21..25 -> R.drawable.twenty_spells
                    in 26..30 -> R.drawable.twenty_five_spells
                    in 31..35 -> R.drawable.thirty_spells
                    in 36..40 -> R.drawable.english_ex
                    in 41..Int.MAX_VALUE -> R.drawable.english_adventurer
                    else -> R.drawable.spelling
                }

                val spellingItems = listOf(drawableRes to "$count Words Spelled")

                // Show carousel
                spellingCarousel.visibility = View.VISIBLE
                emptySpellingText?.visibility = View.GONE

                spellingCarousel.apply {
                    layoutManager = object : LinearLayoutManager(context, HORIZONTAL, false) {
                        override fun canScrollHorizontally(): Boolean = false
                    }
                    adapter = CarouselAdapter(spellingItems)
                }
            } else {
                // No saved (non-skipped) words → show placeholder
                spellingCarousel.visibility = View.GONE
                emptySpellingText?.visibility = View.VISIBLE
                emptySpellingText?.text = "Start Spelling More!"
            }
        }.addOnFailureListener {
            it.printStackTrace()
            spellingCarousel.visibility = View.GONE
            view?.findViewById<TextView>(R.id.emptySpellingText)?.apply {
                visibility = View.VISIBLE
                text = "Start Spelling More!"
            }
        }
    }

    private fun setupAppBarToolbar(
        appBar: AppBarLayout,
        collapsing: CollapsingToolbarLayout,
        toolbar: MaterialToolbar,
        nameExpanded: TextView,
        classExpanded: TextView
    ) {
        collapsing.isTitleEnabled = false
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), android.R.color.white))
        toolbar.setNavigationOnClickListener {
            onSettingsIconClicked()
        }

        var isTitleShown = false
        var scrollRange = -1

        appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (scrollRange == -1) scrollRange = appBarLayout.totalScrollRange

            if (scrollRange + verticalOffset == 0) {
                // Collapsed state
                toolbar.title = nameExpanded.text
                toolbar.subtitle = classExpanded.text
                toolbar.setTitleTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                toolbar.setSubtitleTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                nameExpanded.visibility = View.GONE
                classExpanded.visibility = View.GONE
                isTitleShown = true

                // Change navigation icon to black
                toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), android.R.color.black))

            } else if (isTitleShown) {
                // Expanded state
                toolbar.title = ""
                toolbar.subtitle = ""
                nameExpanded.visibility = View.VISIBLE
                classExpanded.visibility = View.VISIBLE
                isTitleShown = false

                // Change navigation icon to white
                toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        })
    }

    private fun onSettingsIconClicked() {
        val bottomSheet = ProfileBottomSheet()
        bottomSheet.show(childFragmentManager, "ProfileBottomSheet")
    }

}
