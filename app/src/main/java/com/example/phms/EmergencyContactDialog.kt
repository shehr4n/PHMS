package com.example.phms

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.TextView
import com.example.phms.model.EmergencyContact
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class EmergencyContactDialog(
    context: Context,
    private val contactId: String? = null,
    private val userId: String,
    private val onContactSaved: (EmergencyContact) -> Unit
) : Dialog(context) {

    private lateinit var nameEditText: TextInputEditText
    private lateinit var relationshipEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var dialogTitle: TextView
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_add_emergency_contact)

        initializeViews()
        setupButtons()

        // If contactId is provided, we're editing an existing contact
        contactId?.let { loadContactData(it) }
    }

    private fun initializeViews() {
        dialogTitle = findViewById(R.id.dialogTitle)
        nameEditText = findViewById(R.id.contactNameEditText)
        relationshipEditText = findViewById(R.id.contactRelationshipEditText)
        phoneEditText = findViewById(R.id.contactPhoneEditText)
        emailEditText = findViewById(R.id.contactEmailEditText)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)

        // Set title based on whether we're adding or editing
        dialogTitle.text = if (contactId == null) "Add Emergency Contact" else "Edit Emergency Contact"
    }

    private fun setupButtons() {
        cancelButton.setOnClickListener {
            dismiss()
        }

        saveButton.setOnClickListener {
            if (validateInputs()) {
                saveContact()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val name = nameEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()

        if (name.isEmpty()) {
            nameEditText.error = "Name is required"
            isValid = false
        }

        if (phone.isEmpty()) {
            phoneEditText.error = "Phone number is required"
            isValid = false
        }

        return isValid
    }

    private fun saveContact() {
        val name         = nameEditText.text.toString().trim()
        val relationship = relationshipEditText.text.toString().trim()
        val phone        = phoneEditText.text.toString().trim()
        val email        = emailEditText.text.toString().trim()
        val contact = EmergencyContact(
            id           = contactId ?: db.collection("users")
                .document(userId)
                .collection("emergencyContacts")
                .document().id,
            name         = name,
            phone        = phone,
            email        = email,
            relationship = relationship,
            isPrimary    = false
        )
        onContactSaved(contact)
        dismiss()
    }

    private fun loadContactData(contactId: String) {
        db.collection("users").document(userId)
            .collection("emergencyContacts").document(contactId)
            .get()
            .addOnSuccessListener { doc ->
                val c = doc.toObject(EmergencyContact::class.java)
                c?.let {
                    nameEditText.setText(it.name)
                    relationshipEditText.setText(it.relationship)
                    phoneEditText.setText(it.phone)    // ← use it.phone
                    emailEditText.setText(it.email)
                }
            }
    }

} 