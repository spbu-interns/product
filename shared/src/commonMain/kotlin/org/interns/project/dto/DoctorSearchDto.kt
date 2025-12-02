package org.interns.project.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DoctorSearchFilterDto(
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
data class DoctorSearchResultDto(
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
