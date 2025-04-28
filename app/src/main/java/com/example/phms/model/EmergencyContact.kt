package com.example.phms.model

data class EmergencyContact(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val relationship: String = "",
    val isPrimary: Boolean = false
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "phone" to phone,
            "email" to email,
            "relationship" to relationship,
            "isPrimary" to isPrimary
        )
    }
    
    companion object {
        fun fromMap(map: Map<String, Any?>): EmergencyContact {
            return EmergencyContact(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                phone = map["phone"] as? String ?: "",
                email = map["email"] as? String ?: "",
                relationship = map["relationship"] as? String ?: "",
                isPrimary = map["isPrimary"] as? Boolean ?: false
            )
        }
    }
} 