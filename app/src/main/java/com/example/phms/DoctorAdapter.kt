package com.example.phms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DoctorAdapter(
    private var doctors: List<Doctor>,
    private val onCall: (Doctor) -> Unit,
    private val onEmail: (Doctor) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

    class DoctorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.doctorNameText)
        val phoneText: TextView = view.findViewById(R.id.doctorPhoneText)
        val emailText: TextView = view.findViewById(R.id.doctorEmailText)
        val addressText: TextView = view.findViewById(R.id.doctorAddressText)
        val callBtn: ImageButton = view.findViewById(R.id.callButton)
        val emailBtn: ImageButton = view.findViewById(R.id.emailButton)
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

        holder.callBtn.setOnClickListener { onCall(doctor) }
        holder.emailBtn.setOnClickListener { onEmail(doctor) }
    }

    override fun getItemCount() = doctors.size
}
