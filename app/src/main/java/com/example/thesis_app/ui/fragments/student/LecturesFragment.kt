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
import com.example.thesis_app.models.QuizDisplayItem
import com.example.thesis_app.models.QuizMeta
import com.google.firebase.database.*

class LecturesFragment : Fragment() {

    private lateinit var adapter: QuizListAdapter
    private lateinit var topAppBar: com.google.android.material.appbar.MaterialToolbar
    private lateinit var progressBar: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var headerTitle: TextView
    private lateinit var headerSubtitle: TextView
    private lateinit var displayList: MutableList<QuizDisplayItem>

    private lateinit var quizzesRef: DatabaseReference
    private lateinit var progressRef: DatabaseReference
    private lateinit var studentId: String

    private val quizzesListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (!isAdded) return
            buildDisplayList(snapshot)
        }
        override fun onCancelled(error: DatabaseError) {
            Log.e("LecturesFragment", "Quizzes listener cancelled: ${error.message}")
        }
    }

    private val progressListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (!isAdded) return
            adapter.notifyDataSetChanged() // Refresh UI to reflect lock/unlock changes
        }
        override fun onCancelled(error: DatabaseError) {
            Log.e("LecturesFragment", "Progress listener cancelled: ${error.message}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.courses, container, false)

        topAppBar = view.findViewById(R.id.topAppBar)
        progressBar = view.findViewById(R.id.progress_bar)
        recyclerView = view.findViewById(R.id.recycler_view)
        headerTitle = view.findViewById(R.id.headerTitle)
        headerSubtitle = view.findViewById(R.id.headerSubtitle)

        topAppBar.title = "Welcome Student!"
        topAppBar.setTitleTextAppearance(requireContext(), R.style.ToolbarTitleText)

        displayList = mutableListOf()
        val prefs = requireContext().getSharedPreferences("USER_PREFS", 0)
        studentId = prefs.getString("studentId", "") ?: ""

        quizzesRef = FirebaseDatabase.getInstance().reference.child("quizzes")
        progressRef = FirebaseDatabase.getInstance().reference.child("users").child(studentId).child("progress")

        setupRecyclerView()

        // Attach listeners for real-time updates
        quizzesRef.addValueEventListener(quizzesListener)
        progressRef.addValueEventListener(progressListener)

        return view
    }

    private fun setupRecyclerView() {
        if (!isAdded) return

        adapter = QuizListAdapter(displayList, studentId, requireActivity())
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                val layoutManager = rv.layoutManager as LinearLayoutManager
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                if (firstVisible != RecyclerView.NO_POSITION && firstVisible < displayList.size) {
                    val currentItem = displayList[firstVisible]
                    if (currentItem is QuizDisplayItem.Part) {
                        headerTitle.text = currentItem.item.quizTitle
                        headerSubtitle.text = currentItem.item.quizSubtitle
                    }
                }
            }
        })
    }

    private fun buildDisplayList(snapshot: DataSnapshot) {
        progressBar.visibility = View.VISIBLE
        displayList.clear()

        val quizList = snapshot.children.mapNotNull { quizSnapshot ->
            val quizId = quizSnapshot.key ?: return@mapNotNull null
            val title = quizSnapshot.child("title").getValue(String::class.java) ?: ""
            val subtitle = quizSnapshot.child("subtitle").getValue(String::class.java) ?: ""
            val orderValue = quizSnapshot.child("order").value
            val order = when (orderValue) {
                is Long -> orderValue.toInt()
                is Int -> orderValue
                is String -> orderValue.toIntOrNull() ?: Int.MAX_VALUE
                else -> Int.MAX_VALUE
            }
            QuizMeta(quizSnapshot, order, quizId, title, subtitle)
        }.sortedBy { it.order }

        for (quiz in quizList) {
            val quizSnapshot = quiz.snapshot
            val quizId = quiz.quizId
            val title = quiz.title
            val subtitle = quiz.subtitle

            if (quizId == "quiz1" || quizId == "835247") continue

            val validParts = mutableListOf<QuizDisplayItem.Part>()
            val sortedParts = quizSnapshot.children
                .filter { it.key?.startsWith("part") == true || it.key == "post-test" }
                .sortedBy { part ->
                    if (part.key == "post-test") Int.MAX_VALUE
                    else part.key?.filter { it.isDigit() }?.toIntOrNull() ?: Int.MAX_VALUE
                }

            for (partSnapshot in sortedParts) {
                val partId = partSnapshot.key ?: continue
                val questions = partSnapshot.child("questionList").children.mapNotNull { qSnap ->
                    qSnap.getValue(com.example.thesis_app.models.QuestionModel::class.java)
                }

                validParts.add(
                    QuizDisplayItem.Part(
                        com.example.thesis_app.models.QuizPartItem(
                            quizId = quizId,
                            quizTitle = title,
                            quizSubtitle = subtitle,
                            partId = partId,
                            displayName = if (partId == "post-test") "Post-Test" else "Level ${partId.filter { it.isDigit() }}",
                            questions = questions,
                            isUnlocked = true // actual lock/unlock handled in adapter dynamically
                        )
                    )
                )
            }

            if (validParts.isNotEmpty()) {
                displayList.add(QuizDisplayItem.Divider(title))
                displayList.addAll(validParts)
            }
        }

        // ✅ Set default header to first quiz's title/subtitle
        val firstPart = displayList.firstOrNull { it is QuizDisplayItem.Part } as? QuizDisplayItem.Part
        firstPart?.let {
            headerTitle.text = it.item.quizTitle
            headerSubtitle.text = it.item.quizSubtitle
        }

        adapter.notifyDataSetChanged()
        progressBar.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        quizzesRef.removeEventListener(quizzesListener)
        progressRef.removeEventListener(progressListener)
    }
}
