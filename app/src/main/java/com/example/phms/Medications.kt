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


data class Medication(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val sideEffects: String = "",
    val time: String = ""
) {
    constructor() : this("", "", "", "", "", "")
}

class Medications : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var medList: RecyclerView
    private lateinit var addMedButton: Button
    private lateinit var adapter: MedicationAdapter
    private val medications = mutableListOf<Medication>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medications)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        medList = findViewById(R.id.medicationList)
        addMedButton = findViewById(R.id.addMedicationButton)

        adapter = MedicationAdapter(medications,
            onEdit = { showMedicationDialog(it) },
            onDelete = { deleteMedication(it) }
        )
        medList.layoutManager = LinearLayoutManager(this)
        medList.adapter = adapter

        addMedButton.setOnClickListener {
            showMedicationDialog(null)
        }

        loadMedications()
    }

    private fun loadMedications() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("medications")
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
        val timeButton = dialogView.findViewById<Button>(R.id.medicationTime)

        var selectedTime = existing?.time ?: ""

        nameInput.setText(existing?.name ?: "")
        dosageInput.setText(existing?.dosage ?: "")
        frequencyInput.setText(existing?.frequency ?: "")
        sideEffectsInput.setText(existing?.sideEffects ?: "")
        timeButton.text = if (selectedTime.isNotEmpty()) selectedTime else "Set Time"

        timeButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, h, m ->
                selectedTime = String.format("%02d:%02d", h, m)
                timeButton.text = selectedTime
            }, hour, minute, true).show()
        }

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
                    time = selectedTime
                )
                saveMedication(med)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveMedication(medication: Medication) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("medications").document(medication.id)
            .set(medication)
            .addOnSuccessListener {
                loadMedications()
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                scheduleReminder(medication)

            }
    }

    private fun deleteMedication(medication: Medication) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("medications").document(medication.id)
            .delete()
            .addOnSuccessListener {
                loadMedications()
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            }
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private fun scheduleReminder(med: Medication) {
        if (med.time.isEmpty()) return

        val timeParts = med.time.split(":")
        if (timeParts.size != 2) return

        val hour = timeParts[0].toIntOrNull() ?: return
        val minute = timeParts[1].toIntOrNull() ?: return

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
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            med.id.hashCode(),
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
        holder.timeText.text = "Time: ${med.time}"

        holder.editBtn.setOnClickListener { onEdit(med) }
        holder.deleteBtn.setOnClickListener { onDelete(med) }
    }

    override fun getItemCount() = medications.size
}
