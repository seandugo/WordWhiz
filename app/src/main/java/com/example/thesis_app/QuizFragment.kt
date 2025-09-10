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
        progressBar.visibility = View.GONE
        adapter = QuizListAdapter(quizModelList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun getDataFromFirebase() {
        progressBar.visibility = View.VISIBLE

        FirebaseDatabase.getInstance().reference
            .child("quizzes") // ✅ better to point to "quizzes" node
            .get()
            .addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    for (snapshot in dataSnapshot.children) {
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