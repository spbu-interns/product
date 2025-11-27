package org.interns.project.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileUpdateDto(
    // Общие поля пользователя
    @SerialName("name") val firstName: String? = null,
    @SerialName("surname") val lastName: String? = null,
    @SerialName("patronymic") val patronymic: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("clinic_id") val clinicId: Long? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null, // YYYY-MM-DD
    val avatar: String? = null,
    val gender: String? = null,
    val email: String? = null,
    val login: String? = null,

    // Поля пациента
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

    // Поля доктора
    @SerialName("profession")
    val profession: String?,

    @SerialName("info")
    val info: String? = null,

    @SerialName("experience")
    val experience: Int? = null,

    @SerialName("price")
    val price: Double? = null
)