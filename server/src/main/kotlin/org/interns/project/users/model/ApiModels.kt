package org.interns.project.users.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
data class UserOutDto(
    val id: Long? = null,
    val email: String,
    val login: String,
    val role: String,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("clinic_id") val clinicId: Long? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class LoginRequest(
    @SerialName("login_or_email") val loginOrEmail: String,
    val password: String
)