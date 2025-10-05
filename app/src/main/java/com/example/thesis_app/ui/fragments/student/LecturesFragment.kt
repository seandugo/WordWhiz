package com.example.thesis_app.ui.fragments.student

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.QuizListAdapter
import com.example.thesis_app.R
import com.example.thesis_app.models.QuestionModel
import com.example.thesis_app.models.QuizDisplayItem
import com.example.thesis_app.models.QuizPartItem
import com.google.firebase.database.FirebaseDatabase

class LecturesFragment : Fragment() {

    private lateinit var quizPartList: MutableList<QuizDisplayItem>
    private lateinit var adapter: QuizListAdapter
    private lateinit var topAppBar: com.google.android.material.appbar.MaterialToolbar
    private lateinit var progressBar: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var headerLayout: View
    private lateinit var headerTitle: TextView
    private lateinit var headerSubtitle: TextView
    private lateinit var displayList: MutableList<QuizDisplayItem>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.courses, container, false)
        topAppBar = view.findViewById(R.id.topAppBar)
        progressBar = view.findViewById(R.id.progress_bar)
        recyclerView = view.findViewById(R.id.recycler_view)
        headerLayout = view.findViewById(R.id.headerLayout)
        headerTitle = view.findViewById(R.id.headerTitle)
        headerSubtitle = view.findViewById(R.id.headerSubtitle)

        topAppBar.title = "Welcome Student!"
        topAppBar.setTitleTextAppearance(requireContext(), R.style.ToolbarTitleText)

        quizPartList = mutableListOf()
        getDataFromFirebase()

        return view
    }

    private fun setupRecyclerView() {
        if (!isAdded) return

        progressBar.visibility = View.GONE

        val prefs = requireContext().getSharedPreferences("USER_PREFS", 0)
        val studentId = prefs.getString("studentId", null)

        adapter = QuizListAdapter(displayList, studentId ?: "", requireActivity())
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        // ✅ Initialize header at startup with first unlocked quiz part
        val firstPart = displayList.firstOrNull { it is QuizDisplayItem.Part } as? QuizDisplayItem.Part
        firstPart?.let {
            headerTitle.text = it.item.quizTitle
            headerSubtitle.text = it.item.quizSubtitle
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
                if (firstVisiblePos != RecyclerView.NO_POSITION && firstVisiblePos < displayList.size) {
                    val currentItem = displayList[firstVisiblePos]
                    if (currentItem is QuizDisplayItem.Part) {
                        headerTitle.text = currentItem.item.quizTitle
                        headerSubtitle.text = currentItem.item.quizSubtitle
                    }
                }
            }
        })
    }

    private fun getDataFromFirebase() {
        progressBar.visibility = View.VISIBLE
        displayList = mutableListOf()

        val prefs = requireContext().getSharedPreferences("USER_PREFS", 0)
        val studentId = prefs.getString("studentId", null) ?: ""

        val dbRef = FirebaseDatabase.getInstance().reference.child("quizzes")

        dbRef.get().addOnSuccessListener { database ->
            if (!database.exists()) return@addOnSuccessListener

            for (quizSnapshot in database.children) {
                val quizId = quizSnapshot.key ?: ""

                // Skip pretest Quiz1 entirely
                if (quizId == "quiz1") continue

                val title = quizSnapshot.child("title").getValue(String::class.java) ?: ""
                val subtitle = quizSnapshot.child("subtitle").getValue(String::class.java) ?: ""

                if (title.isNotEmpty()) {
                    displayList.add(QuizDisplayItem.Divider(title))
                }

                var allPartsCompleted = true
                var firstPartUnlocked = false

                for (partSnapshot in quizSnapshot.children) {
                    val rawPartId = partSnapshot.key ?: continue
                    if (!rawPartId.startsWith("part")) continue  // skip title/subtitle

                    val questions = mutableListOf<QuestionModel>()
                    for (qSnap in partSnapshot.child("questionList").children) {
                        qSnap.getValue(QuestionModel::class.java)?.let { questions.add(it) }
                    }

                    val partId = rawPartId
                    val displayName = "Level " + partId.filter { it.isDigit() }

                    // Check user progress for this part
                    val progressSnapshot = FirebaseDatabase.getInstance().reference
                        .child("users").child(studentId)
                        .child("progress").child(quizId).child(partId)

                    progressSnapshot.get().addOnSuccessListener { snap ->
                        val answered = snap.child("answeredCount").getValue(Int::class.java) ?: 0
                        val total = snap.child("totalQuestions").getValue(Int::class.java) ?: questions.size
                        val isCompleted = total > 0 && answered >= total

                        // Track if all parts are completed
                        if (!isCompleted) allPartsCompleted = false

                        // Unlock first part if this is the first quiz unlocked
                        if (!firstPartUnlocked) {
                            firstPartUnlocked = true
                        }

                        // After last part processed, update quiz-level completion
                        if (partSnapshot == quizSnapshot.children.last { it.key?.startsWith("part") == true }) {
                            FirebaseDatabase.getInstance().reference
                                .child("users").child(studentId)
                                .child("progress").child(quizId)
                                .child("isCompleted")
                                .setValue(allPartsCompleted)
                        }
                    }

                    val isUnlocked = firstPartUnlocked || allPartsCompleted

                    displayList.add(
                        QuizDisplayItem.Part(
                            QuizPartItem(
                                quizId = quizId,
                                quizTitle = title,
                                quizSubtitle = subtitle,
                                partId = partId,
                                displayName = displayName,
                                questions = questions,
                                isUnlocked = isUnlocked
                            )
                        )
                    )
                }
            }

            setupRecyclerView()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        quizPartList.clear()
    }
}
