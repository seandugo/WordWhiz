package com.example.thesis_app.ui.fragments.bottomsheets

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.example.thesis_app.AboutUsActivity
import com.example.thesis_app.R
import com.example.thesis_app.TermsAndCActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

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
            val intent = Intent(requireContext(), AboutUsActivity::class.java)
            startActivity(intent)
        }

        termsLayout.setOnClickListener {
            val intent = Intent(requireContext(), TermsAndCActivity::class.java)
            startActivity(intent)
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
