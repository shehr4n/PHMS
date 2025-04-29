package com.example.phms.model

import java.util.Date

data class Appointment(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dateTime: Date = Date(),
    val doctorId: String = "",
    val doctorName: String = "",
    val doctorPhone: String = "",
    val doctorEmail: String = "",
    val location: String = "",
    val reminderSent: Boolean = false,
    val status: AppointmentStatus = AppointmentStatus.UPCOMING
)

enum class AppointmentStatus {
    UPCOMING,
    PAST,
    CANCELLED
} 