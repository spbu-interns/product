package org.interns.project.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class NoteVisibilityDto {
    @SerialName("INTERNAL")
    INTERNAL,

    @SerialName("PATIENT")
    PATIENT
}

@Serializable
data class DoctorNoteCreateRequest(
    @SerialName("doctor_id")
    val doctorId: Long,

    @SerialName("note")
    val note: String,

    @SerialName("visibility")
    val visibility: NoteVisibilityDto = NoteVisibilityDto.INTERNAL
)

@Serializable
data class DoctorNoteResponse(
    @SerialName("id")
    val id: Long,

    @SerialName("patient_id")
    val patientId: Long,

    @SerialName("doctor_id")
    val doctorId: Long,

    @SerialName("note")
    val note: String,

    @SerialName("visibility")
    val visibility: NoteVisibilityDto,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class DoctorNotePatchRequest(
    @SerialName("note")
    val note: String? = null,

    @SerialName("visibility")
    val visibility: NoteVisibilityDto? = null
)
