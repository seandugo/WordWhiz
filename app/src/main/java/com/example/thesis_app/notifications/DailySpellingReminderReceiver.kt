package com.example.thesis_app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.os.Build
import com.example.thesis_app.R

class DailySpellingReminderReceiver : BroadcastReceiver() {

    private val CHANNEL_ID = "daily_spelling_channel"

    override fun onReceive(context: Context, intent: Intent?) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Spelling Reminder"
            val descriptionText = "Reminds students every 8 AM"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.wordwhiz_logo)
            .setContentTitle("Daily Spelling Challenge")
            .setContentText("It's 8 AM! Time for your daily spelling challenge! ✏️")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}