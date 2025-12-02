package org.interns.project.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DoctorDto(
    @SerialName("id") val id: Long,
    @SerialName("user_id") val userId: Long,
    @SerialName("clinic_id") val clinicId: Long? = null,
    @SerialName("profession") val profession: String,
    @SerialName("info") val info: String? = null,
    @SerialName("is_confirmed") val isConfirmed: Boolean? = null,
    @SerialName("rating") val rating: Double? = null,
    @SerialName("experience") val experience: Int? = null,
    @SerialName("price") val price: Double? = null,
    @SerialName("online_available") val onlineAvailable: Boolean? = null,
    @SerialName("specialization_ids") val specializationIds: List<Int>? = null,
)