package com.example.phms

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class Communication : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var doctorList: RecyclerView
    private lateinit var addDoctorButton: Button
    private lateinit var call911Button: Button
    private lateinit var adapter: DoctorAdapter
    private val doctors = mutableListOf<Doctor>()
    lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_communication)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        doctorList = findViewById(R.id.doctorList)
        addDoctorButton = findViewById(R.id.addDoctorButton)
        call911Button = findViewById(R.id.call911Button)

        adapter = DoctorAdapter(doctors,
            onCall = { callDoctor(it.phone) },
            onEmail = { emailDoctor(it.email) }
        )
        doctorList.layoutManager = LinearLayoutManager(this)
        doctorList.adapter = adapter

        addDoctorButton.setOnClickListener {
            showAddDoctorDialog()
        }

        call911Button.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:911")
            startActivity(intent)
        }

        loadDoctors()

        backButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }
    }

    private fun callDoctor(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phone")
        startActivity(intent)
    }

    private fun emailDoctor(email: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
        }
        startActivity(intent)
    }

    private fun loadDoctors() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("doctors")
            .get()
            .addOnSuccessListener { documents ->
                doctors.clear()
                for (doc in documents) {
                    val doctor = doc.toObject(Doctor::class.java)
                    doctors.add(doctor)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading doctors", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddDoctorDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_doctor, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.doctorName)
        val phoneInput = dialogView.findViewById<EditText>(R.id.doctorPhone)
        val emailInput = dialogView.findViewById<EditText>(R.id.doctorEmail)
        val addressInput = dialogView.findViewById<EditText>(R.id.doctorAddress)

        AlertDialog.Builder(this)
            .setTitle("Add Doctor")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val doctor = Doctor(
                    name = nameInput.text.toString(),
                    phone = phoneInput.text.toString(),
                    email = emailInput.text.toString(),
                    address = addressInput.text.toString()
                )
                saveDoctor(doctor)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveDoctor(doctor: Doctor) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("doctors")
            .document(doctor.id)
            .set(doctor)
            .addOnSuccessListener {
                loadDoctors()
                Toast.makeText(this, "Doctor added", Toast.LENGTH_SHORT).show()
            }
    }
}
