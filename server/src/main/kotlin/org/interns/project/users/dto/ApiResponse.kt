package org.interns.project.users.dto

data class ApiResponse(
    val success: Boolean,
    val role: String? = null,
    val error: String? = null
)
