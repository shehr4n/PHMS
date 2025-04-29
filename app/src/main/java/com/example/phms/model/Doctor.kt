package com.example.phms.model

data class Doctor(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val location: LocationInfo = LocationInfo()
) 