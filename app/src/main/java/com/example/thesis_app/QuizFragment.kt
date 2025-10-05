package com.example.thesis_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.QuestionModel
import com.example.thesis_app.models.QuizDisplayItem
import com.example.thesis_app.models.QuizPartItem
import com.google.firebase.database.FirebaseDatabase

class QuizFragment : Fragment() {

    private lateinit var displayList: MutableList<QuizDisplayItem>
    private lateinit var adapter: QuizListAdapter
    private lateinit var progressBar: View
    private lateinit var recyclerView: RecyclerView

    // 🔹 Cache progress and unlock data
    private val progressCache = mutableMapOf<String, Pair<Int, Boolean>>() // key = "quiz_part"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.courses, container, false)

        progressBar = view.findViewById(R.id.progress_bar)
        recyclerView = view.findViewById(R.id.recycler_view)

        displayList = mutableListOf()

        // Get quizzes and preload progress
        getUserProgressThenLoadQuizzes()

        return view
    }

    private fun getUserProgressThenLoadQuizzes() {
        progressBar.visibility = View.VISIBLE

        val sharedPrefs = requireContext().getSharedPreferences("USER_PREFS", 0)
        val studentId = sharedPrefs.getString("studentId", "") ?: ""

        val dbRef = FirebaseDatabase.getInstance().reference
        val userProgressRef = dbRef.child("users").child(studentId).child("progress")

        // 🧠 Step 1: Fetch user progress first
        userProgressRef.get().addOnSuccessListener { snapshot ->

            // Store all parts’ progress and unlock status
            snapshot.children.forEach { quizNode ->
                val quizId = quizNode.key ?: return@forEach

                val quizKeys = snapshot.children.map { it.key ?: "" }.sorted()
                val quizIndex = quizKeys.indexOf(quizId)
                val previousQuizCompleted = if (quizIndex == 0) true
                else snapshot.child(quizKeys[quizIndex - 1]).child("isCompleted")
                    .getValue(Boolean::class.java) ?: false

                val partsList = quizNode.children
                    .filter { it.key?.startsWith("part") == true }
                    .map { it.key!! }
                    .sorted()

                for ((index, partId) in partsList.withIndex()) {
                    val answered = snapshot.child("$quizId/$partId/answeredCount")
                        .getValue(Int::class.java) ?: 0
                    val previousPartCompleted = if (index == 0) true
                    else snapshot.child("$quizId/${partsList[index - 1]}/isCompleted")
                        .getValue(Boolean::class.java) ?: false

                    val isUnlocked = previousQuizCompleted && previousPartCompleted
                    progressCache["${quizId}_${partId}"] = Pair(answered, isUnlocked)
                }
            }

            // After loading progress, load quizzes
            getQuizzesFromFirebase(studentId)

        }.addOnFailureListener {
            // If failed, just load quizzes normally
            getQuizzesFromFirebase(studentId)
        }
    }

    private fun getQuizzesFromFirebase(studentId: String) {
        FirebaseDatabase.getInstance().reference
            .child("quizzes")
            .get()
            .addOnSuccessListener { quizSnapshot ->
                displayList.clear()

                if (quizSnapshot.exists()) {
                    val quizChildren = quizSnapshot.children.toList()

                    for ((index, quizNode) in quizChildren.withIndex()) {
                        val quizId = quizNode.key ?: ""
                        val title = quizNode.child("title").getValue(String::class.java) ?: ""
                        val subtitle = quizNode.child("subtitle").getValue(String::class.java) ?: ""

                        // Loop through parts
                        val partsNode = quizNode.child("parts")
                        for (partSnapshot in partsNode.children) {
                            val partId = partSnapshot.key ?: ""
                            val questions = mutableListOf<QuestionModel>()

                            val questionListSnapshot = partSnapshot.child("questionList")
                            for (qSnap in questionListSnapshot.children) {
                                qSnap.getValue(QuestionModel::class.java)?.let { questions.add(it) }
                            }

                            // Convert part1 → Level 1
                            val levelName = "Level " + partId.filter { it.isDigit() }

                            displayList.add(
                                QuizDisplayItem.Part(
                                    QuizPartItem(
                                        quizId = quizId,
                                        quizTitle = title,
                                        quizSubtitle = subtitle,
                                        partId = levelName,
                                        questions = questions
                                    )
                                )
                            )
                        }

                        // Add divider before next quiz
                        if (index < quizChildren.size - 1) {
                            val nextTitle =
                                quizChildren[index + 1].child("title").getValue(String::class.java)
                                    ?: ""
                            if (nextTitle.isNotEmpty()) {
                                displayList.add(QuizDisplayItem.Divider(nextTitle))
                            }
                        }
                    }
                }

                setupRecyclerView(studentId)
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
            }
    }

    private fun setupRecyclerView(studentId: String) {
        progressBar.visibility = View.GONE
        adapter = QuizListAdapter(displayList, studentId, requireActivity())

        // 🧩 Pass preloaded progress cache to adapter
        adapter.setPreloadedProgress(progressCache)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
}
