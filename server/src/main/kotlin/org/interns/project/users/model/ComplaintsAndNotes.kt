package org.interns.project.users.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ===== Complaints =====

@Serializable
enum class ComplaintStatus { OPEN, IN_PROGRESS, CLOSED }

@Serializable
data class ComplaintIn(
    val title: String,
    val body: String
)

@Serializable
data class ComplaintOut(
    val id: Long,
    @SerialName("patient_id") val patientId: Long,
    val title: String,
    val body: String,
    val status: ComplaintStatus,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ComplaintPatch(
    val title: String? = null,
    val body: String? = null,
    val status: ComplaintStatus? = null
)

// ===== Doctor Notes =====

@Serializable
enum class NoteVisibility { INTERNAL, PATIENT }

@Serializable
data class NoteIn(
    @SerialName("doctor_id") val doctorId: Long,
    val note: String,
    val visibility: NoteVisibility = NoteVisibility.INTERNAL
)

@Serializable
data class NoteOut(
    val id: Long,
    @SerialName("patient_id") val patientId: Long,
    @SerialName("doctor_id") val doctorId: Long,
    val note: String,
    val visibility: NoteVisibility,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class NotePatch(
    val note: String? = null,
    val visibility: NoteVisibility? = null
)
