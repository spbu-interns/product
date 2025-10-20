package org.interns.project.security.token

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import io.github.cdimascio.dotenv.dotenv

object JwtService {
    private val dotenv = dotenv {
        filename = ".env"
        ignoreIfMalformed = true
        ignoreIfMissing = true
    }

    private fun env(name: String, default: String? = null): String? =
        System.getenv(name) ?: dotenv[name] ?: default

    private val secret: String by lazy {
        env("JWT_SECRET") ?: error("JWT_SECRET is required (workdir=${System.getProperty("user.dir")})")
    }
    private val issuer: String by lazy { env("JWT_ISSUER", "interns-server")!! }
    private val audience: String by lazy { env("JWT_AUDIENCE", "users")!! }
    private val expiresMin: Long by lazy { (env("JWT_EXPIRES_MIN", "60")!!).toLong() }
    private val alg: Algorithm by lazy { Algorithm.HMAC256(secret) }


    fun issue(
        subject: String,
        login: String,
        role: String? = null,
        email: String? = null
    ): String {
        val now = Instant.now()
        val exp = now.plus(expiresMin, ChronoUnit.MINUTES)

        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
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