package org.interns.project.users.model

import java.time.Instant

data class User(
    val id: Long,
    val email: String,
    val login: String,
    val passwordHash: String,
    val role: String = "CLIENT",
    val firstName: String? = null,
    val lastName: String? = null,
    val patronymic: String? = null,
    val phoneNumber: String? = null,
    val isActive: Boolean = true,
    val clinicId: Long? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)