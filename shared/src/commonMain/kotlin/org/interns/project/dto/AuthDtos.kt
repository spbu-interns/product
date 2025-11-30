package org.interns.project.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Authentication DTOs for sharing between client and server
 */

@Serializable
data class LoginRequest(
    @SerialName("email")
    val email: String,
    
    @SerialName("password")
    val password: String,
    
    @SerialName("account_type")
    val accountType: String = "CLIENT"
)

@Serializable
data class LoginResponse(
    @SerialName("token")
    val token: String?,
    
    @SerialName("user_id")
    val userId: Long,
    
    @SerialName("email")
    val email: String,
    
    @SerialName("account_type")
    val accountType: String,
    
    @SerialName("name")
    val firstName: String? = null,
    
    @SerialName("surname")
    val lastName: String? = null,

    @SerialName("requires_email_verification")
    val requiresEmailVerification: Boolean = false
)

@Serializable
data class RegisterRequest(
    @SerialName("email")
    val email: String,
    
    @SerialName("password")
    val password: String,
    
    @SerialName("account_type")
    val accountType: String = "CLIENT"
)

@Serializable
data class RegisterResponse(
    @SerialName("success")
    val success: Boolean,

    @SerialName("message")
    val message: String? = null,

    @SerialName("user_id")
    val userId: Long? = null,

    @SerialName("requires_email_verification")
    val requiresEmailVerification: Boolean = false
)

@Serializable
data class VerifyEmailRequest(
    @SerialName("token")
    val token: String
)

@Serializable
data class VerifyEmailResponse(
    @SerialName("success")
    val success: Boolean,
    
    @SerialName("message")
    val message: String? = null
)

@Serializable
data class RequestPasswordResetRequest(
    @SerialName("email")
    val email: String
)

@Serializable
data class RequestPasswordResetResponse(
    @SerialName("success")
    val success: Boolean,
    
    @SerialName("message")
    val message: String? = null
)

@Serializable
data class ResetPasswordRequest(
    @SerialName("token")
    val token: String,
    
    @SerialName("new_password")
    val newPassword: String
)

@Serializable
data class ResetPasswordResponse(
    @SerialName("success")
    val success: Boolean,
    
    @SerialName("message")
    val message: String? = null
)

@Serializable
data class ApiResponse<T>(
    @SerialName("success")
    val success: Boolean,

    @SerialName("data")
    val data: T? = null,
    
    @SerialName("error")
    val error: String? = null
)