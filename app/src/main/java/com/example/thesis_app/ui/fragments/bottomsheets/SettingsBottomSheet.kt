package com.example.thesis_app.ui.fragments.bottomsheets

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.thesis_app.LoginActivity
import com.example.thesis_app.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth

class SettingsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var aboutUsLayout: LinearLayout
    private lateinit var termsLayout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.settings, container, false)

        // Initialize layouts
        aboutUsLayout = view.findViewById(R.id.aboutUsLayout)
        termsLayout = view.findViewById(R.id.termsLayout)

        setupListeners()

        return view
    }

    private fun setupListeners() {
        aboutUsLayout.setOnClickListener {
            Toast.makeText(requireContext(), "About Us clicked", Toast.LENGTH_SHORT).show()
            openUrl("https://www.example.com/about")
        }

        termsLayout.setOnClickListener {
            Toast.makeText(requireContext(), "Terms and Conditions clicked", Toast.LENGTH_SHORT).show()
            openUrl("https://www.example.com/terms")
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
