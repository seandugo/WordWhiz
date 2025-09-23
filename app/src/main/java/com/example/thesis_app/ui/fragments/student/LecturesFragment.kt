package com.example.thesis_app.ui.fragments.student

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        if (!isAdded) return

        progressBar.visibility = View.GONE

        val prefs = requireContext().getSharedPreferences("USER_PREFS", 0)
        val studentId = prefs.getString("studentId", null)

        Log.d("LecturesFragment", "Setting up RecyclerView")
        Log.d("LecturesFragment", "studentId = $studentId")
        Log.d("LecturesFragment", "quizModelList size = ${quizModelList.size}")

        adapter = QuizListAdapter(quizModelList, studentId ?: "", requireActivity())
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
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