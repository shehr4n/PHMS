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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomePage : AppCompatActivity() {

    lateinit var  nameText: TextView
    lateinit var ageText: TextView
    lateinit var genderText: TextView
    lateinit var accountButton: ImageView

    lateinit var  vitalButton: TextView
    lateinit var  medButton: TextView
    lateinit var  notesButton: TextView
    lateinit var  dietButton: TextView
    lateinit var  searchButton: TextView

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
        ageText = findViewById(R.id.ageText)
        genderText = findViewById(R.id.genderText)
        accountButton = findViewById(R.id.account_button)

        vitalButton = findViewById(R.id.vital_signs)
        medButton = findViewById(R.id.med)
        notesButton = findViewById(R.id.notes)
        dietButton = findViewById(R.id.placeholder3)
        searchButton = findViewById(R.id.placeholder5)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: "User"
                        val dob = document.getString("dob") ?: "1/1/2000"
                        val gender = document.getString("gender") ?: "O"
                        nameText.text = name
                        ageText.text = calculateAge(dob).toString()
                        genderText.text = gender[0].toString()
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
    }

    fun calculateAge(dob: String): Int {
        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        val birthDate = sdf.parse(dob) ?: return 0
        val today = Calendar.getInstance()

        val birth = Calendar.getInstance()
        birth.time = birthDate

        var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)

        if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        return age
    }
}