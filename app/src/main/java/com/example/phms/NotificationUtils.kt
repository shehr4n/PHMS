package com.example.phms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationUtils {

    const val REMINDER_CHANNEL_ID = "medication_reminder_channel"
    const val TIMEOUT_CHANNEL_ID = "medication_timeout_channel"
    const val CONFLICT_CHANNEL_ID = "medication_conflict_channel"

    fun createNotificationChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Reminder Channel
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to take your medications"
                enableVibration(true)
                enableLights(true)
            }
            
            // Timeout Channel
            val timeoutChannel = NotificationChannel(
                TIMEOUT_CHANNEL_ID,
                "Missed Medication Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when medications are not taken on time"
                enableVibration(true)
                enableLights(true)
            }
            
            // Conflict Channel
            val conflictChannel = NotificationChannel(
                CONFLICT_CHANNEL_ID,
                "Medication Conflicts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts about potential medication conflicts"
                enableVibration(true)
                enableLights(true)
            }
            
            manager.createNotificationChannels(listOf(reminderChannel, timeoutChannel, conflictChannel))
        }
    }

    fun showMedicationReminder(
        context: Context, 
        medicationId: String,
        medicationName: String,
        reminderTime: String
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels(context)
        
        // Create confirmation intent
        val confirmIntent = Intent(context, MedicationConfirmationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("MEDICATION_ID", medicationId)
            putExtra("MEDICATION_NAME", medicationName)
            putExtra("REMINDER_TIME", reminderTime)
        }

        val confirmPendingIntent = PendingIntent.getActivity(
            context,
            "${medicationId}_${reminderTime}_confirm".hashCode(),
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create direct take action
        val takeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TAKE_MEDICATION
            putExtra("MEDICATION_ID", medicationId)
            putExtra("MEDICATION_NAME", medicationName)
            putExtra("REMINDER_TIME", reminderTime)
        }

        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            "${medicationId}_${reminderTime}_take".hashCode(),
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Medication Reminder")
            .setContentText("Time to take $medicationName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(confirmPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_send,
                "Open",
                confirmPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_send,
                "I took it",
                takePendingIntent
            )
            .build()

        val notificationId = "${medicationId}_${reminderTime}".hashCode()
        manager.notify(notificationId, notification)
    }
    
    fun showMissedMedicationAlert(
        context: Context,
        medicationId: String,
        medicationName: String,
        reminderTime: String,
        emergencyContactName: String? = null
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels(context)
        
        // Create intent to open the app
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        val openPendingIntent = PendingIntent.getActivity(
            context,
            "open_${medicationId}".hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, TIMEOUT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Missed Medication Alert")
            .setContentText("$medicationName was not taken at scheduled time: $reminderTime")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$medicationName was not taken at scheduled time: $reminderTime. " +
                    if (emergencyContactName != null) "Emergency contact $emergencyContactName will be notified." else ""))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = "${medicationId}_${reminderTime}_timeout".hashCode()
        manager.notify(notificationId, notification)
    }
    
    fun showMedicationConflict(
        context: Context,
        medicationName: String,
        conflictNames: String
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels(context)
        
        // Create intent to open the app
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        val openPendingIntent = PendingIntent.getActivity(
            context,
            "conflict_open".hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CONFLICT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Medication Conflict Detected")
            .setContentText("Potential conflict between $medicationName and $conflictNames")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Potential medication conflict detected between $medicationName and $conflictNames. Please consult your healthcare provider."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = "conflict_${medicationName}".hashCode()
        manager.notify(notificationId, notification)
    }
    
    fun cancelNotification(context: Context, notificationId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }
}
