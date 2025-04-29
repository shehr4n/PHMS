package com.example.phms

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomePage : AppCompatActivity() {

    lateinit var  nameText: TextView
    lateinit var accountButton: ImageView

    lateinit var  vitalButton: TextView
    lateinit var  medButton: TextView
    lateinit var  notesButton: TextView
    lateinit var  dietButton: TextView
    lateinit var  searchButton: TextView
    lateinit var communicationButton: TextView



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
        accountButton = findViewById(R.id.account_button)

        vitalButton = findViewById(R.id.vital_signs)
        medButton = findViewById(R.id.med)
        notesButton = findViewById(R.id.notes)
        dietButton = findViewById(R.id.placeholder3)
        searchButton = findViewById(R.id.placeholder5)
        communicationButton = findViewById(R.id.communicationTextView)


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

        accountButton.setOnClickListener {
            val intent = Intent(this, Account::class.java)
            startActivity(intent)
        }

        vitalButton.setOnClickListener {
            val intent = Intent(this, Vitals::class.java)
            startActivity(intent)
        }
        medButton.setOnClickListener {
            val intent = Intent(this, Medications::class.java)
            startActivity(intent)
        }

        notesButton.setOnClickListener {
            val intent = Intent(this, Notes::class.java)
            startActivity(intent)
        }

        dietButton.setOnClickListener {
            val intent = Intent(this, Diet::class.java)
            startActivity(intent)
        }

        searchButton.setOnClickListener {
            val intent = Intent(this, Search::class.java)
            startActivity(intent)
        }
        communicationButton.setOnClickListener {
            val intent = Intent(this, Communication::class.java)
            startActivity(intent)
        }

    }
}