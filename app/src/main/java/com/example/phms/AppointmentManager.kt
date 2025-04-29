package com.example.phms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.phms.model.Appointment
import com.example.phms.model.AppointmentStatus
import java.util.*

class AppointmentManager(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun addAppointment(appointment: Appointment, onDone: () -> Unit = {}) {
        val userId = auth.currentUser?.uid ?: return
        val appointmentRef = db.collection("users")
            .document(userId)
            .collection("appointments")
            .document()

        val now = Date()
        val status = if (appointment.dateTime.after(now)) {
            AppointmentStatus.UPCOMING
        } else {
            AppointmentStatus.PAST
        }

        val appointmentWithId = appointment.copy(
            id = appointmentRef.id,
            status = status
        )

        appointmentRef.set(appointmentWithId)
            .addOnSuccessListener {
                scheduleReminder(appointmentWithId)
                updateAppointmentStatus()
                onDone()
            }
    }

    fun updateAppointment(appointment: Appointment) {
        val userId = auth.currentUser?.uid ?: return
        
        val now = Date()
        val status = if (appointment.dateTime.after(now)) {
            AppointmentStatus.UPCOMING
        } else {
            AppointmentStatus.PAST
        }

        val updatedAppointment = appointment.copy(status = status)

        db.collection("users")
            .document(userId)
            .collection("appointments")
            .document(appointment.id)
            .set(updatedAppointment)
            .addOnSuccessListener {
                scheduleReminder(updatedAppointment)
                updateAppointmentStatus()
            }
    }

    fun deleteAppointment(appointmentId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(userId)
            .collection("appointments")
            .document(appointmentId)
            .delete()
            .addOnSuccessListener {
                cancelReminder(appointmentId)
            }
    }

    private fun scheduleReminder(appointment: Appointment) {
        val reminderTime = Calendar.getInstance().apply {
            time = appointment.dateTime
            add(Calendar.HOUR, -1) // 1 hour before appointment
        }

        val intent = Intent(context, AppointmentReminderReceiver::class.java).apply {
            putExtra("APPOINTMENT_ID", appointment.id)
            putExtra("APPOINTMENT_TITLE", appointment.title)
            putExtra("DOCTOR_NAME", appointment.doctorName)
            putExtra("LOCATION", appointment.location)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointment.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime.timeInMillis,
            pendingIntent
        )
    }

    private fun cancelReminder(appointmentId: String) {
        val intent = Intent(context, AppointmentReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun updateAppointmentStatus() {
        val userId = auth.currentUser?.uid ?: return
        val now = Date()

        db.collection("users")
            .document(userId)
            .collection("appointments")
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val appointment = doc.toObject(Appointment::class.java)
                    val newStatus = when {
                        appointment.dateTime.before(now) -> AppointmentStatus.PAST
                        else -> AppointmentStatus.UPCOMING
                    }

                    if (appointment.status != newStatus) {
                        db.collection("users")
                            .document(userId)
                            .collection("appointments")
                            .document(appointment.id)
                            .update("status", newStatus)
                    }
                }
            }
    }

    fun getUpcomingAppointments(callback: (List<Appointment>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(userId)
            .collection("appointments")
            .whereEqualTo("status", AppointmentStatus.UPCOMING)
            .orderBy("dateTime")
            .get()
            .addOnSuccessListener { documents ->
                val appointments = documents.map { it.toObject(Appointment::class.java) }
                callback(appointments)
            }
    }

    fun getPastAppointments(callback: (List<Appointment>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(userId)
            .collection("appointments")
            .whereEqualTo("status", AppointmentStatus.PAST)
            .orderBy("dateTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val appointments = documents.map { it.toObject(Appointment::class.java) }
                callback(appointments)
            }
    }
} 