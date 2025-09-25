package org.interns.project.users.utils

object Validation {
    private val emailRegex = "^(?!.*\\.\\.)[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email)
}
