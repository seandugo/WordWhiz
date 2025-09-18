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

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    (activity as? PreAssessmentActivity)?.previousStep()
                    parentFragmentManager.popBackStack()
                }
            })

        nextButton.setOnClickListener {
            val classCode = classCodeInput.text.toString().trim()

            if (classCode.isEmpty()) {
                Toast.makeText(requireContext(), "Enter a class code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Get studentId from SharedPreferences
            val sharedPrefs = requireContext().getSharedPreferences("USER_PREFS", 0)
            val studentId = sharedPrefs.getString("studentId", "") ?: ""

            if (studentId.isEmpty()) {
                Toast.makeText(requireContext(), "Student ID missing!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = FirebaseDatabase.getInstance().reference

            // 🔹 Check if class exists
            db.child("classes").child(classCode).get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    Toast.makeText(requireContext(), "Class code not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // 🔹 Get student info
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
                        order = snapshot.child("students").childrenCount.toInt(),
                        studentId = studentId,
                        achievements = listOf()
                    )

                    // 🔹 Save under classes → students
                    db.child("classes")
                        .child(classCode)
                        .child("students")
                        .child(studentId)
                        .setValue(newStudent)
                        .addOnSuccessListener {
                            // 🔹 Also add pointer under user → classes
                            db.child("users")
                                .child(studentId)
                                .child("classes")
                                .child(classCode)
                                .setValue(true)

                            Toast.makeText(requireContext(), "Joined class successfully", Toast.LENGTH_SHORT).show()

                            // ✅ Navigate to QuizTimePage
                            val secondPageFragment = QuizTimePage()
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragmentContainerView2, secondPageFragment)
                                .addToBackStack(null)
                                .commit()
                            (activity as? PreAssessmentActivity)?.nextStep()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
