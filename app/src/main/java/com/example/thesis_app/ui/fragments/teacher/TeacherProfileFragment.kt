package com.example.thesis_app.ui.fragments.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.thesis_app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class TeacherProfileFragment: Fragment() {

    private lateinit var textName: TextView

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize TextView
        textName = view.findViewById(R.id.nameText)

        // Load profile data
        loadProfileData()
    }

    private fun loadProfileData() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        database.child("users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val name = snapshot.child("name").value.toString()
                    val email = snapshot.child("email").value.toString()
                    textName.text = name
                } else {
                    textName.text = "No Name"
                }
            }
            .addOnFailureListener {
                textName.text = "No Name"
            }
    }
}
