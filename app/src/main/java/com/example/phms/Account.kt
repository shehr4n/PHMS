package com.example.phms

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
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
    lateinit var logoutButton: TextView
    lateinit var backButton: ImageView

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
        logoutButton = findViewById(R.id.logout_button)
        backButton = findViewById(R.id.back_button)

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
            val dialogView = LayoutInflater.from(this).inflate(R.layout._edit_name, null)
            val newF = dialogView.findViewById<EditText>(R.id.new_f)
            val newL = dialogView.findViewById<EditText>(R.id.new_l)

            AlertDialog.Builder(this)
                .setTitle("Edit Name")
                .setView(dialogView)
                .setPositiveButton("Save") { dialog, _ ->
                    val newFName = newF.text.toString().trim()
                    val newLName = newL.text.toString().trim()
                    if (newFName.isNotEmpty()) {
                        updateNameInFirestore(newFName, newLName)
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
    private fun updateNameInFirestore(firstName: String, lastName: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val fullName = "$firstName $lastName"
            db.collection("users").document(userId)
                .update("name", fullName)
                .addOnSuccessListener {
                    nameText.text = fullName
                }
                .addOnFailureListener { e ->
                    // Handle error (like showing a Toast)
                }
        }
    }
}