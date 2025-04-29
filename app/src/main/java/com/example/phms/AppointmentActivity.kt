package com.example.phms

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.phms.model.Appointment
import com.example.phms.model.Doctor
import com.example.phms.model.AppointmentStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.*

class AppointmentActivity : AppCompatActivity() {
    private val TAG = "AppointmentActivity"
    private lateinit var appointmentManager: AppointmentManager
    private lateinit var upcomingAppointmentsRecyclerView: RecyclerView
    private lateinit var pastAppointmentsRecyclerView: RecyclerView
    private lateinit var addAppointmentButton: Button
    private lateinit var upcomingAdapter: AppointmentAdapter
    private lateinit var pastAdapter: AppointmentAdapter
    private val upcomingAppointments = mutableListOf<Appointment>()
    private val pastAppointments = mutableListOf<Appointment>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointments)

        appointmentManager = AppointmentManager(this)
        upcomingAppointmentsRecyclerView = findViewById(R.id.upcomingAppointmentsRecyclerView)
        pastAppointmentsRecyclerView = findViewById(R.id.pastAppointmentsRecyclerView)
        addAppointmentButton = findViewById(R.id.addAppointmentButton)

        setupRecyclerViews()
        setupAddButton()

        observeAllAppointments()
    }

    private fun setupRecyclerViews() {
        upcomingAdapter = AppointmentAdapter(
            appointments = upcomingAppointments,
            onEdit = { showEditAppointmentDialog(it) },
            onDelete = { confirmDelete(it) }
        )
        pastAdapter = AppointmentAdapter(
            appointments = pastAppointments,
            onEdit = { showEditAppointmentDialog(it) },
            onDelete = { confirmDelete(it) }
        )

        upcomingAppointmentsRecyclerView.layoutManager = LinearLayoutManager(this)
        upcomingAppointmentsRecyclerView.adapter = upcomingAdapter

        pastAppointmentsRecyclerView.layoutManager = LinearLayoutManager(this)
        pastAppointmentsRecyclerView.adapter = pastAdapter
    }

    private fun setupAddButton() {
        addAppointmentButton.setOnClickListener {
            showAddAppointmentDialog()
        }
    }

    private fun observeAllAppointments() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        db.collection("users")
            .document(user.uid)
            .collection("appointments")
            .orderBy("dateTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e(TAG, "Listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener

                val list = snap.documents.mapNotNull { it.toObject(Appointment::class.java) }
                // Partition by status
                upcomingAppointments.clear()
                pastAppointments.clear()
                for (appt in list) {
                    if (appt.status == AppointmentStatus.UPCOMING) upcomingAppointments.add(appt)
                    else if (appt.status == AppointmentStatus.PAST) pastAppointments.add(appt)
                }
                upcomingAdapter.notifyDataSetChanged()
                pastAdapter.notifyDataSetChanged()
            }
    }

    private fun showAddAppointmentDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_appointment, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.appointmentTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.appointmentDescription)
        val selectDoctorButton = dialogView.findViewById<Button>(R.id.selectDoctorButton)
        val selectedDoctorText = dialogView.findViewById<TextView>(R.id.selectedDoctorText)
        val selectedDoctorAddress = dialogView.findViewById<TextView>(R.id.selectedDoctorAddress)
        val selectDateTimeButton = dialogView.findViewById<Button>(R.id.selectDateTimeButton)
        val selectedDateTimeText = dialogView.findViewById<TextView>(R.id.selectedDateTimeText)

        var selectedDoctor: Doctor? = null
        var selectedDateTime: Calendar = Calendar.getInstance()

        selectDoctorButton.setOnClickListener {
            showDoctorSelectionDialog { doctor ->
                selectedDoctor = doctor
                selectedDoctorText.text = "Selected: ${doctor.name}"
                selectedDoctorAddress.text = doctor.address
            }
        }

        selectDateTimeButton.setOnClickListener {
            showDateTimePicker { cal ->
                selectedDateTime = cal
                selectedDateTimeText.text = dateFormat.format(cal.time)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Add Appointment")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                if (selectedDoctor == null) {
                    Toast.makeText(this, "Please select a doctor", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val appt = Appointment(
                    title = titleInput.text.toString(),
                    description = descriptionInput.text.toString(),
                    dateTime = selectedDateTime.time,
                    doctorId = selectedDoctor!!.id,
                    doctorName = selectedDoctor!!.name,
                    doctorPhone = selectedDoctor!!.phone,
                    doctorEmail = selectedDoctor!!.email,
                    location = selectedDoctor!!.address,
                    status = AppointmentStatus.UPCOMING
                )
                appointmentManager.addAppointment(appt)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditAppointmentDialog(appointment: Appointment) {
        // unchanged: edits call appointmentManager.updateAppointment(...)
    }

    private fun confirmDelete(appointment: Appointment) {
        AlertDialog.Builder(this)
            .setTitle("Delete Appointment")
            .setMessage("Are you sure you want to delete this appointment?")
            .setPositiveButton("Delete") { _, _ ->
                appointmentManager.deleteAppointment(appointment.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDoctorSelectionDialog(onDoctorSelected: (Doctor) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(uid).collection("doctors")
            .get()
            .addOnSuccessListener { docs ->
                val doctors = docs.map { it.toObject(Doctor::class.java) }
                val names = doctors.map { it.name }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle("Select Doctor")
                    .setItems(names) { _, i -> onDoctorSelected(doctors[i]) }
                    .show()
            }
    }

    private fun showDateTimePicker(onPick: (Calendar) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, y, m, d ->
                cal.set(y, m, d)
                TimePickerDialog(
                    this,
                    { _, h, min ->
                        cal.set(Calendar.HOUR_OF_DAY, h)
                        cal.set(Calendar.MINUTE, min)
                        onPick(cal)
                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false
                ).show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    class AppointmentAdapter(
        private val appointments: List<Appointment>,
        private val onEdit: (Appointment) -> Unit,
        private val onDelete: (Appointment) -> Unit
    ) : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

        class AppointmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val titleText: TextView = view.findViewById(R.id.appointmentTitleText)
            val descText: TextView = view.findViewById(R.id.appointmentDescriptionText)
            val docNameText: TextView = view.findViewById(R.id.doctorNameText)
            val locText: TextView = view.findViewById(R.id.locationText)
            val dtText: TextView = view.findViewById(R.id.dateTimeText)
            val edit: ImageButton = view.findViewById(R.id.editButton)
            val del: ImageButton = view.findViewById(R.id.deleteButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_appointment, parent, false)
            return AppointmentViewHolder(v)
        }

        override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
            val appt = appointments[position]
            holder.titleText.text = appt.title
            holder.descText.text = appt.description
            holder.docNameText.text = "Doctor: ${appt.doctorName}"
            holder.locText.text = "Location: ${appt.location}"
            holder.dtText.text = dateFormat.format(appt.dateTime)
            holder.edit.setOnClickListener { onEdit(appt) }
            holder.del.setOnClickListener { onDelete(appt) }
        }

        override fun getItemCount() = appointments.size
    }
}
