package com.example.phms

import android.Manifest
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import java.util.Calendar
import java.text.SimpleDateFormat


data class Medication(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val sideEffects: String = "",
    val times: List<String> = listOf(),
    val status: String = "active", // "active" or "discontinued"
    val startDate: Date = Date(),
    val endDate: Date? = null,
    val discontinuationReason: String = ""
) {
    constructor() : this("", "", "", "", "", listOf(), "active", Date(), null, "")
}

class Medications : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var medList: RecyclerView
    private lateinit var addMedButton: Button
    private lateinit var viewHistoryButton: Button
    private lateinit var testReminderButton: Button
    private lateinit var adapter: MedicationAdapter
    private val medications = mutableListOf<Medication>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medications)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        medList = findViewById(R.id.medicationList)
        addMedButton = findViewById(R.id.addMedicationButton)
        viewHistoryButton = findViewById(R.id.viewMedicationHistoryButton)
        testReminderButton = findViewById(R.id.testReminderButton)

        adapter = MedicationAdapter(medications,
            onEdit = { showMedicationDialog(it) },
            onDelete = { deleteMedication(it) }
        )
        medList.layoutManager = LinearLayoutManager(this)
        medList.adapter = adapter

        addMedButton.setOnClickListener {
            showMedicationDialog(null)
        }
        
        viewHistoryButton.setOnClickListener {
            viewMedicationHistory()
        }
        
        testReminderButton.setOnClickListener {
            sendTestReminder()
        }

        loadMedications()
    }

    private fun sendTestReminder() {
        if (medications.isEmpty()) {
            Toast.makeText(this, "Add a medication first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val medication = medications[0]
        
        // Use MedicationMonitor's debug function for immediate and scheduled reminders
        val monitor = MedicationMonitor(this)
        monitor.scheduleDebugReminder(medication)
        
        // Log notification for history
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val log = hashMapOf(
                "medicationId" to medication.id,
                "medicationName" to medication.name,
                "reminderTime" to SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                "timestamp" to Date(),
                "status" to "notification_sent"
            )
            
            db.collection("users").document(userId)
                .collection("medicationLogs")
                .add(log)
        }
    }
    
    private fun viewMedicationHistory() {
        val user = auth.currentUser ?: return
        
        db.collection("users").document(user.uid)
            .collection("medicationLogs")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "No medication history found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                
                val historyItems = mutableListOf<String>()
                for (doc in documents) {
                    val medicationId = doc.getString("medicationId") ?: continue
                    val status = doc.getString("status") ?: continue
                    val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: continue
                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(timestamp)
                    
                    // Find medication name
                    var medicationName = doc.getString("medicationName") ?: ""
                    if (medicationName.isEmpty()) {
                        for (med in medications) {
                            if (med.id == medicationId) {
                                medicationName = med.name
                                break
                            }
                        }
                    }
                    
                    val statusText = when(status) {
                        "taken" -> "✅ Taken"
                        "missed" -> "❌ Missed"
                        "skipped" -> "⏭️ Skipped"
                        else -> status
                    }
                    
                    historyItems.add("$dateStr - $medicationName: $statusText")
                }
                
                // Show history dialog
                AlertDialog.Builder(this)
                    .setTitle("Medication History")
                    .setItems(historyItems.toTypedArray(), null)
                    .setPositiveButton("OK", null)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading history", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadMedications() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("medications")
            .whereEqualTo("status", "active")  // Only load active medications
            .get()
            .addOnSuccessListener { documents ->
                medications.clear()
                for (doc in documents) {
                    val med = doc.toObject(Medication::class.java)
                    medications.add(med)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun showMedicationDialog(existing: Medication?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_medication, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.medicationName)
        val dosageInput = dialogView.findViewById<EditText>(R.id.medicationDosage)
        val frequencyInput = dialogView.findViewById<EditText>(R.id.medicationFrequency)
        val sideEffectsInput = dialogView.findViewById<EditText>(R.id.medicationSideEffects)
        val timeContainer = dialogView.findViewById<LinearLayout>(R.id.timeButtonsContainer)

        nameInput.setText(existing?.name ?: "")
        dosageInput.setText(existing?.dosage ?: "")
        frequencyInput.setText(existing?.frequency ?: "")
        sideEffectsInput.setText(existing?.sideEffects ?: "")

        val selectedTimes = mutableListOf<String>()
        if (existing != null) {
            selectedTimes.addAll(existing.times)
        }

        // Add TextWatcher to frequency input for real-time updates
        frequencyInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateTimeButtons(timeContainer, s?.toString() ?: "", selectedTimes)
            }
        })

        // Initial setup of time buttons
        updateTimeButtons(timeContainer, existing?.frequency ?: "", selectedTimes)

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add Medication" else "Edit Medication")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val med = Medication(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    name = nameInput.text.toString(),
                    dosage = dosageInput.text.toString(),
                    frequency = frequencyInput.text.toString(),
                    sideEffects = sideEffectsInput.text.toString(),
                    times = selectedTimes.toList()
                )
                saveMedication(med)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateTimeButtons(container: LinearLayout, frequency: String, selectedTimes: MutableList<String>) {
        container.removeAllViews()
        
        val timesPerDay = when {
            frequency.toLowerCase().contains("twice") || 
            frequency.toLowerCase().contains("2 time") || 
            frequency.toLowerCase().contains("2x") -> 2
            
            frequency.toLowerCase().contains("thrice") || 
            frequency.toLowerCase().contains("3 time") || 
            frequency.toLowerCase().contains("3x") -> 3
            
            else -> 1
        }

        for (i in 0 until timesPerDay) {
            val timeButton = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                text = if (i < selectedTimes.size) selectedTimes[i] else "Set Time ${i + 1}"
                setOnClickListener {
                    showTimePickerDialog(i) { time ->
                        if (i < selectedTimes.size) {
                            selectedTimes[i] = time
                        } else {
                            selectedTimes.add(time)
                        }
                        text = time
                    }
                }
            }
            container.addView(timeButton)
        }

        // Trim excess times if frequency is reduced
        while (selectedTimes.size > timesPerDay) {
            selectedTimes.removeAt(selectedTimes.size - 1)
        }
    }

    private fun showTimePickerDialog(index: Int, onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, h, m ->
            val time = String.format("%02d:%02d", h, m)
            onTimeSelected(time)
        }, hour, minute, true).show()
    }

    private fun saveMedication(medication: Medication) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("medications").document(medication.id)
            .set(medication)
            .addOnSuccessListener {
                loadMedications()
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                
                // Initialize monitoring for the medication
                val monitor = MedicationMonitor(this)
                monitor.scheduleReminders(medication)
                monitor.checkMedicationConflicts(medication)
            }
    }

    private fun deleteMedication(medication: Medication) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_discontinue_medication, null)
        val reasonInput = dialogView.findViewById<EditText>(R.id.discontinuationReason)

        AlertDialog.Builder(this)
            .setTitle("Discontinue Medication")
            .setMessage("Are you sure you want to discontinue ${medication.name}?")
            .setView(dialogView)
            .setPositiveButton("Discontinue") { _, _ ->
                discontinueMedication(medication, reasonInput.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun discontinueMedication(medication: Medication, reason: String) {
        val user = auth.currentUser ?: return
        val discontinuedMed = medication.copy(
            status = "discontinued",
            endDate = Date(),
            discontinuationReason = reason
        )

        db.collection("users").document(user.uid)
            .collection("medications").document(medication.id)
            .set(discontinuedMed)
            .addOnSuccessListener {
                loadMedications()
                Toast.makeText(this, "Medication discontinued", Toast.LENGTH_SHORT).show()
            }
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private fun scheduleReminder(med: Medication) {
        for (time in med.times) {
            val timeParts = time.split(":")
            if (timeParts.size != 2) continue

            val hour = timeParts[0].toIntOrNull() ?: continue
            val minute = timeParts[1].toIntOrNull() ?: continue

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
            }

            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(this, "Enable exact alarms in system settings", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val intent = Intent(this, ReminderReceiver::class.java).apply {
                putExtra("MED_NAME", med.name)
                putExtra("MED_TIME", time)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                "${med.id}_${time}".hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } catch (e: SecurityException) {
                Toast.makeText(this, "Permission denied for exact alarms", Toast.LENGTH_SHORT).show()
            }
        }
    }


}

class MedicationAdapter(
    private var medications: List<Medication>,
    private val onEdit: (Medication) -> Unit,
    private val onDelete: (Medication) -> Unit
) : RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder>() {

    class MedicationViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.medName)
        val dosageText: TextView = view.findViewById(R.id.medDosage)
        val frequencyText: TextView = view.findViewById(R.id.medFrequency)
        val timeText: TextView = view.findViewById(R.id.medTime)
        val editBtn: ImageButton = view.findViewById(R.id.editMed)
        val deleteBtn: ImageButton = view.findViewById(R.id.deleteMed)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): MedicationViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medication, parent, false)
        return MedicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        val med = medications[position]
        holder.nameText.text = med.name
        holder.dosageText.text = "Dosage: ${med.dosage}"
        holder.frequencyText.text = "Frequency: ${med.frequency}"
        holder.timeText.text = "Time: ${med.times.joinToString(", ")}"

        holder.editBtn.setOnClickListener { onEdit(med) }
        holder.deleteBtn.setOnClickListener { onDelete(med) }
    }

    override fun getItemCount() = medications.size
}
