package com.example.phms.model

data class LocationInfo(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val timestamp: Long = System.currentTimeMillis()
) 