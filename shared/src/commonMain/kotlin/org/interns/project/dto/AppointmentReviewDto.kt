package org.interns.project.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppointmentReviewDto(
    @SerialName("id") val id: Long,
    @SerialName("appointment_id") val appointmentId: Long,
    @SerialName("doctor_id") val doctorId: Long,
    @SerialName("client_id") val clientId: Long,
    @SerialName("rating") val rating: Int,
    @SerialName("comment") val comment: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class AppointmentReviewRequest(
    @SerialName("rating") val rating: Int,
    @SerialName("comment") val comment: String? = null,
)

@Serializable
data class AppointmentWithReviewDto(
    @SerialName("appointment_id") val appointmentId: Long,
    @SerialName("status") val status: String,
    @SerialName("slot_start") val slotStart: String,
    @SerialName("slot_end") val slotEnd: String,
    @SerialName("doctor_id") val doctorId: Long,
    @SerialName("doctor_name") val doctorName: String? = null,
    @SerialName("doctor_profession") val doctorProfession: String? = null,
    @SerialName("review") val review: AppointmentReviewDto? = null,
)