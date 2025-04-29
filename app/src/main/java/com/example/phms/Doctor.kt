package com.example.phms

import com.google.firebase.firestore.GeoPoint
import java.util.*

data class Doctor(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0)
)
