package org.interns.project.dto

import kotlinx.serialization.Serializable

@Serializable
data class PatientDashboardDto(
    val profile: UserResponseDto,
    val clientInfo: ClientProfileDto?,
    val upcomingAppointmentsCount: Int,
    val medicalRecordsCount: Int,
    val nextAppointment: AppointmentDto?,
    val recentMedicalRecords: List<MedicalRecordOutDto>
)