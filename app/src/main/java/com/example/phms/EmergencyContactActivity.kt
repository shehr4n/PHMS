package com.example.phms

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.phms.model.EmergencyContact
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EmergencyContactActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var addContactFab: FloatingActionButton
    private lateinit var adapter: EmergencyContactAdapter
    private val contactsList = mutableListOf<EmergencyContact>()
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val TAG = "EmergencyContactActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_contact)

        recyclerView = findViewById(R.id.contactsRecyclerView)
        addContactFab = findViewById(R.id.addContactFab)
        
        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = EmergencyContactAdapter(
            contactsList,
            onEditClick = { contact -> showEditContactDialog(contact) },
            onDeleteClick = { contact -> confirmDeleteContact(contact) },
            onSetPrimaryClick = { contact -> setPrimaryContact(contact) }
        )
        recyclerView.adapter = adapter

        // Set up Add button
        addContactFab.setOnClickListener {
            showAddContactDialog()
        }

        // Load contacts from Firestore
        loadEmergencyContacts()
    }

    private fun loadEmergencyContacts() {
        if (userId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(userId)
            .collection("emergencyContacts")
            .get()
            .addOnSuccessListener { documents ->
                contactsList.clear()
                for (document in documents) {
                    val contact = document.toObject(EmergencyContact::class.java)
                    contactsList.add(contact)
                }
                
                // Update UI based on contacts list
                updateEmptyStateVisibility()
                
                // Notify adapter of data change
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting emergency contacts", e)
                Toast.makeText(this, "Failed to load contacts", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEmptyStateVisibility() {
        val emptyView = findViewById<View>(R.id.emptyView)
        if (contactsList.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showAddContactDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_emergency_contact, null)
        val nameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.contactNameEditText)
        val phoneInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.contactPhoneEditText)
        val emailInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.contactEmailEditText)
        val relationshipInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.contactRelationshipEditText)

        AlertDialog.Builder(this)
            .setTitle("Add Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                
                if (name.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(this, "Name and phone number are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val contact = EmergencyContact(
                    id = db.collection("users").document(userId).collection("emergencyContacts").document().id,
                    name = name,
                    phone = phone,
                    email = emailInput.text.toString().trim(),
                    relationship = relationshipInput.text.toString().trim(),
                    isPrimary = contactsList.isEmpty() // Make primary if it's the first contact
                )
                
                saveContact(contact)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditContactDialog(contact: EmergencyContact) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_emergency_contact, null)
        val nameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.contactNameEditText)
        val phoneInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.contactPhoneEditText)
        val emailInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.contactEmailEditText)
        val relationshipInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.contactRelationshipEditText)
        
        // Populate fields with existing data
        nameInput.setText(contact.name)
        phoneInput.setText(contact.phone)
        emailInput.setText(contact.email)
        relationshipInput.setText(contact.relationship)

        AlertDialog.Builder(this)
            .setTitle("Edit Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                
                if (name.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(this, "Name and phone number are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val updatedContact = contact.copy(
                    name = name,
                    phone = phone,
                    email = emailInput.text.toString().trim(),
                    relationship = relationshipInput.text.toString().trim()
                )
                
                saveContact(updatedContact)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteContact(contact: EmergencyContact) {
        val isPrimary = contact.isPrimary
        val message = if (isPrimary) {
            "This is your primary emergency contact. Are you sure you want to delete it?"
        } else {
            "Are you sure you want to delete this emergency contact?"
        }
        
        AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage(message)
            .setPositiveButton("Delete") { _, _ ->
                deleteContact(contact)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveContact(contact: EmergencyContact) {
        if (userId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        
        db.collection("users").document(userId).collection("emergencyContacts")
            .document(contact.id)
            .set(contact)
            .addOnSuccessListener {
                // Update local list
                val existingIndex = contactsList.indexOfFirst { it.id == contact.id }
                if (existingIndex >= 0) {
                    contactsList[existingIndex] = contact
                } else {
                    contactsList.add(contact)
                }
                
                updateEmptyStateVisibility()
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save contact", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteContact(contact: EmergencyContact) {
        if (userId.isEmpty()) {
            return
        }

        db.collection("users").document(userId)
            .collection("emergencyContacts")
            .document(contact.id)
            .delete()
            .addOnSuccessListener {
                // Remove from local list
                contactsList.removeIf { it.id == contact.id }
                
                // If this was the primary contact and we have other contacts, make another one primary
                if (contact.isPrimary && contactsList.isNotEmpty()) {
                    val newPrimary = contactsList[0].copy(isPrimary = true)
                    saveContact(newPrimary)
                }
                
                updateEmptyStateVisibility()
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error deleting contact", e)
                Toast.makeText(this, "Failed to delete contact", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun setPrimaryContact(contact: EmergencyContact) {
        // First find and update the current primary
        val currentPrimary = contactsList.find { it.isPrimary }
        
        if (currentPrimary != null && currentPrimary.id != contact.id) {
            // Update the old primary
            saveContact(currentPrimary.copy(isPrimary = false))
        }
        
        // Make this contact primary
        saveContact(contact.copy(isPrimary = true))
        
        Toast.makeText(this, "${contact.name} set as primary emergency contact", Toast.LENGTH_SHORT).show()
    }
} 