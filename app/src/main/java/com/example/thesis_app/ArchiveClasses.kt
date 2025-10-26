package com.example.thesis_app

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_app.models.ArchivedClass
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ArchiveClasses : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArchivedClassAdapter
    private val archivedList = mutableListOf<ArchivedClass>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.classes_archive)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Archive Classes"

        val typeface = ResourcesCompat.getFont(this, R.font.pixel)
        for (i in 0 until toolbar.childCount) {
            val view = toolbar.getChildAt(i)
            if (view is TextView) {
                view.typeface = typeface
                view.textSize = 18f
            }
        }

        toolbar.setNavigationOnClickListener { finish() }

        recyclerView = findViewById(R.id.classRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ArchivedClassAdapter(archivedList)
        recyclerView.adapter = adapter

        fetchArchivedClasses()
    }

    private fun fetchArchivedClasses() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("users")
            .child(userId)
            .child("archived_classes")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                archivedList.clear()

                for (classSnap in snapshot.children) {
                    val archivedAt = classSnap.child("archivedAt").getValue(Long::class.java) ?: 0L
                    val classDataSnap = classSnap.child("classData")

                    // ✅ Safely read nested classData
                    val className = classDataSnap.child("className").getValue(String::class.java) ?: "Unnamed Class"
                    val roomNumber = classDataSnap.child("roomNumber").getValue(String::class.java) ?: "N/A"

                    val archivedClass = ArchivedClass(
                        classId = classSnap.key ?: "",
                        className = className,
                        roomNumber = roomNumber,
                        archivedAt = archivedAt
                    )

                    archivedList.add(archivedClass)

                    // ✅ Auto-delete logic (7 days)
                    val deleteAfter = 7 * 24 * 60 * 60 * 1000L
                    val deleteTime = archivedAt + deleteAfter
                    if (archivedAt > 0 && System.currentTimeMillis() >= deleteTime) {
                        classSnap.ref.removeValue()
                    }
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ArchiveClasses, "Failed to load archives", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
