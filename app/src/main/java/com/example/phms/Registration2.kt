package com.example.phms

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Calendar

class Registration2 : AppCompatActivity() {

    lateinit var  genderRadioGroup : RadioGroup
    lateinit var  dobButton : EditText
    lateinit var  heightFtInput : EditText
    lateinit var  heightInInput : EditText
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

        genderRadioGroup = findViewById(R.id.gender_radio_group)
        dobButton = findViewById(R.id.DOB)
        heightFtInput = findViewById(R.id.height_ft)
        heightInInput = findViewById(R.id.height_in)
        weightInput = findViewById(R.id.weight)
        finishButton = findViewById(R.id.finish_button)
        var date = ""

        auth = FirebaseAuth.getInstance()

        dobButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    date = "${selectedMonth + 1}/$selectedDay/$selectedYear"
                    dobButton.setText(date)
                }, year, month, day)

            datePickerDialog.show()
        }

        finishButton.setOnClickListener {
            val db = FirebaseFirestore.getInstance()

            val selectedGenderId = genderRadioGroup.checkedRadioButtonId
            val dob = dobButton.text.toString()
            val heightFt = heightFtInput.text.toString()
            val heightIn = heightInInput.text.toString()
            val weight = weightInput.text.toString()

            if (selectedGenderId == -1 ||
                dob.isNullOrEmpty() ||
                heightFt.isNullOrEmpty() ||
                heightIn.isNullOrEmpty() ||
                weight.isNullOrEmpty()) {

                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val heightFtInt = heightFt.toInt()
            val heightInInt = heightIn.toInt()
            if (heightFtInt < 1 || heightInInt > 12) {
                Toast.makeText(this, "Invalid height input", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedGenderButton = findViewById<RadioButton>(selectedGenderId).text.toString()
            val user = hashMapOf(
                "gender" to selectedGenderButton,
                "dob" to dob,
                "height_ft" to heightFtInt,
                "height_in" to heightInInt,
                "weight" to weight.toInt()
            )
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            if (uid != null) {
                db.collection("users")
                    .document(uid)
                    .set(user, SetOptions.merge())
                    .addOnSuccessListener {
                        val intent = Intent(this, HomePage::class.java)
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error saving user: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}