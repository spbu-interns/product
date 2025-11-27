package org.interns.project.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserResponseDto(
    @SerialName("id")
    val id: Long,

    @SerialName("email")
    val email: String,

    @SerialName("login")
    val login: String,

    @SerialName("role")
    val role: String,

    @SerialName("patronymic")
    val patronymic: String? = null,

    @SerialName("phone_number")
    val phoneNumber: String? = null,

    @SerialName("clinic_id")
    val clinicId: Long? = null,

    @SerialName("name")
    val name: String? = null,

    @SerialName("surname")
    val surname: String? = null,

    @SerialName("date_of_birth")
    val dateOfBirth: String? = null,

    @SerialName("avatar")
    val avatar: String? = null,

    @SerialName("gender")
    val gender: String? = null,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("email_verified_at")
    val emailVerifiedAt: String? = null,

    @SerialName("password_changed_at")
    val passwordChangedAt: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class ClientProfileDto(
    @SerialName("id")
    val id: Long,

    @SerialName("user_id")
    val userId: Long,

    @SerialName("blood_type")
    val bloodType: String? = null,

    @SerialName("height")
    val height: Double? = null,

    @SerialName("weight")
    val weight: Double? = null,

    @SerialName("emergency_contact_name")
    val emergencyContactName: String? = null,

    @SerialName("emergency_contact_number")
    val emergencyContactNumber: String? = null,

    @SerialName("address")
    val address: String? = null,

    @SerialName("snils")
    val snils: String? = null,

    @SerialName("passport")
    val passport: String? = null,

    @SerialName("dms_oms")
    val dmsOms: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class DoctorProfileDto(
    @SerialName("id")
    val id: Long,

    @SerialName("user_id")
    val userId: Long,

    @SerialName("clinic_id")
    val clinicId: Long? = null,

    @SerialName("profession")
    val profession: String,

    @SerialName("info")
    val info: String? = null,

    @SerialName("is_confirmed")
    val isConfirmed: Boolean? = null,

    @SerialName("rating")
    val rating: Double? = null,

    @SerialName("experience")
    val experience: Int? = null,

    @SerialName("price")
    val price: Double? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,
)
