package com.example.phms

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class Registration : AppCompatActivity() {

    lateinit var  fnameInput : EditText
    lateinit var  lnameInput : EditText
    lateinit var  phoneInput : EditText
    lateinit var  emailInput : EditText
    lateinit var  passwordInput : EditText
    lateinit var  confirmPasswordInput : EditText
    lateinit var  registerButton: Button

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

        auth = FirebaseAuth.getInstance()

        registerButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        Log.d("Register", "User created: ${auth.currentUser?.email}")
                        val intent = Intent(this, HomePage::class.java)
                        startActivity(intent)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.e("Register", "Failed: ${task.exception?.message}")
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }
    }
}