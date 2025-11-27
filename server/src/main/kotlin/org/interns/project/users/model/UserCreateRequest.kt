package org.interns.project.users.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserCreateRequest(
    val email: String,
    val login: String,
    val password: String,
    val role: String,
    @SerialName("username") val username: String,
    @SerialName("name") val firstName: String? = null,
    @SerialName("surname")  val lastName:  String? = null,
    @SerialName("clinic_id")  val clinicId:  Int? = null,
    @SerialName("is_active")  val isActive:  Boolean = true
)
