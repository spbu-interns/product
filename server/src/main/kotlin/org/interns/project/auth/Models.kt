package org.interns.project.auth

data class LoginRequest(
    val login: String,
    val password: String
)

data class LoginSuccessResponse(
    val success: Boolean = true,
    val token: String,
    val role: String
)

data class LoginFailureResponse(
    val success: Boolean = false,
    val error: String
)

data class User(
    val id: Long,
    val login: String,
    val passwordHash: String,
    val role: String
)
