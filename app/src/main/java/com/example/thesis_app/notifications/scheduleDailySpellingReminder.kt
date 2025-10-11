package com.example.thesis_app.notifications

import android.content.Context
import androidx.work.*
import com.example.thesis_app.notifications.DailyReminderWorker
import java.util.concurrent.TimeUnit

fun scheduleDailySpellingReminder(context: Context) {
    // 🔹 For testing: trigger the notification after 10 seconds only
    val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(10, TimeUnit.SECONDS) // 👈 test delay here
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "daily_spelling_reminder",
        ExistingPeriodicWorkPolicy.UPDATE,
        dailyWorkRequest
    )
}
