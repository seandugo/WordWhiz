package com.example.thesis_app.ui.fragments.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.example.thesis_app.models.QuizModel
import com.example.thesis_app.QuizListAdapter
import com.example.thesis_app.R

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
        // Inflate the fragment layout (create fragment_main.xml)
        val view = inflater.inflate(R.layout.courses, container, false)

        // Find views
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
            val studentId = prefs.getString("studentId", "") ?: ""

            adapter = QuizListAdapter(quizModelList, studentId, requireActivity())      // ✅ pass it
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
        }


    private fun getDataFromFirebase() {
        progressBar.visibility = View.VISIBLE

        FirebaseDatabase.getInstance().reference
            .child("quizzes")
            .get()
            .addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    for (snapshot in dataSnapshot.children) {
                        val quizModel = snapshot.getValue(QuizModel::class.java)
                        if (quizModel != null) {
                            val fixedModel = quizModel.copy(id = snapshot.key ?: "")
                            quizModelList.add(fixedModel)
                        }
                    }
                }
                setupRecyclerView()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        quizModelList.clear()
    }
}