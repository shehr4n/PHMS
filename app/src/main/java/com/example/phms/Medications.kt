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
import androidx.appcompat.app.AlertDialog
import android.widget.EditText

class Medications : AppCompatActivity() {

    lateinit var  medList: TextView
    lateinit var  addButton: Button

    lateinit var auth: FirebaseAuth
    var db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.medication)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        medList = findViewById(R.id.med_list)
        addButton = findViewById(R.id.add_med_button)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser

        addButton.setOnClickListener {
            val editText = EditText(this)
            editText.hint = "Enter medication name"

            AlertDialog.Builder(this)
                .setTitle("Add Medication")
                .setView(editText)
                .setPositiveButton("Add") { _, _ ->
                    val newMedication = editText.text.toString()
                    if (newMedication.isNotEmpty()) {
                        medList.append("\n$newMedication")
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}