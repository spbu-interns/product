package org.interns.project.security.token

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.interns.project.config.AppConfig
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

object JwtService {
    private val alg: Algorithm by lazy { Algorithm.HMAC256(AppConfig.jwtSecret) }

    fun issue(
        subject: String,
        login: String,
        role: String? = null,
        email: String? = null
    ): String {
        val now = Instant.now()
        val exp = now.plus(AppConfig.jwtExpiresMin.toLong(), ChronoUnit.MINUTES)

        return JWT.create()
            .withIssuer(AppConfig.jwtIssuer)
            .withAudience(AppConfig.jwtAudience)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(exp))
            .withSubject(subject)
            .withClaim("userId", subject)
            .withClaim("login", login)
            .withClaim("role", role)
            .withClaim("email", email)
            .sign(alg)
    }
}
