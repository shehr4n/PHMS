package com.example.phms

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Registration : AppCompatActivity() {

    lateinit var  fnameInput : EditText
    lateinit var  lnameInput : EditText
    lateinit var  phoneInput : EditText
    lateinit var  emailInput : EditText
    lateinit var  passwordInput : EditText
    lateinit var  confirmPasswordInput : EditText
    lateinit var  registerButton: Button
    lateinit var  backButton: TextView

    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.registration)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fnameInput = findViewById(R.id.first)
        lnameInput = findViewById(R.id.last)
        phoneInput = findViewById(R.id.phone)
        emailInput = findViewById(R.id.email)
        passwordInput = findViewById(R.id.password)
        confirmPasswordInput = findViewById(R.id.confirm_password)
        registerButton = findViewById(R.id.register_button)
        backButton = findViewById(R.id.back_button)

        auth = FirebaseAuth.getInstance()

        registerButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val password2 = confirmPasswordInput.text.toString()
            val name = "${fnameInput.text.toString()} ${lnameInput.text.toString()}"
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and password required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != password2) {
                Toast.makeText(this, "Password inputs do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val db = FirebaseFirestore.getInstance()

                        user?.let {
                            val uid = it.uid
                            val userProfile = hashMapOf(
                                "name" to name,
                                "phone" to phoneInput.text.toString(),
                                "email" to email,
                            )

                            db.collection("users")
                                .document(uid)
                                .set(userProfile)
                                .addOnSuccessListener {
                                    // Profile created successfully
                                    Log.d("Firestore", "User profile created successfully!")
                                    val intent = Intent(this, Registration2::class.java)
                                    startActivity(intent)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Error writing document", e)
                                    Toast.makeText(
                                        baseContext,
                                        "Error saving user profile: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    } else {
                        // Display message to user
                        Toast.makeText(
                            baseContext,
                            "Registration failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }

        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}