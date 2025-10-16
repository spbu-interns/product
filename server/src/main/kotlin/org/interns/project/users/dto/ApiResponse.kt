package org.interns.project.users.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
    val success: Boolean,
    val role: String? = null,
    val error: String? = null,
    val token: String? = null // на будущее, а пока что не реализована генерация токена при входе
)
