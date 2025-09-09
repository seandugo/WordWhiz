package com.example.thesis_app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileImage: CircleImageView
    private lateinit var textName: TextView
    private lateinit var textEmail: TextView
    private lateinit var btnUploadImage: Button
    private lateinit var btnEditProfile: Button

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    private var imageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                imageUri = result.data?.data
                imageUri?.let { uri ->
                    Glide.with(this).load(uri).into(profileImage)
                    uploadImageToFirebase(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        profileImage = findViewById(R.id.profile_image)
        textName = findViewById(R.id.text_name)
        textEmail = findViewById(R.id.text_email)
        btnUploadImage = findViewById(R.id.btn_upload_image)
        btnEditProfile = findViewById(R.id.btn_edit_profile)

        loadProfileData()

        btnUploadImage.setOnClickListener { openImagePicker() }
        btnEditProfile.setOnClickListener {
            Toast.makeText(this, "Edit profile clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfileData() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        // Fetch name & email from Firebase Realtime Database
        database.child("users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val name = snapshot.child("name").value.toString()
                    val email = snapshot.child("email").value.toString()
                    textName.text = name
                    textEmail.text = email
                } else {
                    textName.text = "No Name"
                    textEmail.text = user.email ?: "No Email"
                }
            }
            .addOnFailureListener {
                textName.text = "No Name"
                textEmail.text = user.email ?: "No Email"
            }

        // Load profile image
        val profileRef = storage.child("teacher_profile_images/${user.uid}.jpg")
        profileRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this).load(uri).into(profileImage)
        }.addOnFailureListener {
            profileImage.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun uploadImageToFirebase(uri: Uri) {
        val user = auth.currentUser ?: return
        val profileRef = storage.child("teacher_profile_images/${user.uid}.jpg")

        profileRef.putFile(uri).addOnSuccessListener {
            Toast.makeText(this, "Profile image uploaded successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}