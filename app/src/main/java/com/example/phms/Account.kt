package com.example.phms

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.text.InputType
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.*

class Account : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var titleText: TextView
    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var phoneText: TextView
    private lateinit var dobText: TextView
    private lateinit var passwordText: TextView
    private lateinit var emergencyContactButton: Button
    private lateinit var logoutButton: Button
    private lateinit var shareLocationButton: Button
    private lateinit var viewDoctorLocationButton: Button
    private lateinit var lastLocationText: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private var lastAddress: String? = null
    private var lastLocationTimestamp: Long? = null
    private val LOCATION_PERMISSION_REQUEST = 1001

    lateinit var auth: FirebaseAuth
    var db = FirebaseFirestore.getInstance()
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.account)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        backButton = findViewById(R.id.back_button)
        titleText = findViewById(R.id.titleText)
        nameText = findViewById(R.id.nameText)
        emailText = findViewById(R.id.emailText)
        phoneText = findViewById(R.id.phoneText)
        dobText = findViewById(R.id.dob)
        passwordText = findViewById(R.id.passwordText)
        emergencyContactButton = findViewById(R.id.emergencyContactButton)
        logoutButton = findViewById(R.id.logout_button)
        shareLocationButton = findViewById(R.id.shareLocationButton)
        viewDoctorLocationButton = findViewById(R.id.viewDoctorLocationButton)
        lastLocationText = findViewById(R.id.lastLocationText)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid ?: ""
        if (userId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            navigateToLogin()
            return
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            navigateToLogin()
        }
        emergencyContactButton.setOnClickListener {
            val intent = Intent(this, EmergencyContactActivity::class.java)
            startActivity(intent)
        }
        backButton.setOnClickListener {
            finish()
        }

        setupLocationButtons()
        loadUserData()
        loadLastLocationFromFirestore()
    }

    private fun loadUserData() {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fullName = document.getString("name") ?: "User"
                    nameText.text = fullName
                    emailText.text = document.getString("email") ?: "Not provided"
                    phoneText.text = document.getString("phone") ?: "Not provided"
                    dobText.text = document.getString("dob") ?: "Not provided"
                    passwordText.text = "*************" // Never show real password
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun updateInFirestore(newStr: String, field: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId)
                .update(field, newStr)
                .addOnSuccessListener {
                    // Handle
                }
                .addOnFailureListener { e ->
                    // Handle error
                }
        }
    }

    private fun setupLocationButtons() {
        shareLocationButton.setOnClickListener {
            checkLocationPermissionAndFetchLocation { location, address ->
                shareLocation(location, address)
            }
        }
        viewDoctorLocationButton.setOnClickListener {
            viewDoctorLocation()
        }
    }

    private fun checkLocationPermissionAndFetchLocation(onLocationReady: (Location, String) -> Unit) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val address = addresses?.firstOrNull()?.getAddressLine(0) ?: "Lat: ${location.latitude}, Lng: ${location.longitude}"
                lastLocation = location
                lastAddress = address
                lastLocationTimestamp = System.currentTimeMillis()
                updateLastLocationUI(location, address, lastLocationTimestamp!!)
                saveLastLocationToFirestore(location, address, lastLocationTimestamp!!)
                onLocationReady(location, address)
            } else {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Try again if permission granted
                checkLocationPermissionAndFetchLocation { _, _ -> }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareLocation(location: Location, address: String) {
        val uri = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        val shareText = "My current location: $address\n$uri"
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(intent, "Share Location"))
    }

    private fun viewDoctorLocation() {
        // For demo: hardcoded doctor location. In production, fetch from Firestore or user profile.
        val doctorLat = 23.8103
        val doctorLng = 90.4125
        val doctorLabel = "Doctor's Office"
        val gmmIntentUri = Uri.parse("geo:$doctorLat,$doctorLng?q=$doctorLat,$doctorLng($doctorLabel)")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(this, "Google Maps not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLastLocationUI(location: Location, address: String, timestamp: Long) {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val timeString = sdf.format(Date(timestamp))
        lastLocationText.text = "Last known location: $address\n($timeString)"
    }

    private fun saveLastLocationToFirestore(location: Location, address: String, timestamp: Long) {
        val locationData = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "address" to address,
            "timestamp" to timestamp
        )
        db.collection("users").document(userId).update("lastLocation", locationData)
    }

    private fun loadLastLocationFromFirestore() {
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            val loc = document.get("lastLocation") as? Map<*, *>
            if (loc != null) {
                val address = loc["address"] as? String ?: "Unknown"
                val timestamp = (loc["timestamp"] as? Number)?.toLong() ?: 0L
                lastLocationText.text = "Last known location: $address\n(${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))})"
            }
        }
    }
}