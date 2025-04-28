package com.example.phms

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.text.InputType
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
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

    lateinit var  nameText: TextView
    lateinit var  emailText: TextView
    lateinit var  phoneText: TextView
    lateinit var  passwordText: TextView
    lateinit var logoutButton: TextView
    lateinit var backButton: ImageView
    lateinit var emergencyContactButton: Button

    lateinit var auth: FirebaseAuth
    var db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.account)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        nameText = findViewById(R.id.nameText)
        emailText = findViewById(R.id.emailText)
        phoneText = findViewById(R.id.phoneText)
        passwordText = findViewById(R.id.passwordText)
        logoutButton = findViewById(R.id.logout_button)
        backButton = findViewById(R.id.back_button)
        emergencyContactButton = findViewById(R.id.emergencyContactButton)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val name = document.getString("name") ?: "User"
                    nameText.text = name
                    val email = document.getString("email") ?: "User"
                    emailText.text = email
                    val phone = document.getString("phone") ?: "User"
                    phoneText.text = phone
                }
        }

        backButton.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        nameText.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog2, null)
            val input1 = dialogView.findViewById<EditText>(R.id.input_1)
            val input2 = dialogView.findViewById<EditText>(R.id.input_2)
            input1.hint = "First Name"
            input2.hint = "Last Name"
            val nameParts = nameText.text.toString().trim().split(" ")
            input1.setText(nameParts[0])
            input2.setText(nameParts[1])

            AlertDialog.Builder(this)
                .setTitle("Edit Name")
                .setView(dialogView)
                .setPositiveButton("Save") { dialog, _ ->
                    val newFName = input1.text.toString().trim()
                    val newLName = input2.text.toString().trim()
                    val fullName = "$newFName $newLName"
                    if (newFName.isNotEmpty()) {
                        updateInFirestore(fullName, "name")
                        nameText.text = fullName
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        }

        emailText.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog1, null)
            val input = dialogView.findViewById<EditText>(R.id.input)
            input.hint = "Email"
            input.setText(emailText.text)
            AlertDialog.Builder(this)
                .setTitle("Edit Email")
                .setView(dialogView)
                .setPositiveButton("Save") { dialog, _ ->
                    val newEmail = input.text.toString().trim()
                    if (newEmail.isNotEmpty()) {
                        updateInFirestore(newEmail, "email")
                        emailText.text = newEmail
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        }

        phoneText.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog1, null)
            val input = dialogView.findViewById<EditText>(R.id.input)
            input.hint = "Phone Number"
            input.setText(phoneText.text)
            AlertDialog.Builder(this)
                .setTitle("Edit Phone Number")
                .setView(dialogView)
                .setPositiveButton("Save") { dialog, _ ->
                    val newPhone = input.text.toString().trim()
                    if (newPhone.isNotEmpty()) {
                        updateInFirestore(newPhone, "phone")
                        phoneText.text = newPhone
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        }

        passwordText.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog2, null)
            val input1 = dialogView.findViewById<EditText>(R.id.input_1)
            val input2 = dialogView.findViewById<EditText>(R.id.input_2)
            input1.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            input2.inputType = input1.inputType
            input1.hint = "New Password"
            input2.hint = "Confirm New Password"

            AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Save") { dialog, _ ->
                    val newPW = input1.text.toString().trim()
                    if (newPW == input2.text.toString().trim()) {
                        currentUser?.updatePassword(newPW)
                            ?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Password updated successfully
                                    Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Handle error (e.g., weak password)
                                    Toast.makeText(this, "Error updating password", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                    else {
                        Toast.makeText(this, "Password inputs do not match", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        }

        emergencyContactButton.setOnClickListener {
            val intent = Intent(this, EmergencyContactActivity::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateInFirestore(newStr: String, field: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId)
                .update(field, newStr)
                .addOnSuccessListener {
                    // Handle
                }
                .addOnFailureListener { e ->
                    // Handle error
                }
        }
    }
}