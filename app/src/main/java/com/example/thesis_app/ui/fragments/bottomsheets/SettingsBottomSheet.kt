package com.example.thesis_app.ui.fragments.bottomsheets

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity
import com.example.thesis_app.AboutUsActivity
import com.example.thesis_app.R
import com.example.thesis_app.TermsAndCActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SettingsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var aboutUsLayout: LinearLayout
    private lateinit var termsLayout: LinearLayout
    private lateinit var helpLayout: LinearLayout

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.settings, container, false)

        // Initialize layouts
        aboutUsLayout = view.findViewById(R.id.aboutUsLayout)
        termsLayout = view.findViewById(R.id.termsLayout)
        helpLayout = view.findViewById(R.id.helpLayout)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

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

        helpLayout.setOnClickListener {
            openHelpPage()
        }
    }

    private fun openHelpPage() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Default to student if no user found
            openUrl("https://seandugo.github.io/wordwhiz_app/manual.html")
            return
        }

        // Fetch user type from Firebase
        database.child(currentUser.uid).child("role").get()
            .addOnSuccessListener { snapshot ->
                val role = snapshot.value?.toString()?.lowercase() ?: "student"
                val section =
                    if (role == "teacher") "teacher-intro" else "student-intro"

                openUrl("https://seandugo.github.io/wordwhiz_app/manual.html?section=$section")
            }
            .addOnFailureListener {
                // Default to student manual if any error occurs
                openUrl("https://seandugo.github.io/wordwhiz_app/manual.html")
            }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
