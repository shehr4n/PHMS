package com.example.phms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.phms.model.Doctor
import com.example.phms.service.LocationService
import java.util.*

class Communication : AppCompatActivity(){

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var doctorList: RecyclerView
    private lateinit var addDoctorButton: Button
    private lateinit var call911Button: Button
    private lateinit var shareLocationButton: Button
    private lateinit var adapter: DoctorAdapter
    private lateinit var map: GoogleMap
    private val doctors = mutableListOf<Doctor>()
    lateinit var backButton: ImageView
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_communication)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        doctorList = findViewById(R.id.doctorList)
        addDoctorButton = findViewById(R.id.addDoctorButton)
        call911Button = findViewById(R.id.call911Button)
        shareLocationButton = findViewById(R.id.shareLocationButton)

        // Setup map
        //val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        //mapFragment.getMapAsync(this)

        adapter = DoctorAdapter(doctors,
            onCall = { callDoctor(it.phone) },
            onEmail = { emailDoctor(it.email) },
            onLocation = { showDoctorLocation(it) }
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

        shareLocationButton.setOnClickListener {
            checkLocationPermission()
        }

        loadDoctors()

        backButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationService()
        }
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        startService(serviceIntent)
        Toast.makeText(this, "Location sharing started", Toast.LENGTH_SHORT).show()
    }

    private fun showDoctorLocation(doctor: Doctor) {
        val uri = Uri.parse("geo:0,0?q=" + Uri.encode(doctor.address))
        Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }.let { intent ->
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No map app found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /*
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        }
    }*/
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
/*private fun showAddDoctorDialog() {
    val dialogView = LayoutInflater.from(this)
        .inflate(R.layout.dialog_add_doctor, null)
    val nameInput    = dialogView.findViewById<EditText>(R.id.doctorName)
    val phoneInput   = dialogView.findViewById<EditText>(R.id.doctorPhone)
    val emailInput   = dialogView.findViewById<EditText>(R.id.doctorEmail)
    val addressInput = dialogView.findViewById<EditText>(R.id.doctorAddress)

    AlertDialog.Builder(this)
        .setTitle("Add Doctor")
        .setView(dialogView)
        .setPositiveButton("Save") { _, _ ->
            try {
                // Validate
                if (nameInput.text.isNullOrBlank()) {
                    nameInput.error = "Name required"
                    return@setPositiveButton
                }
                // Build your Doctor
                val doctor = Doctor(
                    name    = nameInput.text.toString().trim(),
                    phone   = phoneInput.text.toString().trim(),
                    email   = emailInput.text.toString().trim(),
                    address = addressInput.text.toString().trim()
                )
                // This is the only place we call saveDoctor
                saveDoctor(doctor)
            } catch (e: Exception) {
                // Catch ANY exception here and show it
                Log.e("Communication", "Error in Save button", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        .setNegativeButton("Cancel", null)
        .show()
}*/

    private fun saveDoctor(doctor: Doctor) {
        val user = auth.currentUser ?: return
        val docRef = db.collection("users")
            .document(user.uid)
            .collection("doctors")
            .document() // generate a new document reference with a unique ID

        val doctorWithId = doctor.copy(id = docRef.id)

        docRef.set(doctorWithId)
            .addOnSuccessListener {
                loadDoctors()
                Toast.makeText(this, "Doctor added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add doctor: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("Communication", "saveDoctor failed", e)
            }
    }
}
