package com.example.thesis_app.ui.fragments.pretest

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.thesis_app.PreAssessmentActivity
import com.example.thesis_app.R
import com.example.thesis_app.models.StudentItem
import com.google.firebase.database.FirebaseDatabase

class ClassCodePage : Fragment(R.layout.class_code) {
    private lateinit var nextButton: Button
    private lateinit var classCodeInput: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nextButton = view.findViewById(R.id.button)
        classCodeInput = view.findViewById(R.id.editTextClassCode)

        // Get studentId from shared preferences
        val sharedPrefs = requireContext().getSharedPreferences("USER_PREFS", 0)
        val studentId = sharedPrefs.getString("studentId", "") ?: ""
        if (studentId.isEmpty()) {
            Toast.makeText(requireContext(), "Student ID missing!", Toast.LENGTH_SHORT).show()
            disableInputs()
            return
        }

        // Check for existing class code in Firebase and populate input
        val db = FirebaseDatabase.getInstance().reference
        db.child("users").child(studentId).child("classes").get().addOnSuccessListener { snapshot ->
            if (snapshot.exists() && snapshot.hasChildren()) {
                // Get the first class code (assuming single class enrollment)
                val classCode = snapshot.children.first().key
                if (classCode != null) {
                    classCodeInput.setText(classCode)
                    classCodeInput.isEnabled = false // Keep input disabled to prevent editing
                    classCodeInput.alpha = 0.5f // Visually indicate disabled state
                    nextButton.isEnabled = true // Ensure button is enabled
                    nextButton.alpha = 1.0f
                    Toast.makeText(requireContext(), "Class code loaded", Toast.LENGTH_SHORT).show()
                }
            } else {
                // No class code, enable input for new entry
                enableInputs()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error checking classes: ${e.message}", Toast.LENGTH_SHORT).show()
            disableInputs() // Disable inputs on error
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    (activity as? PreAssessmentActivity)?.showExitConfirmation()
                }
            })

        nextButton.setOnClickListener {
            val classCode = classCodeInput.text.toString().trim()
            if (classCode.isEmpty()) {
                Toast.makeText(requireContext(), "Enter a class code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if student is already enrolled in this class
            db.child("users").child(studentId).child("classes").child(classCode).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Student is already enrolled, navigate to next page
                    navigateToQuizTimePage()
                } else {
                    // New class code, proceed with joining class
                    db.child("classes").child(classCode).get().addOnSuccessListener { classSnapshot ->
                        if (!classSnapshot.exists()) {
                            Toast.makeText(requireContext(), "Class code not found", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        // Get student info
                        db.child("users").child(studentId).get().addOnSuccessListener { studentSnap ->
                            if (!studentSnap.exists()) {
                                Toast.makeText(requireContext(), "Student not found!", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            val name = studentSnap.child("name").getValue(String::class.java) ?: "Unknown"
                            val email = studentSnap.child("email").getValue(String::class.java)

                            val newStudent = StudentItem(
                                name = name,
                                email = email,
                                order = classSnapshot.child("students").childrenCount.toInt(),
                                studentId = studentId,
                                achievements = listOf()
                            )

                            // Add student under class
                            db.child("classes")
                                .child(classCode)
                                .child("students")
                                .child(studentId)
                                .setValue(newStudent)
                                .addOnSuccessListener {
                                    // Add class reference under user
                                    db.child("users")
                                        .child(studentId)
                                        .child("classes")
                                        .child(classCode)
                                        .setValue(true)

                                    // Initialize student progress
                                    initializeStudentProgress(studentId, classCode)

                                    Toast.makeText(requireContext(), "Joined class successfully", Toast.LENGTH_SHORT).show()

                                    // Navigate to QuizTimePage
                                    navigateToQuizTimePage()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeStudentProgress(studentId: String, classCode: String) {
        val db = FirebaseDatabase.getInstance()
        val studentProgressRef = db.getReference("users/$studentId/progress")
        val classStudentProgressRef = db.getReference("classes/$classCode/students/$studentId/progress")

        studentProgressRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                // No existing progress, initialize empty
                classStudentProgressRef.setValue(emptyMap<String, Any>())
                return@addOnSuccessListener
            }

            // Copy progress to class students
            classStudentProgressRef.setValue(snapshot.value)
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to copy progress: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Failed to fetch student progress: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToQuizTimePage() {
        val secondPageFragment = QuizTimePage()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView2, secondPageFragment)
            .addToBackStack(null)
            .commit()
        (activity as? PreAssessmentActivity)?.nextStep()
    }

    private fun disableInputs() {
        classCodeInput.isEnabled = false
        nextButton.isEnabled = false
        classCodeInput.alpha = 0.5f
        nextButton.alpha = 0.5f
    }

    private fun enableInputs() {
        classCodeInput.isEnabled = true
        nextButton.isEnabled = true
        classCodeInput.alpha = 1.0f
        nextButton.alpha = 1.0f
    }
}