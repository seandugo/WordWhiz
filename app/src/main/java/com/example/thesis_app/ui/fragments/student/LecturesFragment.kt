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
import com.example.thesis_app.models.QuizModel
import com.example.thesis_app.QuizListAdapter
import com.example.thesis_app.R
import com.google.firebase.database.FirebaseDatabase

class LecturesFragment : Fragment() {

    private lateinit var quizModelList: MutableList<QuizModel>
    private lateinit var adapter: QuizListAdapter
    private lateinit var topAppBar: com.google.android.material.appbar.MaterialToolbar
    private lateinit var progressBar: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var headerLayout: View
    private lateinit var headerTitle: TextView
    private lateinit var headerSubtitle: TextView

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

        quizModelList = mutableListOf()
        getDataFromFirebase()

        return view
    }

    private fun setupRecyclerView() {
        if (!isAdded) return

        progressBar.visibility = View.GONE

        val prefs = requireContext().getSharedPreferences("USER_PREFS", 0)
        val studentId = prefs.getString("studentId", null)

        adapter = QuizListAdapter(quizModelList, studentId ?: "", requireActivity())
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
                if (firstVisiblePos != RecyclerView.NO_POSITION && firstVisiblePos < quizModelList.size) {
                    val currentQuiz = quizModelList[firstVisiblePos]

                    // Update header
                    headerTitle.text = currentQuiz.title
                    headerSubtitle.text = currentQuiz.subtitle
                }
            }
        })

    }

    private fun getDataFromFirebase() {
        progressBar.visibility = View.VISIBLE

        val prefs = requireContext().getSharedPreferences("USER_PREFS", 0)
        val studentId = prefs.getString("studentId", "")
        Log.d("LecturesFragment", "studentId = $studentId")

        FirebaseDatabase.getInstance().reference
            .child("quizzes")
            .get()
            .addOnSuccessListener { database ->
                if (database.exists()) {
                    for (quizSnapshot in database.children) {
                        val quizModel = quizSnapshot.getValue(QuizModel::class.java)
                        if (quizModel != null) {
                            val fixedModel = quizModel.copy(id = quizSnapshot.key ?: "")
                            quizModelList.add(fixedModel)
                        }
                    }
                    Log.d("LecturesFragment", "Quizzes found: ${quizModelList.size}")
                } else {
                    Log.d("LecturesFragment", "No quizzes found at this grade level")
                }
                setupRecyclerView()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Log.e("LecturesFragment", "Failed to fetch quizzes: ${it.message}")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        quizModelList.clear()
    }
}