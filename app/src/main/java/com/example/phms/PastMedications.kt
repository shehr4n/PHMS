package com.example.phms

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PastMedications : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: PastMedicationsAdapter
    private val medications = mutableListOf<Medication>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_past_medications)

        recyclerView = findViewById(R.id.pastMedicationsRecyclerView)
        emptyView = findViewById(R.id.emptyView)

        setupRecyclerView()
        loadPastMedications()
    }

    private fun setupRecyclerView() {
        adapter = PastMedicationsAdapter(medications)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadPastMedications() {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("users").document(userId)
            .collection("medications")
            .whereEqualTo("status", "discontinued")
            .get()
            .addOnSuccessListener { documents ->
                medications.clear()
                for (document in documents) {
                    val medication = document.toObject(Medication::class.java)
                    medications.add(medication)
                }
                medications.sortByDescending { it.endDate }
                updateUI()
            }
    }

    private fun updateUI() {
        if (medications.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            adapter.notifyDataSetChanged()
        }
    }
}

class PastMedicationsAdapter(private val medications: List<Medication>) :
    RecyclerView.Adapter<PastMedicationsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.medicationName)
        val detailsText: TextView = view.findViewById(R.id.dosageText)
        val datesText: TextView = view.findViewById(R.id.activeDatesText)
        val reasonText: TextView = view.findViewById(R.id.discontinuationReason)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_past_medication, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val medication = medications[position]
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        holder.nameText.text = medication.name
        holder.detailsText.text = "${medication.dosage} - ${medication.frequency}"
        
        val dates = "Active: ${dateFormat.format(medication.startDate)} - ${dateFormat.format(medication.endDate)}"
        holder.datesText.text = dates
        
        holder.reasonText.text = "Reason: ${medication.discontinuationReason}"
    }

    override fun getItemCount() = medications.size
} 