package org.interns.project.users.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class UserInDto(
    val email: String,
    val login: String,
    val password: String,
    val role: String,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("clinic_id") val clinicId: Int? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class UserProfilePatch(
    // старые поля для совместимости
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val patronymic: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("clinic_id") val clinicId: Long? = null,

    // строкой в формате YYYY-MM-DD, fastapi сам превратит в date
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val avatar: String? = null,
    val gender: String? = null // MALE или FEMALE
)

@Serializable
data class UserOutDto(
    val id: Long,
    val email: String,
    val login: String,
    val role: String,

    // старые поля (совместимость)
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val patronymic: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("clinic_id") val clinicId: Long? = null,

    // новые поля профиля
    val name: String? = null,
    val surname: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null, // YYYY-MM-DD
    val avatar: String? = null,
    val gender: String? = null, // MALE/FEMALE

    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("email_verified_at") val emailVerifiedAt: String? = null,
    @SerialName("password_changed_at") val passwordChangedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class LoginRequest(
    @SerialName("login_or_email") val loginOrEmail: String,
    val password: String
)

@Serializable
data class ClientRegData(
    @SerialName("blood_type") val bloodType: String? = null,
    val height: Double? = null,
    val weight: Double? = null,
    @SerialName("emergency_contact_name") val emergencyContactName: String? = null,
    @SerialName("emergency_contact_number") val emergencyContactNumber: String? = null,
    val address: String? = null,
    val snils: String? = null,
    val passport: String? = null,
    @SerialName("dms_oms") val dmsOms: String? = null
)

@Serializable
data class ClientOut(
    val id: Long,
    @SerialName("user_id") val userId: Long,
    @SerialName("blood_type") val bloodType: String? = null,
    val height: Double? = null,
    val weight: Double? = null,
    @SerialName("emergency_contact_name") val emergencyContactName: String? = null,
    @SerialName("emergency_contact_number") val emergencyContactNumber: String? = null,
    val address: String? = null,
    val snils: String? = null,
    val passport: String? = null,
    @SerialName("dms_oms") val dmsOms: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class DoctorRegData(
    @SerialName("clinic_id") val clinicId: Long? = null,
    val profession: String,
    val info: String? = null,
    @SerialName("is_confirmed") val isConfirmed: Boolean? = null,
    val rating: Double? = null,
    val experience: Int? = null,
    val price: Double? = null
)

@Serializable
data class AdminRegData(
    @SerialName("clinic_id") val clinicId: Long? = null,
    val position: String? = null
)

@Serializable
data class RegistrationRequest(
    val username: String? = null,
    val password: String,
    val email: String,
    val role: String,
    @SerialName("is_active") val isActive: Boolean = true,
    val client: ClientRegData? = null,
    val doctor: DoctorRegData? = null,
    val admin: AdminRegData? = null
)

@Serializable
data class DoctorOut(
    val id: Long,
    @SerialName("user_id") val userId: Long,
    @SerialName("clinic_id") val clinicId: Long? = null,
    val profession: String,
    val info: String? = null,
    @SerialName("is_confirmed") val isConfirmed: Boolean? = null,
    val rating: Double? = null,
    val experience: Int? = null,
    val price: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class DoctorSearchFilter(
    @SerialName("specialization_ids") val specializationIds: List<Int>? = null,
    val city: String? = null,
    val region: String? = null,
    val metro: String? = null,
    @SerialName("online_only") val onlineOnly: Boolean = false,
    @SerialName("min_price") val minPrice: Double? = null,
    @SerialName("max_price") val maxPrice: Double? = null,
    @SerialName("min_rating") val minRating: Double? = null,
    val gender: String? = null,
    @SerialName("min_age") val minAge: Int? = null,
    @SerialName("max_age") val maxAge: Int? = null,
    @SerialName("min_experience") val minExperience: Int? = null,
    @SerialName("max_experience") val maxExperience: Int? = null,
    val date: String? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

@Serializable
data class DoctorSearchResult(
    val id: Long,
    @SerialName("user_id") val userId: Long,
    @SerialName("clinic_id") val clinicId: Long? = null,
    val profession: String,
    val info: String? = null,
    @SerialName("is_confirmed") val isConfirmed: Boolean? = null,
    val rating: Double? = null,
    val experience: Int? = null,
    val price: Double? = null,
    @SerialName("online_available") val onlineAvailable: Boolean,
    val gender: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val city: String? = null,
    val region: String? = null,
    val metro: String? = null,
    @SerialName("specialization_names") val specializationNames: List<String> = emptyList()
)

@Serializable
data class DoctorPatch(
    @SerialName("clinic_id") val clinicId: Long? = null,
    val profession: String? = null,
    val info: String? = null,
    @SerialName("is_confirmed") val isConfirmed: Boolean? = null,
    val rating: Double? = null,
    val experience: Int? = null,
    val price: Double? = null
)

@Serializable
data class ClientPatch(
    @SerialName("blood_type") val bloodType: String? = null,
    val height: Double? = null,
    val weight: Double? = null,
    @SerialName("emergency_contact_name") val emergencyContactName: String? = null,
    @SerialName("emergency_contact_number") val emergencyContactNumber: String? = null,
    val address: String? = null,
    val snils: String? = null,
    val passport: String? = null,
    @SerialName("dms_oms") val dmsOms: String? = null
)

// --- Appointment slots / appointments ---
@Serializable
data class SlotOutDto(
    val id: Long,
    @SerialName("doctor_id") val doctorId: Long,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val duration: Int,
    @SerialName("is_booked") val isBooked: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

data class Slot(
    val id: Long,
    val doctorId: Long,
    val startTime: Instant?,
    val endTime: Instant?,
    val durationMinutes: Int,
    val isBooked: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

@Serializable
data class AppointmentOutDto(
    val id: Long,
    @SerialName("slot_id") val slotId: Long,
    @SerialName("client_id") val clientId: Long,
    val status: String,
    val comments: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("canceled_at") val canceledAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("appointment_type_id") val appointmentTypeId: Long? = null
)

data class Appointment(
    val id: Long,
    val slotId: Long,
    val clientId: Long,
    val status: String,
    val comments: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val canceledAt: Instant?,
    val completedAt: Instant?,
    val appointmentTypeId: Long?
)