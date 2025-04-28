package com.example.phms

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Account : AppCompatActivity() {

    // UI Components
    private lateinit var nameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var firstNameText: TextView
    private lateinit var lastNameText: TextView
    private lateinit var phoneText: TextView
    private lateinit var dobText: TextView
    private lateinit var editProfileButton: Button
    private lateinit var logoutButton: Button
    private lateinit var emergencyContactButton: Button

    // Firebase
    private lateinit var auth: FirebaseAuth
    private var db = FirebaseFirestore.getInstance()
    private var userId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.account)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid ?: ""
        
        if (userId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            navigateToLogin()
            return
        }

        // Initialize UI components
        initializeViews()
        
        // Set up button click listeners
        setupClickListeners()
        
        // Load user data
        loadUserData()
    }
    
    private fun initializeViews() {
        nameText = findViewById(R.id.nameText)
        userEmailText = findViewById(R.id.userEmailText)
        firstNameText = findViewById(R.id.firstNameText)
        lastNameText = findViewById(R.id.lastNameText)
        phoneText = findViewById(R.id.phoneText)
        dobText = findViewById(R.id.dobText)
        editProfileButton = findViewById(R.id.editProfileButton)
        logoutButton = findViewById(R.id.logoutButton)
        emergencyContactButton = findViewById(R.id.emergencyContactButton)
        
        // Display user email
        userEmailText.text = auth.currentUser?.email ?: "Not signed in"
    }
    
    private fun setupClickListeners() {
        logoutButton.setOnClickListener {
            auth.signOut()
            navigateToLogin()
        }

        emergencyContactButton.setOnClickListener {
            val intent = Intent(this, EmergencyContactActivity::class.java)
            startActivity(intent)
        }
        
        editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }
    }
    
    private fun loadUserData() {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Get full name from name field if it exists
                    val fullName = document.getString("name") ?: "User"
                    nameText.text = fullName
                    
                    // Try to get firstName and lastName directly first
                    var firstName = document.getString("firstName") ?: ""
                    var lastName = document.getString("lastName") ?: ""
                    
                    // If firstName or lastName is missing, try to extract from full name
                    if (firstName.isEmpty() || lastName.isEmpty()) {
                        val nameParts = fullName.split(" ")
                        if (nameParts.size >= 2) {
                            // If name has at least two parts, use first as firstName and rest as lastName
                            firstName = nameParts[0]
                            lastName = nameParts.subList(1, nameParts.size).joinToString(" ")
                        } else if (nameParts.size == 1) {
                            // If name has only one part, use it as firstName
                            firstName = nameParts[0]
                        }
                        
                        // Save these extracted names back to Firestore for future use
                        if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                            val updates = hashMapOf<String, Any>()
                            if (firstName.isNotEmpty()) updates["firstName"] = firstName
                            if (lastName.isNotEmpty()) updates["lastName"] = lastName
                            
                            if (updates.isNotEmpty()) {
                                db.collection("users").document(userId).update(updates)
                                    .addOnFailureListener { e ->
                                        Log.e("Account", "Failed to update firstName/lastName: ${e.message}")
                                    }
                            }
                        }
                    }
                    
                    // Display values in UI
                    firstNameText.text = firstName
                    lastNameText.text = lastName
                    phoneText.text = document.getString("phone") ?: "Not provided"
                    dobText.text = document.getString("dob") ?: "Not provided"
                    
                    // Log for debugging
                    Log.d("Account", "Loaded data: firstName=$firstName, lastName=$lastName")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Account", "Error loading user data", e)
            }
    }
    
    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        
        val firstNameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.firstNameEditText)
        val lastNameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.lastNameEditText)
        val phoneInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.phoneEditText)
        val dobInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dobEditText)
        
        // Pre-populate with existing data
        firstNameInput.setText(firstNameText.text)
        lastNameInput.setText(lastNameText.text)
        phoneInput.setText(phoneText.text.toString().takeIf { it != "Not provided" } ?: "")
        dobInput.setText(dobText.text.toString().takeIf { it != "Not provided" } ?: "")
        
        // Create dialog with custom view
        val dialog = AlertDialog.Builder(this)
            .setTitle("Update Profile")
            .setView(dialogView)
            .setCancelable(true)
            .create()
            
        // Set up the Cancel button click listener
        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }
            
        // Set up the Update button click listener
        dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val updatedFirstName = firstNameInput.text.toString().trim()
            val updatedLastName = lastNameInput.text.toString().trim()
            val updatedPhone = phoneInput.text.toString().trim()
            val updatedDob = dobInput.text.toString().trim()
            
            if (updatedFirstName.isEmpty() || updatedLastName.isEmpty()) {
                Toast.makeText(this, "First name and last name are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            updateUserProfile(updatedFirstName, updatedLastName, updatedPhone, updatedDob)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun updateUserProfile(firstName: String, lastName: String, phone: String, dob: String) {
        val userUpdates = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "phone" to phone,
            "dob" to dob
        )
        
        db.collection("users").document(userId)
            .update(userUpdates as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                loadUserData() // Reload the UI with updated data
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}