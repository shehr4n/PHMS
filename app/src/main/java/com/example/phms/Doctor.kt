package com.example.phms

import java.util.*

data class Doctor(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = ""
)
