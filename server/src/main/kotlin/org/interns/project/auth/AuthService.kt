package org.interns.project.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

class AuthService(
    private val userRepository: UserRepository,
    private val jwtSecret: String,
    private val jwtExpirationMs: Long = 3600000
) {

    sealed class Result {
        data class Success(val token: String, val role: String) : Result()
        data class Failure(val message: String) : Result()
    }

    fun login(login: String, password: String): Result {
        if (login.isBlank() || password.isBlank()) {
            return Result.Failure("Login and password must not be empty")
        }

        val user = userRepository.findByLogin(login) ?: return Result.Failure("Неверный login или пароль")

        val verifier = BCrypt.verifyer()
        val result = verifier.verify(password.toCharArray(), user.passwordHash)
        if (!result.verified) {
            return Result.Failure("Неверный login или пароль")
        }

        val now = Date()
        val exp = Date(now.time + jwtExpirationMs)
        val token = JWT.create()
            .withIssuedAt(now)
            .withExpiresAt(exp)
            .withClaim("login", user.login)
            .withClaim("role", user.role)
            .sign(Algorithm.HMAC256(jwtSecret))

        return Result.Success(token, user.role)
    }
}
