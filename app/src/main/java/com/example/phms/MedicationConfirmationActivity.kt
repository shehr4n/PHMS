package com.example.phms

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MedicationConfirmationActivity : AppCompatActivity() {
    private lateinit var medicationNameText: TextView
    private lateinit var timeText: TextView
    private lateinit var confirmButton: Button
    private lateinit var skipButton: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medication_confirmation)

        medicationNameText = findViewById(R.id.medicationNameText)
        timeText = findViewById(R.id.timeText)
        confirmButton = findViewById(R.id.confirmButton)
        skipButton = findViewById(R.id.skipButton)

        val medicationId = intent.getStringExtra("MEDICATION_ID") ?: return finish()
        val reminderTime = intent.getStringExtra("REMINDER_TIME") ?: return finish()
        val medicationName = intent.getStringExtra("MEDICATION_NAME") ?: ""

        // Cancel the notification using NotificationUtils
        val notificationId = "${medicationId}_${reminderTime}".hashCode()
        NotificationUtils.cancelNotification(this, notificationId)

        // Display medication details
        if (medicationName.isNotEmpty()) {
            medicationNameText.text = "Did you take $medicationName?"
        } else {
            // Fallback to loading from Firebase
            loadMedicationDetails(medicationId)
        }
        
        // Format and display time
        try {
            val (hour, minute) = reminderTime.split(":")
            val timeDisplay = String.format("%02d:%02d", hour.toInt(), minute.toInt())
            timeText.text = "Scheduled time: $timeDisplay"
        } catch (e: Exception) {
            timeText.text = "Scheduled time: $reminderTime"
        }

        confirmButton.setOnClickListener {
            val monitor = MedicationMonitor(this)
            monitor.confirmMedicationIntake(medicationId, reminderTime)
            
            // Log the confirmation
            val userId = auth.currentUser?.uid ?: return@setOnClickListener finish()
            
            val takenLog = hashMapOf(
                "medicationId" to medicationId,
                "medicationName" to medicationName,
                "time" to reminderTime,
                "timestamp" to java.util.Date(),
                "status" to "taken"
            )
            
            db.collection("users")
                .document(userId)
                .collection("medicationLogs")
                .add(takenLog)
                .addOnSuccessListener {
                    android.widget.Toast.makeText(this, 
                        "Medication intake confirmed", 
                        android.widget.Toast.LENGTH_SHORT).show()
                    finish()
                }
        }

        skipButton.setOnClickListener {
            // Log the skip
            val userId = auth.currentUser?.uid ?: return@setOnClickListener finish()
            
            val skippedLog = hashMapOf(
                "medicationId" to medicationId,
                "medicationName" to medicationName,
                "time" to reminderTime,
                "timestamp" to java.util.Date(),
                "status" to "skipped"
            )

            db.collection("users")
                .document(userId)
                .collection("medicationLogs")
                .add(skippedLog)
                .addOnSuccessListener {
                    android.widget.Toast.makeText(this, 
                        "Dose skipped", 
                        android.widget.Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
    }
    
    private fun loadMedicationDetails(medicationId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(userId)
            .collection("medications")
            .document(medicationId)
            .get()
            .addOnSuccessListener { document ->
                val medication = document.toObject(Medication::class.java)
                if (medication != null) {
                    medicationNameText.text = "Did you take ${medication.name}?"
                }
            }
    }
} 