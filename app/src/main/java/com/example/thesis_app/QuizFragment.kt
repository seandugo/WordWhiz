package com.example.thesis_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.example.thesis_app.models.QuizModel

class QuizFragment : Fragment() {

    private lateinit var quizModelList: MutableList<QuizModel>
    private lateinit var adapter: QuizListAdapter

    // Views
    private lateinit var progressBar: View
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.courses, container, false)

        progressBar = view.findViewById(R.id.progress_bar)
        recyclerView = view.findViewById(R.id.recycler_view)

        quizModelList = mutableListOf()
        getDataFromFirebase()

        return view
    }

    private fun setupRecyclerView() {
        progressBar.visibility = View.GONE

        val sharedPrefs = requireContext().getSharedPreferences("USER_PREFS", 0)
        val studentId = sharedPrefs.getString("studentId", "") ?: ""

        adapter = QuizListAdapter(quizModelList, studentId, requireActivity())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun getDataFromFirebase() {
        progressBar.visibility = View.VISIBLE

        val sharedPrefs = requireContext().getSharedPreferences("USER_PREFS", 0)
        val studentId = sharedPrefs.getString("studentId", "") ?: ""

        if (studentId.isEmpty()) return

        // 1. Get user's grade level
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(studentId)
            .child("grade_level")
            .get()
            .addOnSuccessListener { gradeSnapshot ->
                if (gradeSnapshot.exists()) {
                    val gradeLevel = gradeSnapshot.getValue(Int::class.java) ?: 0

                    // 2. Fetch quizzes only from that grade level node
                    FirebaseDatabase.getInstance().reference
                        .child("quizzes")
                        .child(gradeLevel.toString())
                        .get()
                        .addOnSuccessListener { quizSnapshot ->
                            quizModelList.clear()
                            if (quizSnapshot.exists()) {
                                for (snapshot in quizSnapshot.children) {
                                    val quizModel = snapshot.getValue(QuizModel::class.java)
                                    if (quizModel != null) {
                                        quizModelList.add(quizModel)
                                    }
                                }
                            }
                            setupRecyclerView()
                        }
                }
            }
    }
}