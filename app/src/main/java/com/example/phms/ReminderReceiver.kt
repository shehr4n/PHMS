package com.example.phms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medName = intent.getStringExtra("MED_NAME") ?: "Medication Reminder"
        NotificationUtils.showNotification(context, medName)
    }
}