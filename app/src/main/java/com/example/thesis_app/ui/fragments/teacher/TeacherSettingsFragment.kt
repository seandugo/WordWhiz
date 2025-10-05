package com.example.thesis_app.ui.fragments.teacher

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.thesis_app.AboutUsActivity
import com.example.thesis_app.R

class TeacherSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.settings, container, false)

        // Find the About Us button (LinearLayout) and set click listener
        val aboutUsLayout = view.findViewById<LinearLayout>(R.id.aboutUsLayout)
        aboutUsLayout.setOnClickListener {
            // Launch AboutUsActivity
            val intent = Intent(requireContext(), AboutUsActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}