package org.interns.project.users.model

import java.time.Instant
import java.time.LocalDate

data class User(
    val id: Long,
    val email: String,
    val login: String,
    val passwordHash: String,
    val role: String = "CLIENT",
    val firstName: String? = null,
    val lastName: String? = null,
    val name: String? = null,
    val surname: String? = null,
    val patronymic: String? = null,
    val phoneNumber: String? = null,
    val isActive: Boolean = true,
    val clinicId: Long? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val emailVerifiedAt: Instant? = null,
    val passwordChangedAt: Instant? = null,

    val dateOfBirth: LocalDate? = null,
    val avatar: String? = null,
    val gender: String? = null,

)