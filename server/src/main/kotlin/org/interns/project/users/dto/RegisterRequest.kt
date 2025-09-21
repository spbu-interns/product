package org.interns.project.users.dto

data class RegisterRequest(
    val email: String,
    val login: String,
    val password: String,
    val role: String? = "CLIENT",
    val firstName: String? = null,
    val lastName: String? = null,
    val patronymic: String? = null,
    val phoneNumber: String? = null
)
