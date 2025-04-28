package com.example.phms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MedicationMonitor(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val REMINDER_RESPONSE_TIMEOUT = 30 * 60 * 1000L // 30 minutes in milliseconds
        const val MEDICATION_REMINDER_ACTION = "com.example.phms.MEDICATION_REMINDER"
        const val MEDICATION_TIMEOUT_ACTION = "com.example.phms.MEDICATION_TIMEOUT"
        const val MEDICATION_CONFLICT_ACTION = "com.example.phms.MEDICATION_CONFLICT"
    }

    /**
     * Schedule an immediate reminder for testing
     */
    fun scheduleDebugReminder(medication: Medication) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.SECOND, 10) // Schedule for 10 seconds from now
        }
        
        val hourAndMinute = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
        
        // Use NotificationUtils directly for immediate test
        NotificationUtils.showMedicationReminder(
            context,
            medication.id,
            medication.name,
            hourAndMinute
        )
        
        Toast.makeText(
            context,
            "Debug reminder scheduled for ${hourAndMinute}",
            Toast.LENGTH_SHORT
        ).show()
        
        // Also set up an actual alarm
        scheduleOneTimeReminder(medication, calendar)
    }
    
    private fun scheduleOneTimeReminder(medication: Medication, calendar: Calendar) {
        val hourAndMinute = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
        
        Log.d("MedicationMonitor", "Scheduling one-time reminder for ${medication.name} at ${hourAndMinute}")
        
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = MEDICATION_REMINDER_ACTION
            putExtra("MEDICATION_ID", medication.id)
            putExtra("MEDICATION_NAME", medication.name)
            putExtra("REMINDER_TIME", hourAndMinute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "debug_${medication.id}_${System.currentTimeMillis()}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "Please enable exact alarms permission", Toast.LENGTH_LONG).show()
            NotificationUtils.showMedicationReminder(
                context,
                medication.id,
                medication.name,
                hourAndMinute
            )
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d("MedicationMonitor", "One-time alarm set successfully")
        } catch (e: Exception) {
            Log.e("MedicationMonitor", "Failed to set alarm", e)
            // Fallback to direct notification
            NotificationUtils.showMedicationReminder(
                context,
                medication.id,
                medication.name,
                hourAndMinute
            )
        }
    }

    fun scheduleReminders(medication: Medication) {
        val userId = auth.currentUser?.uid ?: return
        
        Log.d("MedicationMonitor", "Scheduling reminders for ${medication.name}")
        
        for (time in medication.times) {
            try {
                val (hour, minute) = time.split(":").map { it.toInt() }
                
                // Schedule daily reminder
                scheduleReminderAlarm(medication, hour, minute)
                
                // Schedule timeout check
                scheduleTimeoutCheck(medication, hour, minute)
                
                // For testing - If the medication is due in the next hour, also set a one-time alarm
                val now = Calendar.getInstance()
                val medicationTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                
                if (medicationTime.after(now) && 
                    medicationTime.timeInMillis - now.timeInMillis < 60 * 60 * 1000) {
                    Log.d("MedicationMonitor", "Medication due soon, setting immediate alarm")
                    scheduleOneTimeReminder(medication, medicationTime)
                }
            } catch (e: Exception) {
                Log.e("MedicationMonitor", "Error parsing time: $time", e)
            }
        }
    }

    private fun scheduleReminderAlarm(medication: Medication, hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }

        val formattedTime = String.format("%02d:%02d", hour, minute)
        Log.d("MedicationMonitor", "Scheduling reminder for ${medication.name} at $formattedTime, millis: ${calendar.timeInMillis}")

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = MEDICATION_REMINDER_ACTION
            putExtra("MEDICATION_ID", medication.id)
            putExtra("MEDICATION_NAME", medication.name)
            putExtra("REMINDER_TIME", formattedTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "${medication.id}_${formattedTime}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "Please enable exact alarms permission in settings", Toast.LENGTH_LONG).show()
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                
                // Also set a repeating alarm as backup
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
            Log.d("MedicationMonitor", "Alarm set successfully for ${medication.name}")
        } catch (e: Exception) {
            Log.e("MedicationMonitor", "Failed to set alarm", e)
        }
    }

    private fun scheduleTimeoutCheck(medication: Medication, hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            add(Calendar.MILLISECOND, REMINDER_RESPONSE_TIMEOUT.toInt())
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }

        val formattedTime = String.format("%02d:%02d", hour, minute)
        
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = MEDICATION_TIMEOUT_ACTION
            putExtra("MEDICATION_ID", medication.id)
            putExtra("MEDICATION_NAME", medication.name)
            putExtra("REMINDER_TIME", formattedTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "${medication.id}_${formattedTime}_timeout".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e("MedicationMonitor", "Failed to set timeout alarm", e)
        }
    }

    fun confirmMedicationIntake(medicationId: String, time: String, medicationName: String? = null) {
        val userId = auth.currentUser?.uid ?: return
        
        Log.d("MedicationMonitor", "Confirming medication intake for ID: $medicationId at time: $time")
        
        // First check if we already logged this medication as taken today
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val tomorrow = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }.time
        
        db.collection("users")
            .document(userId)
            .collection("medicationLogs")
            .whereEqualTo("medicationId", medicationId)
            .whereEqualTo("time", time)
            .whereEqualTo("status", "taken")
            .whereGreaterThanOrEqualTo("timestamp", today)
            .whereLessThan("timestamp", tomorrow)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Not yet logged as taken today, so log it
                    recordMedicationIntake(userId, medicationId, time, medicationName)
                } else {
                    Log.d("MedicationMonitor", "Medication already logged as taken today, not creating duplicate entry")
                }
                
                // Always cancel the alarms regardless
                cancelAllAlarmsForMedication(medicationId, time)
            }
            .addOnFailureListener { e ->
                // On failure, just log it (may cause duplicates but better than not logging)
                Log.e("MedicationMonitor", "Failed to check for existing intake logs", e)
                recordMedicationIntake(userId, medicationId, time, medicationName)
                cancelAllAlarmsForMedication(medicationId, time)
            }
    }
    
    private fun recordMedicationIntake(userId: String, medicationId: String, time: String, medicationName: String? = null) {
        // If medication name isn't provided, try to fetch it
        if (medicationName == null) {
            db.collection("users")
                .document(userId)
                .collection("medications")
                .document(medicationId)
                .get()
                .addOnSuccessListener { document ->
                    val med = document.toObject(Medication::class.java)
                    val name = med?.name ?: "Unknown medication"
                    saveIntakeRecord(userId, medicationId, time, name)
                }
                .addOnFailureListener { e ->
                    Log.e("MedicationMonitor", "Failed to get medication name", e)
                    saveIntakeRecord(userId, medicationId, time, "Unknown medication")
                }
        } else {
            saveIntakeRecord(userId, medicationId, time, medicationName)
        }
    }
    
    private fun saveIntakeRecord(userId: String, medicationId: String, time: String, medicationName: String) {
        // Record the confirmation in Firestore
        val confirmation = hashMapOf(
            "medicationId" to medicationId,
            "medicationName" to medicationName,
            "time" to time,
            "timestamp" to Date(),
            "status" to "taken"
        )

        db.collection("users")
            .document(userId)
            .collection("medicationLogs")
            .add(confirmation)
            .addOnSuccessListener {
                Log.d("MedicationMonitor", "Medication intake logged successfully in Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("MedicationMonitor", "Failed to log medication intake", e)
            }
    }

    private fun cancelAllAlarmsForMedication(medicationId: String, time: String) {
        // Cancel reminder alarm
        cancelReminderAlarm(medicationId, time)
        
        // Cancel timeout alarm
        cancelTimeoutAlarm(medicationId, time)
        
        Log.d("MedicationMonitor", "Canceled all alarms for medication $medicationId at time $time")
    }
    
    private fun cancelReminderAlarm(medicationId: String, time: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = MEDICATION_REMINDER_ACTION
            putExtra("MEDICATION_ID", medicationId)
            putExtra("REMINDER_TIME", time)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "${medicationId}_${time}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel() // Additional cancellation for thoroughness
            Log.d("MedicationMonitor", "Reminder alarm canceled for $medicationId at $time")
        } catch (e: Exception) {
            Log.e("MedicationMonitor", "Failed to cancel reminder alarm", e)
        }
    }

    private fun cancelTimeoutAlarm(medicationId: String, time: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = MEDICATION_TIMEOUT_ACTION
            putExtra("MEDICATION_ID", medicationId)
            putExtra("REMINDER_TIME", time)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "${medicationId}_${time}_timeout".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel() // Additional cancellation for thoroughness
            Log.d("MedicationMonitor", "Timeout alarm canceled for $medicationId at $time")
        } catch (e: Exception) {
            Log.e("MedicationMonitor", "Failed to cancel timeout alarm", e)
        }
    }

    fun checkMedicationConflicts(newMedication: Medication) {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("users")
            .document(userId)
            .collection("medications")
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { documents ->
                val medications = documents.mapNotNull { it.toObject(Medication::class.java) }
                val conflicts = findConflicts(newMedication, medications)
                
                if (conflicts.isNotEmpty()) {
                    notifyConflicts(newMedication, conflicts)
                }
            }
    }

    private fun findConflicts(newMed: Medication, existingMeds: List<Medication>): List<Medication> {
        // This is a simplified conflict check. In a real application,
        // you would want to implement more sophisticated drug interaction checks
        return existingMeds.filter { existingMed ->
            existingMed.id != newMed.id && 
            existingMed.times.any { existingTime ->
                newMed.times.any { newTime ->
                    // Check if medications are taken within 2 hours of each other
                    val existingMinutes = existingTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
                    val newMinutes = newTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
                    Math.abs(existingMinutes - newMinutes) < 120
                }
            }
        }
    }

    private fun notifyConflicts(medication: Medication, conflicts: List<Medication>) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = MEDICATION_CONFLICT_ACTION
            putExtra("MEDICATION_NAME", medication.name)
            putExtra("CONFLICT_NAMES", conflicts.joinToString(", ") { it.name })
        }

        // Send immediate notification
        context.sendBroadcast(intent)
    }
} 