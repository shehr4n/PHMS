package com.example.phms

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Registration2 : AppCompatActivity() {

    lateinit var  gender : Spinner
    lateinit var  dobButton : EditText
    lateinit var  heightInput : EditText
    lateinit var  weightInput : EditText
    lateinit var  finishButton: Button

    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.registration2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        gender = findViewById(R.id.gender_spinner)
        dobButton = findViewById(R.id.DOB)
        heightInput = findViewById(R.id.height)
        weightInput = findViewById(R.id.weight)
        finishButton = findViewById(R.id.finish_button)

        auth = FirebaseAuth.getInstance()



        finishButton.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }
    }
}