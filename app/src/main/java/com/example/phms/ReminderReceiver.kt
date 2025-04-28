package com.example.phms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class ReminderReceiver : BroadcastReceiver() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        const val ACTION_TAKE_MEDICATION = "com.example.phms.TAKE_MEDICATION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "Received action: ${intent.action}")

        when (intent.action) {
            MedicationMonitor.MEDICATION_REMINDER_ACTION -> handleMedicationReminder(context, intent)
            MedicationMonitor.MEDICATION_TIMEOUT_ACTION -> handleMedicationTimeout(context, intent)
            MedicationMonitor.MEDICATION_CONFLICT_ACTION -> handleMedicationConflict(context, intent)
            ACTION_TAKE_MEDICATION -> handleTakeMedication(context, intent)
        }
    }
    
    private fun handleTakeMedication(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "Handling take medication action")
        val medicationId = intent.getStringExtra("MEDICATION_ID") ?: return
        val medicationName = intent.getStringExtra("MEDICATION_NAME") ?: return
        val reminderTime = intent.getStringExtra("REMINDER_TIME") ?: return
        
        // Cancel the reminder notification
        val notificationId = "${medicationId}_${reminderTime}".hashCode()
        NotificationUtils.cancelNotification(context, notificationId)
        
        // Important: Let MedicationMonitor handle the recording and cancellation
        val monitor = MedicationMonitor(context)
        monitor.confirmMedicationIntake(medicationId, reminderTime, medicationName)
        
        // Show feedback toast
        Toast.makeText(
            context, 
            "Medication intake recorded: $medicationName", 
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun handleMedicationReminder(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "Handling medication reminder")
        val medicationId = intent.getStringExtra("MEDICATION_ID") ?: return
        val medicationName = intent.getStringExtra("MEDICATION_NAME") ?: return
        val reminderTime = intent.getStringExtra("REMINDER_TIME") ?: return

        // Check if we've already sent a notification for this medication and time today
        checkAndLogNotification(context, medicationId, medicationName, reminderTime)
    }
    
    private fun checkAndLogNotification(context: Context, medicationId: String, medicationName: String, reminderTime: String) {
        val userId = auth.currentUser?.uid ?: return
        val today = Date()
        val calendar = java.util.Calendar.getInstance().apply {
            time = today
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.time
        
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.time
        
        // Check if we already logged a notification for this medication today
        db.collection("users").document(userId)
            .collection("medicationLogs")
            .whereEqualTo("medicationId", medicationId)
            .whereEqualTo("reminderTime", reminderTime)
            .whereEqualTo("status", "notification_sent")
            .whereGreaterThanOrEqualTo("timestamp", startOfDay)
            .whereLessThan("timestamp", endOfDay)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // No notification sent yet today, send it and log it
                    NotificationUtils.showMedicationReminder(
                        context,
                        medicationId,
                        medicationName,
                        reminderTime
                    )
                    
                    // Log notification
                    val log = hashMapOf(
                        "medicationId" to medicationId,
                        "medicationName" to medicationName,
                        "reminderTime" to reminderTime,
                        "timestamp" to Date(),
                        "status" to "notification_sent"
                    )
                    
                    db.collection("users").document(userId)
                        .collection("medicationLogs")
                        .add(log)
                } else {
                    // We already sent a notification today, just show it without logging again
                    Log.d("ReminderReceiver", "Already sent notification for $medicationName today, showing without logging")
                    NotificationUtils.showMedicationReminder(
                        context,
                        medicationId,
                        medicationName,
                        reminderTime
                    )
                }
            }
            .addOnFailureListener { e ->
                // On failure, just show the notification
                Log.e("ReminderReceiver", "Error checking notification history", e)
                NotificationUtils.showMedicationReminder(
                    context,
                    medicationId,
                    medicationName,
                    reminderTime
                )
            }
    }

    private fun handleMedicationTimeout(context: Context, intent: Intent) {
        val medicationId = intent.getStringExtra("MEDICATION_ID") ?: return
        val medicationName = intent.getStringExtra("MEDICATION_NAME") ?: return
        val reminderTime = intent.getStringExtra("REMINDER_TIME") ?: return

        // Check if medication was already taken
        checkIfMedicationTaken(context, medicationId, medicationName, reminderTime)
    }
    
    private fun checkIfMedicationTaken(context: Context, medicationId: String, medicationName: String, reminderTime: String) {
        val userId = auth.currentUser?.uid ?: return
        val today = Date()
        val calendar = java.util.Calendar.getInstance().apply {
            time = today
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.time
        
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.time
        
        // Check if medication was already taken today
        db.collection("users").document(userId)
            .collection("medicationLogs")
            .whereEqualTo("medicationId", medicationId)
            .whereEqualTo("time", reminderTime)
            .whereEqualTo("status", "taken")
            .whereGreaterThanOrEqualTo("timestamp", startOfDay)
            .whereLessThan("timestamp", endOfDay)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Medication not taken, send emergency alert
                    sendEmergencyAlert(context, medicationId, medicationName, reminderTime)
                } else {
                    // Already taken, no need for timeout alert
                    Log.d("ReminderReceiver", "Medication $medicationName was already taken today, skipping timeout alert")
                }
            }
            .addOnFailureListener { e ->
                // On failure, assume not taken and send alert
                Log.e("ReminderReceiver", "Error checking if medication was taken", e)
                sendEmergencyAlert(context, medicationId, medicationName, reminderTime)
            }
    }
    
    private fun sendEmergencyAlert(context: Context, medicationId: String, medicationName: String, reminderTime: String) {
        // Get emergency contact information
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val emergencyContactName = document.getString("emergencyContactName")
                val emergencyContactPhone = document.getString("emergencyContactPhone")

                // Use NotificationUtils to show the missed medication alert
                NotificationUtils.showMissedMedicationAlert(
                    context,
                    medicationId,
                    medicationName,
                    reminderTime,
                    emergencyContactName
                )

                // Log the missed medication
                val missedLog = hashMapOf(
                    "medicationId" to medicationId,
                    "medicationName" to medicationName,
                    "scheduledTime" to reminderTime,
                    "timestamp" to Date(),
                    "status" to "missed",
                    "notifiedContact" to emergencyContactName
                )

                db.collection("users")
                    .document(userId)
                    .collection("medicationLogs")
                    .add(missedLog)
                    
                // Send SMS to emergency contact if available
                if (!emergencyContactPhone.isNullOrEmpty()) {
                    sendSmsToEmergencyContact(context, emergencyContactPhone, medicationName, emergencyContactName)
                }
            }
    }
    
    private fun sendSmsToEmergencyContact(context: Context, phoneNumber: String, medicationName: String, contactName: String?) {
        try {
            val userName = auth.currentUser?.displayName ?: "The user"
            val message = "$userName missed their scheduled dose of $medicationName. Please check on them."
            
            // Get SMS manager to send text
            val smsManager = android.telephony.SmsManager.getDefault()
            smsManager.sendTextMessage(
                phoneNumber,
                null,
                message,
                null,
                null
            )
            
            Log.d("ReminderReceiver", "Successfully sent SMS to emergency contact: $contactName")
            Toast.makeText(
                context,
                "Alert sent to emergency contact",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e("ReminderReceiver", "Failed to send SMS to emergency contact", e)
            Toast.makeText(
                context,
                "Failed to send SMS to emergency contact",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleMedicationConflict(context: Context, intent: Intent) {
        val medicationName = intent.getStringExtra("MEDICATION_NAME") ?: return
        val conflictNames = intent.getStringExtra("CONFLICT_NAMES") ?: return

        // Use NotificationUtils to show the conflict notification
        NotificationUtils.showMedicationConflict(
            context,
            medicationName,
            conflictNames
        )
    }
}