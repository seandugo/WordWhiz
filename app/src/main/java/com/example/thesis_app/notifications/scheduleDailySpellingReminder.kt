package com.example.thesis_app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

fun scheduleDailySpellingReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, DailySpellingReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 8)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }

    if (calendar.timeInMillis <= System.currentTimeMillis()) {
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }

    alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pendingIntent
    )
}
