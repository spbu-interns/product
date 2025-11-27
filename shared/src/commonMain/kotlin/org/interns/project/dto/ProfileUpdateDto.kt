package org.interns.project.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileUpdateDto(
    @SerialName("name") val firstName: String? = null,
    @SerialName("surname") val lastName: String? = null,
    @SerialName("patronymic") val patronymic: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,

    @SerialName("clinic_id") val clinicId: Long? = null, // важно!

    @SerialName("date_of_birth") val dateOfBirth: String? = null, // YYYY-MM-DD
    val avatar: String? = null,
    val gender: String? = null // "MALE" / "FEMALE"
)

@Serializable
data class ProfileDto(
    val id: Long,

    @SerialName("name") val firstName: String? = null,
    @SerialName("surname") val lastName: String? = null,
    @SerialName("patronymic") val patronymic: String? = null,

    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val avatar: String? = null,
    val gender: String? = null,

    val email: String? = null,
    val login: String? = null,
    val role: String? = null,

    @SerialName("is_active") val isActive: Boolean = true
)
