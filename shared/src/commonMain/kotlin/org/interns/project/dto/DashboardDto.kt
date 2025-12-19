package org.interns.project.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppointmentDto(
    @SerialName("id") val id: Long,
    @SerialName("slot_id") val slotId: Long,
    @SerialName("client_id") val clientId: Long,
    @SerialName("status") val status: String,
    @SerialName("comments") val comments: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("canceled_at") val canceledAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("appointment_type_id") val appointmentTypeId: Long? = null,
    @SerialName("doctor_name") val doctorName: String? = null,
    @SerialName("doctor_specialization") val doctorSpecialization: String? = null,
)

@Serializable
data class MedicalRecordInDto(
    @SerialName("client_id") val clientId: Long,
    @SerialName("doctor_id") val doctorId: Long? = null,
    @SerialName("appointment_id") val appointmentId: Long? = null,
    @SerialName("diagnosis") val diagnosis: String? = null,
    @SerialName("symptoms") val symptoms: String? = null,
    @SerialName("treatment") val treatment: String? = null,
    @SerialName("recommendations") val recommendations: String? = null
)

@Serializable
data class MedicalRecordOutDto(
    @SerialName("id") val id: Long,
    @SerialName("client_id") val clientId: Long,
    @SerialName("doctor_id") val doctorId: Long? = null,
    @SerialName("appointment_id") val appointmentId: Long? = null,
    @SerialName("diagnosis") val diagnosis: String? = null,
    @SerialName("symptoms") val symptoms: String? = null,
    @SerialName("treatment") val treatment: String? = null,
    @SerialName("recommendations") val recommendations: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class DoctorPatientDto(
    @SerialName("client_id") val clientId: Long,
    @SerialName("user_id") val userId: Long,
    @SerialName("name") val name: String? = null,
    @SerialName("surname") val surname: String? = null,
    @SerialName("patronymic") val patronymic: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null, // yyyy-mm-dd
    @SerialName("avatar") val avatar: String? = null,
    @SerialName("gender") val gender: String? = null,
)

@Serializable
data class NextAppointmentDto(
    @SerialName("appointment_id") val appointmentId: Long,
    @SerialName("slot_start") val slotStart: String? = null,
    @SerialName("doctor_id") val doctorId: Long? = null,
    @SerialName("doctor_name") val doctorName: String? = null,
    @SerialName("doctor_surname") val doctorSurname: String? = null,
    @SerialName("doctor_patronymic") val doctorPatronymic: String? = null,
    @SerialName("doctor_profession") val doctorProfession: String? = null,
)

/**
 * общий ответ для /users/{id}/full:
 * для клиента — заполнены client, appointments, medicalRecords
 * для доктора — doctor, appointments, patients
 */
@Serializable
data class FullUserProfileDto(
    @SerialName("user") val user: UserResponseDto,
    @SerialName("client") val client: ClientProfileDto? = null,
    @SerialName("doctor") val doctor: DoctorProfileDto? = null,
    @SerialName("appointments") val appointments: List<AppointmentDto> = emptyList(),
    @SerialName("medical_records") val medicalRecords: List<MedicalRecordOutDto> = emptyList(),
    @SerialName("patients") val patients: List<DoctorPatientDto> = emptyList(),
    @SerialName("next_appointment") val nextAppointment: NextAppointmentDto? = null,
)
