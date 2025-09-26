package org.interns.project.users.service

import io.ktor.http.HttpStatusCode
import org.interns.project.users.dto.RegisterRequest
import org.interns.project.users.dto.ApiResponse
import org.interns.project.users.model.User
import org.interns.project.users.repo.InMemoryUserRepo
import org.interns.project.users.utils.Validation
import at.favre.lib.crypto.bcrypt.BCrypt
import org.interns.project.config.SecurityConfig
import org.interns.project.users.repo.UserRepo
import java.time.Instant

class UserService(private val repo: UserRepo = InMemoryUserRepo())  {

    fun registerAndReturn(req: RegisterRequest): User {
        if (!Validation.isValidEmail(req.email)) {
            throw IllegalArgumentException("Неверный формат email")
        }

        if (req.login.isBlank() || req.password.isBlank()) {
            throw IllegalArgumentException("login and password must not be empty")
        }

        val role = req.role ?: "CLIENT"
        if (role !in listOf("CLIENT", "DOCTOR", "ADMIN")) {
            throw IllegalArgumentException("invalid role")
        }

        if (repo.findByEmail(req.email) != null) {
            throw IllegalStateException("email already exists")
        }
        if (repo.findByLogin(req.login) != null) {
            throw IllegalStateException("login already exists")
        }

        val bcryptHash = BCrypt.withDefaults()
            .hashToString(SecurityConfig.bcryptCost, req.password.toCharArray())

        val now = Instant.now()
        val user = User(
            id = 0L,
            email = req.email,
            login = req.login,
            passwordHash = bcryptHash,
            role = role,
            firstName = req.firstName,
            lastName = req.lastName,
            patronymic = req.patronymic,
            phoneNumber = req.phoneNumber,
            isActive = true,
            clinicId = null,
            createdAt = now,
            updatedAt = now
        )

        return repo.save(user)
    }

    fun login(login: String, password: String): Pair<HttpStatusCode, ApiResponse> {
        val user = repo.findByLogin(login)
            ?: return HttpStatusCode.Unauthorized to ApiResponse(success = false, error = "Неверный логин или пароль")

        val result = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash)
        if (!result.verified) {
            return HttpStatusCode.Unauthorized to ApiResponse(success = false, error = "Неверный логин или пароль")
        }

        return HttpStatusCode.OK to ApiResponse(success = true, role = user.role)
    }

    fun clearStore() {
        repo.clear()
    }
}
