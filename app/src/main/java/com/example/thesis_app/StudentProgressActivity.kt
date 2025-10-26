package com.example.thesis_app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.ui.CarouselAdapter
import com.example.thesis_app.models.ProgressItem
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.database.FirebaseDatabase

class StudentProgressActivity : AppCompatActivity() {

    private lateinit var progressCarousel: RecyclerView
    private lateinit var achievementsCarousel: RecyclerView
    private lateinit var progressAdapter: ProgressListAdapter

    private lateinit var headerName: TextView
    private lateinit var headerClass: TextView
    private lateinit var headerCode: TextView
    private lateinit var lecturesCount: TextView
    private lateinit var daysStreak: TextView
    private lateinit var spellingCount: TextView
    private lateinit var emptySpellingText: TextView

    private lateinit var studentId: String
    private lateinit var className: String
    private lateinit var studentName: String

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student_progress)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        headerName = findViewById(R.id.headerStudentName)
        headerClass = findViewById(R.id.headerStudentClass)
        headerCode = findViewById(R.id.headerStudentCode)
        lecturesCount = findViewById(R.id.lecturesCount)
        daysStreak = findViewById(R.id.daysStreak)
        spellingCount = findViewById(R.id.spellingCount)
        emptySpellingText = findViewById(R.id.emptySpellingText)

        progressCarousel = findViewById(R.id.progressCarousel)
        achievementsCarousel = findViewById(R.id.achievementsCarousel)

        studentName = intent.getStringExtra(EXTRA_NAME) ?: "Unknown"
        className = intent.getStringExtra(EXTRA_CLASS) ?: "N/A"
        studentId = intent.getStringExtra(EXTRA_CODE) ?: "N/A"

        headerName.text = studentName
        headerClass.text = "Class: $className"
        headerCode.text = "Student ID: $studentId"

        progressAdapter = ProgressListAdapter { part ->
            val intent = Intent(this, QuizDetailActivity::class.java)
            intent.putExtra("levelName", part.levelName)
            intent.putExtra("studentId", studentId)
            intent.putExtra("quizId","129503")
            startActivity(intent)
        }

        progressCarousel.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        progressCarousel.adapter = progressAdapter
        progressCarousel.isNestedScrollingEnabled = false
        achievementsCarousel.isNestedScrollingEnabled = false

        findViewById<TextView>(R.id.seeAllProgress).setOnClickListener {
            val intent = Intent(this, ProgressListActivity::class.java)
            intent.putExtra("studentId", studentId)
            startActivity(intent)
        }
        findViewById<TextView>(R.id.seeAllSpelling).setOnClickListener {
            val intent = Intent(this, SpellingAchievementsActivity::class.java)
            intent.putExtra("studentId", studentId)
            startActivity(intent)
        }

        val appBar = findViewById<AppBarLayout>(R.id.appbar)
        val collapsing = findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
        setupAppBarToolbar(appBar, collapsing, toolbar)

        fetchLecturesCount()
        fetchDaysStreak()
        fetchSpellingCount()
        fetchProgressData()
        setupAchievementsCarousel()
    }

    private fun fetchLecturesCount() {
        val progressRef = FirebaseDatabase.getInstance()
            .getReference("users/$studentId/progress")

        progressRef.get().addOnSuccessListener { snapshot ->
            var completedCount = 0
            snapshot.children.forEach { quizSnap ->
                val quizId = quizSnap.key ?: return@forEach
                if (quizId == "835247") return@forEach
                val isCompleted = quizSnap.child("isCompleted").getValue(Boolean::class.java) ?: false
                if (isCompleted) completedCount++
            }
            lecturesCount.text = completedCount.toString()
        }.addOnFailureListener {
            lecturesCount.text = "0"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchDaysStreak() {
        val userRef = FirebaseDatabase.getInstance()
            .getReference("users/$studentId/activityStreak")

        userRef.get().addOnSuccessListener { snapshot ->
            val streakCount = snapshot.child("streakCount").getValue(Int::class.java) ?: 0
            daysStreak.text = streakCount.toString()
        }.addOnFailureListener {
            daysStreak.text = "0"
        }
    }

    private fun fetchSpellingCount() {
        val ref = FirebaseDatabase.getInstance()
            .getReference("users/$studentId/spellingActivity/savedWords")
        ref.get().addOnSuccessListener { snapshot ->
            val count = snapshot.children.count { it.child("skipped").getValue(Boolean::class.java) == false }
            spellingCount.text = count.toString()
        }.addOnFailureListener {
            spellingCount.text = "0"
        }
    }

    private fun fetchProgressData() {
        val quizId = "129503"
        val quizzesRef = FirebaseDatabase.getInstance().getReference("quizzes/$quizId")
        val userProgressRef = FirebaseDatabase.getInstance().getReference("users/$studentId/progress/$quizId")

        quizzesRef.get().addOnSuccessListener { quizSnap ->
            val quizTitle = quizSnap.child("title").getValue(String::class.java) ?: "Untitled Quiz"
            userProgressRef.get().addOnSuccessListener { progSnap ->
                val newItems = mutableListOf<ProgressItem.Part>()
                if (progSnap.exists()) {
                    val partKeys = progSnap.children.mapNotNull { it.key }
                        .filter { it.startsWith("part") || it == "post-test" }
                    val completedParts = progSnap.children.count { it.child("isCompleted").getValue(Boolean::class.java) == true }
                    newItems.add(ProgressItem.Part(
                        levelName = quizTitle,
                        totalParts = partKeys.size,
                        completedParts = completedParts,
                        quizId = quizId
                    ))
                }
                progressAdapter.updateData(newItems)
            }.addOnFailureListener { progressAdapter.updateData(emptyList()) }
        }.addOnFailureListener { progressAdapter.updateData(emptyList()) }
    }

    private fun setupAchievementsCarousel() {
        val ref = FirebaseDatabase.getInstance()
            .getReference("users/$studentId/spellingActivity/savedWords")
        ref.get().addOnSuccessListener { snapshot ->
            val count = snapshot.children.count { it.child("skipped").getValue(Boolean::class.java) == false }
            if (count > 0) {
                val drawableRes = when (count) {
                    in 1..5 -> R.drawable.spelling
                    in 6..10 -> R.drawable.five_spells
                    in 11..15 -> R.drawable.ten_spells
                    in 16..20 -> R.drawable.fifteen_spells
                    in 21..25 -> R.drawable.twenty_spells
                    in 26..30 -> R.drawable.twenty_five_spells
                    in 31..35 -> R.drawable.thirty_spells
                    in 36..40 -> R.drawable.english_ex
                    else -> R.drawable.english_adventurer
                }
                achievementsCarousel.visibility = View.VISIBLE
                emptySpellingText.visibility = View.GONE
                achievementsCarousel.layoutManager = object : LinearLayoutManager(this, HORIZONTAL, false) {
                    override fun canScrollHorizontally(): Boolean = false
                }
                achievementsCarousel.adapter = CarouselAdapter(listOf(drawableRes to "$count Words Spelled"))
            } else {
                achievementsCarousel.visibility = View.GONE
                emptySpellingText.visibility = View.VISIBLE
                emptySpellingText.text = "Start Spelling More!"
            }
        }.addOnFailureListener {
            achievementsCarousel.visibility = View.GONE
            emptySpellingText.visibility = View.VISIBLE
            emptySpellingText.text = "Start Spelling More!"
        }
    }

    private fun setupAppBarToolbar(appBar: AppBarLayout, collapsing: CollapsingToolbarLayout, toolbar: MaterialToolbar) {
        collapsing.isTitleEnabled = false
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.white))
        toolbar.setNavigationOnClickListener { finish() }

        var isTitleShown = false
        var scrollRange = -1
        appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { bar, verticalOffset ->
            if (scrollRange == -1) scrollRange = bar.totalScrollRange
            if (scrollRange + verticalOffset == 0) {
                toolbar.title = studentName
                toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.black))
                headerName.visibility = View.GONE
                headerClass.visibility = View.GONE
                isTitleShown = true
                toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.black))
            } else if (isTitleShown) {
                toolbar.title = ""
                headerName.visibility = View.VISIBLE
                headerClass.visibility = View.VISIBLE
                isTitleShown = false
                toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.white))
            }
        })
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_CLASS = "extra_class"
        private const val EXTRA_CODE = "extra_code"

        fun start(context: Context, name: String, className: String, studentId: String) {
            val intent = Intent(context, StudentProgressActivity::class.java).apply {
                putExtra(EXTRA_NAME, name)
                putExtra(EXTRA_CLASS, className)
                putExtra(EXTRA_CODE, studentId)
            }
            context.startActivity(intent)
        }
    }
}
