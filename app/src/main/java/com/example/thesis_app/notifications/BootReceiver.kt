package com.example.thesis_app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.thesis_app.notifications.CooldownWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {

            // ✅ Restore your daily reminder if needed
            scheduleDailySpellingReminder(context)

            // ✅ Re-schedule cooldown notification if still active
            val prefs = context.getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
            val cooldownEnd = prefs.getLong("cooldownEndTime", 0L)
            val now = System.currentTimeMillis()
            if (cooldownEnd > now) {
                val delayMillis = cooldownEnd - now
                val workRequest = OneTimeWorkRequestBuilder<CooldownWorker>()
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}
