package org.interns.project.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val login: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val success: Boolean = true,
    val token: String? = null,
    val role: String? = null,
    val error: String? = null
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    val is_active: Boolean = true
)

@Serializable
data class RegisterResponse(
    val success: Boolean = true,
    val id: Int? = null,
    val error: String? = null
)

@Serializable
data class ApiErrorResponse(
    val success: Boolean = false,
    val error: String
)