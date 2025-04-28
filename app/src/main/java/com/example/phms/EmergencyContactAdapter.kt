package com.example.phms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.phms.model.EmergencyContact
import androidx.recyclerview.widget.RecyclerView

class EmergencyContactAdapter(
    private var contacts: List<EmergencyContact>,
    private val onEditClick: (EmergencyContact) -> Unit,
    private val onDeleteClick: (EmergencyContact) -> Unit,
    private val onSetPrimaryClick: (EmergencyContact) -> Unit
) : RecyclerView.Adapter<EmergencyContactAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.contactNameTextView)
        val phoneTextView: TextView = view.findViewById(R.id.contactPhoneTextView)
        val emailTextView: TextView = view.findViewById(R.id.contactEmailTextView)
        val relationshipTextView: TextView = view.findViewById(R.id.contactRelationshipTextView)
        val primaryIndicator: TextView = view.findViewById(R.id.primaryIndicator)
        val editButton: Button = view.findViewById(R.id.editContactButton)
        val deleteButton: Button = view.findViewById(R.id.deleteContactButton)
        val setPrimaryButton: Button = view.findViewById(R.id.setPrimaryButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.nameTextView.text = contact.name
        holder.phoneTextView.text = contact.phone
        holder.emailTextView.text = contact.email
        holder.relationshipTextView.text = contact.relationship
        
        if (contact.isPrimary) {
            holder.primaryIndicator.visibility = View.VISIBLE
            holder.setPrimaryButton.visibility = View.GONE
        } else {
            holder.primaryIndicator.visibility = View.GONE
            holder.setPrimaryButton.visibility = View.VISIBLE
        }
        
        holder.editButton.setOnClickListener { onEditClick(contact) }
        holder.deleteButton.setOnClickListener { onDeleteClick(contact) }
        holder.setPrimaryButton.setOnClickListener { onSetPrimaryClick(contact) }
    }

    override fun getItemCount() = contacts.size

    fun updateContacts(newContacts: List<EmergencyContact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }
}
