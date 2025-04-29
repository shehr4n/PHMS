package com.example.phms

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class Search : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private lateinit var categorySpinner: Spinner
    private lateinit var timeframeSpinner: Spinner
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var noResultsTextView: TextView
    lateinit var backButton: ImageView
    
    private val categories = arrayOf( "Medications")
    private val timeframes = arrayOf("Current", "Past", "All")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        categorySpinner = findViewById(R.id.categorySpinner)
        timeframeSpinner = findViewById(R.id.timeframeSpinner)
        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
        noResultsTextView = findViewById(R.id.noResultsTextView)

        // Setup spinners
        setupSpinners()

        // Setup RecyclerView
        resultsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Setup search button
        searchButton.setOnClickListener {
            performSearch()
        }

        backButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }
    }

    private fun setupSpinners() {
        // Category spinner
        ArrayAdapter(this, android.R.layout.simple_spinner_item, categories).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            categorySpinner.adapter = adapter
        }

        // Timeframe spinner
        ArrayAdapter(this, android.R.layout.simple_spinner_item, timeframes).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            timeframeSpinner.adapter = adapter
        }
    }

    private fun performSearch() {
        val category = categorySpinner.selectedItem.toString()
        val timeframe = timeframeSpinner.selectedItem.toString()
        val searchQuery = searchEditText.text.toString().trim().lowercase()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to search", Toast.LENGTH_SHORT).show()
            return
        }

        when (category) {
            "Medications" -> searchMedications(currentUser.uid, timeframe, searchQuery)
        }
    }

    private fun searchMedications(userId: String, timeframe: String, query: String) {
        db.collection("users")
            .document(userId)
            .collection("medications")
            .get()
            .addOnSuccessListener { documents ->
                val medications = documents.mapNotNull { doc ->
                    val medication = doc.toObject(Medication::class.java)
                    
                    // Filter based on timeframe
                    when (timeframe) {
                        "Current" -> {
                            if (medication.status != "active") return@mapNotNull null
                        }
                        "Past" -> {
                            if (medication.status != "discontinued") return@mapNotNull null
                        }
                        // "All" timeframe doesn't filter by status
                    }
                    
                    // Filter based on search query
                    if (query.isNotEmpty() && 
                        !medication.name.lowercase().contains(query) && 
                        !medication.dosage.lowercase().contains(query)) {
                        return@mapNotNull null
                    }
                    
                    SearchResult(
                        id = medication.id,
                        title = medication.name,
                        date = medication.endDate ?: Date(), // Use end date for discontinued meds, current date for active
                        type = "Medication",
                        details = buildString {
                            append("Dosage: ${medication.dosage}")
                            append(", Frequency: ${medication.frequency}")
                            if (medication.times.isNotEmpty()) {
                                append(", Times: ${medication.times.joinToString(", ")}")
                            }
                            if (medication.status == "discontinued" && medication.discontinuationReason.isNotEmpty()) {
                                append("\nDiscontinued: ${medication.discontinuationReason}")
                            }
                        }
                    )
                }
                
                displayResults(medications)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error searching medications", Toast.LENGTH_SHORT).show()
            }
    }



    private fun displayResults(results: List<SearchResult>) {
        if (results.isEmpty()) {
            resultsRecyclerView.visibility = View.GONE
            noResultsTextView.visibility = View.VISIBLE
        } else {
            resultsRecyclerView.visibility = View.VISIBLE
            noResultsTextView.visibility = View.GONE
            
            val adapter = SearchResultsAdapter(results) { result ->
                // Handle result click based on type
                when (result.type) {
                    "Medication" -> {
                        // Navigate to medication details
                        Toast.makeText(this, "Viewing medication: ${result.title}", Toast.LENGTH_SHORT).show()
                    }

                }
            }
            resultsRecyclerView.adapter = adapter
        }
    }
}

data class SearchResult(
    val id: String,
    val title: String,
    val date: Date,
    val type: String,
    val details: String
) 