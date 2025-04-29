package com.example.phms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.phms.model.Doctor

class DoctorAdapter(
    private val doctors: List<Doctor>,
    private val onCall: (Doctor) -> Unit,
    private val onEmail: (Doctor) -> Unit,
    private val onLocation: (Doctor) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

    class DoctorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.doctorNameText)
        val phoneText: TextView = view.findViewById(R.id.doctorPhoneText)
        val emailText: TextView = view.findViewById(R.id.doctorEmailText)
        val addressText: TextView = view.findViewById(R.id.doctorAddressText)
        val callButton: ImageButton = view.findViewById(R.id.callButton)
        val emailButton: ImageButton = view.findViewById(R.id.emailButton)
        val locationButton: ImageButton = view.findViewById(R.id.locationButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor, parent, false)
        return DoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        val doctor = doctors[position]
        holder.nameText.text = doctor.name
        holder.phoneText.text = "Phone: ${doctor.phone}"
        holder.emailText.text = "Email: ${doctor.email}"
        holder.addressText.text = "Address: ${doctor.address}"

        holder.callButton.setOnClickListener { onCall(doctor) }
        holder.emailButton.setOnClickListener { onEmail(doctor) }
        holder.locationButton.setOnClickListener { onLocation(doctor) }
    }

    override fun getItemCount() = doctors.size
}
