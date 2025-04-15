package com.example.phms

//import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

data class VitalSign(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val value: Double,
    val timestamp: Date,
    val notes: String = "",
    val isAbnormal: Boolean = false
)

class Vitals : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var vitalsList: RecyclerView
    private lateinit var addVitalButton: Button
    private lateinit var filterSpinner: Spinner
    private lateinit var dateFilter: EditText
    private lateinit var adapter: VitalsAdapter
    private val vitalSigns = mutableListOf<VitalSign>()

    // Normal ranges for different vital signs
    private val normalRanges = mapOf(
        "HeartRate" to 60.0..100.0,
        "BloodPressure" to 90.0..120.0,
        "Glucose" to 70.0..140.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vitals)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        vitalsList = findViewById(R.id.vitalsList)
        addVitalButton = findViewById(R.id.addVitalButton)
        filterSpinner = findViewById(R.id.filterSpinner)
        dateFilter = findViewById(R.id.dateFilter)

        setupRecyclerView()
        setupSpinner()
        setupListeners()
        loadVitalSigns()
    }

    private fun setupRecyclerView() {
        adapter = VitalsAdapter(vitalSigns) { vitalSign ->
            showVitalSignDialog(vitalSign)
        }
        vitalsList.layoutManager = LinearLayoutManager(this)
        vitalsList.adapter = adapter
    }

    private fun setupSpinner() {
        val types = arrayOf("All", "HeartRate", "BloodPressure", "Glucose")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = adapter
    }

    private fun setupListeners() {
        addVitalButton.setOnClickListener {
            showAddVitalSignDialog()
        }

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                filterVitalSigns()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadVitalSigns() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .collection("vitals")
                .get()
                .addOnSuccessListener { documents ->
                    vitalSigns.clear()
                    for (document in documents) {
                        val vitalSign = document.toObject(VitalSign::class.java)
                        vitalSigns.add(vitalSign)
                    }
                    adapter.notifyDataSetChanged()
                }
        }
    }

    private fun showAddVitalSignDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_vital, null)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.typeSpinner)
        val valueInput = dialogView.findViewById<EditText>(R.id.valueInput)
        val notesInput = dialogView.findViewById<EditText>(R.id.notesInput)

        val types = arrayOf("HeartRate", "BloodPressure", "Glucose")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = spinnerAdapter

        AlertDialog.Builder(this)
            .setTitle("Add Vital Sign")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val type = typeSpinner.selectedItem.toString()
                val value = valueInput.text.toString().toDoubleOrNull() ?: 0.0
                val notes = notesInput.text.toString()
                val timestamp = Date()
                val isAbnormal = checkIfAbnormal(type, value)

                val vitalSign = VitalSign(
                    type = type,
                    value = value,
                    timestamp = timestamp,
                    notes = notes,
                    isAbnormal = isAbnormal
                )

                saveVitalSign(vitalSign)
                if (isAbnormal) {
                    showAbnormalAlert(vitalSign)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showVitalSignDialog(vitalSign: VitalSign) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_vital, null)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.typeSpinner)
        val valueInput = dialogView.findViewById<EditText>(R.id.valueInput)
        val notesInput = dialogView.findViewById<EditText>(R.id.notesInput)

        val types = arrayOf("HeartRate", "BloodPressure", "Glucose")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = spinnerAdapter

        typeSpinner.setSelection(types.indexOf(vitalSign.type))
        valueInput.setText(vitalSign.value.toString())
        notesInput.setText(vitalSign.notes)

        AlertDialog.Builder(this)
            .setTitle("Edit Vital Sign")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val type = typeSpinner.selectedItem.toString()
                val value = valueInput.text.toString().toDoubleOrNull() ?: 0.0
                val notes = notesInput.text.toString()
                val isAbnormal = checkIfAbnormal(type, value)

                val updatedVitalSign = vitalSign.copy(
                    type = type,
                    value = value,
                    notes = notes,
                    isAbnormal = isAbnormal
                )

                updateVitalSign(updatedVitalSign)
                if (isAbnormal) {
                    showAbnormalAlert(updatedVitalSign)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveVitalSign(vitalSign: VitalSign) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .collection("vitals")
                .document(vitalSign.id)
                .set(vitalSign)
                .addOnSuccessListener {
                    loadVitalSigns()
                    Toast.makeText(this, "Vital sign saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error saving vital sign", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateVitalSign(vitalSign: VitalSign) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .collection("vitals")
                .document(vitalSign.id)
                .set(vitalSign)
                .addOnSuccessListener {
                    loadVitalSigns()
                    Toast.makeText(this, "Vital sign updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error updating vital sign", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteVitalSign(vitalSign: VitalSign) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .collection("vitals")
                .document(vitalSign.id)
                .delete()
                .addOnSuccessListener {
                    loadVitalSigns()
                    Toast.makeText(this, "Vital sign deleted successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error deleting vital sign", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun checkIfAbnormal(type: String, value: Double): Boolean {
        val range = normalRanges[type] ?: return false
        return value < range.start || value > range.endInclusive
    }

    private fun showAbnormalAlert(vitalSign: VitalSign) {
        AlertDialog.Builder(this)
            .setTitle("Abnormal Vital Sign Detected!")
            .setMessage("Your ${vitalSign.type} reading of ${vitalSign.value} is outside the normal range. Please consult your healthcare provider.")
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }

    private fun filterVitalSigns() {
        val selectedType = filterSpinner.selectedItem.toString()
        val filteredSigns = if (selectedType == "All") {
            vitalSigns
        } else {
            vitalSigns.filter { it.type == selectedType }
        }
        adapter.updateData(filteredSigns)
    }
}

class VitalsAdapter(
    private var vitalSigns: List<VitalSign>,
    private val onItemClick: (VitalSign) -> Unit
) : RecyclerView.Adapter<VitalsAdapter.VitalSignViewHolder>() {

    class VitalSignViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val typeText: TextView = view.findViewById(R.id.typeText)
        val valueText: TextView = view.findViewById(R.id.valueText)
        val timestampText: TextView = view.findViewById(R.id.timestampText)
        val notesText: TextView = view.findViewById(R.id.notesText)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VitalSignViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vital_sign, parent, false)
        return VitalSignViewHolder(view)
    }

    override fun onBindViewHolder(holder: VitalSignViewHolder, position: Int) {
        val vitalSign = vitalSigns[position]
        holder.typeText.text = vitalSign.type
        holder.valueText.text = vitalSign.value.toString()
        holder.timestampText.text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            .format(vitalSign.timestamp)
        holder.notesText.text = vitalSign.notes

        if (vitalSign.isAbnormal) {
            holder.itemView.setBackgroundColor(android.graphics.Color.RED)
        } else {
            holder.itemView.setBackgroundColor(android.graphics.Color.WHITE)
        }

        holder.itemView.setOnClickListener { onItemClick(vitalSign) }
    }

    override fun getItemCount() = vitalSigns.size

    fun updateData(newVitalSigns: List<VitalSign>) {
        vitalSigns = newVitalSigns
        notifyDataSetChanged()
    }
}