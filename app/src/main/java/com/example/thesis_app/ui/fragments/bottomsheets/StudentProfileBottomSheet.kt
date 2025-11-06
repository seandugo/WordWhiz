package com.example.thesis_app.ui.fragments.bottomsheets

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.thesis_app.LoginActivity
import com.example.thesis_app.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class StudentProfileBottomSheet : BottomSheetDialogFragment() {

    private lateinit var logoutLayout: LinearLayout
    private lateinit var changeClassLayout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.student_profile_settings, container, false)

        changeClassLayout = view.findViewById(R.id.changeClassLayout)
        logoutLayout = view.findViewById(R.id.logoutLayout)

        setupListeners()
        return view
    }

    private fun setupListeners() {
        logoutLayout.setOnClickListener { showLogoutConfirmation() }
        changeClassLayout.setOnClickListener { checkExistingRequestThenShowDialog() }
    }

    // ✅ Check if there's an existing pending class change request first
    private fun checkExistingRequestThenShowDialog() {
        val prefs = requireContext().getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null)

        if (studentId == null) {
            Toast.makeText(requireContext(), "Error: No student ID found.", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance()
        val requestRef = database.getReference("classChangeRequests/$studentId")

        requestRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val status = snapshot.child("status").value?.toString() ?: "pending"
                if (status == "pending") {
                    // ⚠️ Request still pending
                    AlertDialog.Builder(requireContext())
                        .setTitle("Request Pending")
                        .setMessage("You already have a pending class change request. Please wait for your teacher to review it before sending another one.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    // ✅ Allow new request if previous one is resolved
                    showChangeClassDialog()
                }
            } else {
                // ✅ No request exists — allow new request
                showChangeClassDialog()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error checking request status. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ Show the Change Class dialog
    private fun showChangeClassDialog() {
        val dialogView = layoutInflater.inflate(R.layout.change_class_dialog, null)
        val editClassCode = dialogView.findViewById<TextInputEditText>(R.id.editClassCode)

        AlertDialog.Builder(requireContext())
            .setTitle("Change Class")
            .setView(dialogView)
            .setPositiveButton("Submit") { dialog, _ ->
                val newClassCode = editClassCode.text?.toString()?.trim()
                if (newClassCode.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a class code.", Toast.LENGTH_SHORT).show()
                } else {
                    submitClassChangeRequest(newClassCode)
                    dialog.dismiss()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ✅ Submit class change request and notify the target teacher
    private fun submitClassChangeRequest(classCode: String) {
        val prefs = requireContext().getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null)
        val studentName = prefs.getString("studentName", "Unknown Student")

        if (studentId == null) {
            Toast.makeText(requireContext(), "Error: No student ID found.", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance()
        val requestData = mapOf(
            "studentId" to studentId,
            "studentName" to studentName,
            "requestedClassCode" to classCode,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        val updates = hashMapOf<String, Any>(
            "/classChangeRequests/$studentId" to requestData,
            "/classes/$classCode/pendingRequests/$studentId" to requestData
        )

        database.reference.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Class change request sent! Your teacher will review it.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to send request. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ -> markUserOfflineThenLogout() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun markUserOfflineThenLogout() {
        val prefs = requireContext().getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null)

        if (studentId != null) {
            val presenceRef = FirebaseDatabase.getInstance()
                .getReference("users/$studentId/presence")

            presenceRef.child("status").setValue("offline")
            presenceRef.child("lastSeen").setValue(System.currentTimeMillis())
                .addOnCompleteListener { performLogout(prefs) }
        } else {
            performLogout(prefs)
        }
    }

    private fun performLogout(prefs: android.content.SharedPreferences) {
        FirebaseAuth.getInstance().signOut()
        prefs.edit().clear().apply()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
        dismiss()
    }
}
