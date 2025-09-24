package com.example.thesis_app.ui.fragments.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.thesis_app.R
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar

class TeacherProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.profile, container, false)

        val collapsingToolbar = view.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)

        // Set collapsing title
        collapsingToolbar.title = "Juan Dela Cruz"

        // If you want a navigation icon action
        toolbar.setNavigationOnClickListener {
            // Example: open drawer or navigate back
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        return view
    }
}
