package com.example.thesis_app.ui.fragments.bottomsheets

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.example.thesis_app.LoginActivity
import com.example.thesis_app.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth

class ProfileBottomSheet : BottomSheetDialogFragment() {

    private lateinit var logoutLayout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.profile_settings, container, false)

        logoutLayout = view.findViewById(R.id.logoutLayout)

        setupListeners()

        return view
    }

    private fun setupListeners() {
        logoutLayout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                val prefs = requireContext().getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                prefs.edit().clear().apply()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
                dismiss() // close bottom sheet after logout
            }
            .setNegativeButton("No", null)
            .show()
    }
}
