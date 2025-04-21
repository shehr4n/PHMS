package com.example.phms

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomePage : AppCompatActivity() {

    lateinit var  nameText: TextView
    lateinit var  vitalButton: TextView
    lateinit var  medButton: TextView
    lateinit var  dietButton: TextView

    lateinit var auth: FirebaseAuth
    var db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.home_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        nameText = findViewById(R.id.nameText)
        vitalButton = findViewById(R.id.vital_signs)
        medButton = findViewById(R.id.med)
        dietButton = findViewById(R.id.placeholder3)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: "User"
                        nameText.text = name
                    } else {
                        nameText.text = "User"
                    }
                }
                /* .addOnFailureListener { e ->
                    nameText.text = "Error"
                } */
        } /* else {
            nameText.text = "Guest"
        } */

        vitalButton.setOnClickListener {
            val intent = Intent(this, Vitals::class.java)
            startActivity(intent)
        }
        medButton.setOnClickListener {
            val intent = Intent(this, Medications::class.java)
            startActivity(intent)
        }
        dietButton.setOnClickListener {
            val intent = Intent(this, Diet::class.java)
            startActivity(intent)
        }
    }
}