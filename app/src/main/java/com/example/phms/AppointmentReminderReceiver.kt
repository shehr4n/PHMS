package com.example.phms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.phms.NotificationUtils

class AppointmentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appointmentId = intent.getStringExtra("APPOINTMENT_ID") ?: return
        val title = intent.getStringExtra("APPOINTMENT_TITLE") ?: return
        val doctorName = intent.getStringExtra("DOCTOR_NAME") ?: return
        val location = intent.getStringExtra("LOCATION") ?: return

        NotificationUtils.showAppointmentReminder(
            context = context,
            appointmentId = appointmentId,
            title = title,
            doctorName = doctorName,
            location = location
        )
    }
} 