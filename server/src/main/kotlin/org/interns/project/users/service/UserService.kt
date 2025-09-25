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

    fun register(req: RegisterRequest): Pair<HttpStatusCode, ApiResponse> {
        if (!Validation.isValidEmail(req.email)) {
            return HttpStatusCode.BadRequest to ApiResponse(success = false, error = "Неверный формат email")
        }

        if (repo.findByEmail(req.email) != null) {
            return HttpStatusCode.BadRequest to ApiResponse(success = false, error = "Email уже зарегистрирован")
        }

        if (repo.findByLogin(req.login) != null) {
            return HttpStatusCode.BadRequest to ApiResponse(success = false, error = "Логин уже занят")
        }

        val bcryptHash = BCrypt.withDefaults()
            .hashToString(SecurityConfig.bcryptCost, req.password.toCharArray())

        val id: Long = repo.nextId()
        val now: Instant = Instant.now()

        val user = User(
            id = id,
            email = req.email,
            login = req.login,
            passwordHash = bcryptHash,
            role = req.role ?: "CLIENT",
            firstName = req.firstName,
            lastName = req.lastName,
            patronymic = req.patronymic,
            phoneNumber = req.phoneNumber,
            isActive = true,
            clinicId = null,
            createdAt = now,
            updatedAt = now
        )

        repo.save(user)
        return HttpStatusCode.OK to ApiResponse(success = true, role = user.role)
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
