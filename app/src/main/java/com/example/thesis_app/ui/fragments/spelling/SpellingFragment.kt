package com.example.thesis_app.ui.fragments.spelling

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.example.thesis_app.R
import com.example.thesis_app.notifications.scheduleDailySpellingReminder
import com.example.thesis_app.ui.fragments.student.DailySpellingFragment
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class SpellingFragment : Fragment() {

    private val CHANNEL_ID = "daily_spelling_channel"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.daily_spelling_fragment, container, false)

        // Schedule the daily 8 AM reminder
        scheduleDailySpellingReminder(requireContext())

        // Check if we need to show notification today
        checkDailySpellingStatus()

        // Set up start button
        val startButton: Button = view.findViewById(R.id.startButton)
        startButton.setOnClickListener {
            markDailySpellingCompleted {
                navigateToSpellingQuiz()
            }
        }

        return view
    }

    // 🔹 Check Firebase if dailySpelling is false and not yet notified today
    private fun checkDailySpellingStatus() {
        val prefs = requireActivity().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null) ?: return

        val db = FirebaseDatabase.getInstance().reference
        db.child("users").child(studentId).child("dailySpelling")
            .get()
            .addOnSuccessListener { snapshot ->
                val hasStarted = snapshot.getValue(Boolean::class.java) ?: false
                if (!hasStarted && !hasShownNotificationToday(prefs)) {
                    showNotificationReminder()
                    markNotificationShownToday(prefs)
                }
            }
    }

    // 🔹 Mark "dailySpelling" as true in Firebase before navigating
    private fun markDailySpellingCompleted(onComplete: () -> Unit) {
        val prefs = requireActivity().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
        val studentId = prefs.getString("studentId", null) ?: return

        val db = FirebaseDatabase.getInstance().reference
        db.child("users").child(studentId).child("dailySpelling").setValue(true)
            .addOnSuccessListener { onComplete() }
            .addOnFailureListener { onComplete() } // fallback even if failed
    }

    // 🔹 Navigate to the spelling quiz fragment
    private fun navigateToSpellingQuiz() {
        val fragment = DailySpellingFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, fragment)
            .addToBackStack(null)
            .commit()
    }

    // 🔹 Check if already shown today
    private fun hasShownNotificationToday(prefs: android.content.SharedPreferences): Boolean {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastShown = prefs.getString("lastNotificationDate", "")
        return today == lastShown
    }

    // 🔹 Mark notification as shown today
    private fun markNotificationShownToday(prefs: android.content.SharedPreferences) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        prefs.edit().putString("lastNotificationDate", today).apply()
    }

    // 🔹 Show daily notification
    private fun showNotificationReminder() {
        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Spelling Reminder"
            val descriptionText = "Reminds students to start daily spelling challenge"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.wordwhiz_logo)
            .setContentTitle("Daily Spelling Challenge")
            .setContentText("You haven’t started today’s spelling challenge yet!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
