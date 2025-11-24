package org.interns.project.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ComplaintCreateRequest(
    @SerialName("title")
    val title: String,

    @SerialName("body")
    val body: String
)

@Serializable
data class ComplaintResponse(
    @SerialName("id")
    val id: Long,

    @SerialName("patient_id")
    val patientId: Long,

    @SerialName("title")
    val title: String,

    @SerialName("body")
    val body: String,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class ComplaintPatchRequest(
    @SerialName("title")
    val title: String? = null,

    @SerialName("body")
    val body: String? = null
)

@Serializable
data class SlotCreateRequest(
    @SerialName("start_time")
    val startTime: String,

    @SerialName("end_time")
    val endTime: String
)

@Serializable
data class SlotResponse(
    @SerialName("id") val id: Long,
    @SerialName("doctor_id") val doctorId: Long,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("duration") val duration: Int,
    @SerialName("is_booked") val isBooked: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class AppointmentCreateRequest(
    @SerialName("slot_id") val slotId: Long,
    @SerialName("client_id") val clientId: Long,
    @SerialName("comments") val comments: String? = null,
    @SerialName("appointment_type_id") val appointmentTypeId: Long? = null
)

@Serializable
data class AppointmentResponse(
    @SerialName("id") val id: Long,
    @SerialName("slot_id") val slotId: Long,
    @SerialName("client_id") val clientId: Long,
    @SerialName("status") val status: String,
    @SerialName("comments") val comments: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("canceled_at") val canceledAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("appointment_type_id") val appointmentTypeId: Long? = null
)