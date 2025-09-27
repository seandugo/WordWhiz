package com.example.thesis_app.ui.fragments.spelling

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.thesis_app.R
import com.example.thesis_app.ui.fragments.student.DailySpellingFragment
import com.google.firebase.database.FirebaseDatabase

class SpellingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.daily_spelling_fragment, container, false)

        val btnNext: Button = view.findViewById(R.id.startButton)
        btnNext.setOnClickListener {
            markDailySpellingCompleted {
                // Navigate to DailySpellingFragment after marking complete
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerView, DailySpellingFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        return view
    }

    private fun markDailySpellingCompleted(onComplete: () -> Unit) {
        val prefs = requireActivity().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null) ?: return

        val db = FirebaseDatabase.getInstance().reference
        db.child("users").child(studentId).child("dailySpelling").setValue(true)
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener {
                // fallback: still navigate even if DB fails
                onComplete()
            }
    }
}
