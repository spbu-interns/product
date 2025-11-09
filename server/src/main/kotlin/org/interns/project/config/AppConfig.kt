package org.interns.project.config

import io.github.cdimascio.dotenv.dotenv

object AppConfig {
    private val env = dotenv {
        filename = ".env"
        ignoreIfMalformed = true
        ignoreIfMissing = true
    }

    private fun envStr(key: String, default: String? = null): String =
        System.getenv(key) ?: env[key] ?: default ?: error("$key is not set")

    private fun envInt(key: String, default: Int? = null): Int =
        (System.getenv(key) ?: env[key])?.toIntOrNull() ?: default ?: error("$key is not set")

    private fun envBool(key: String, default: Boolean): Boolean =
        (System.getenv(key) ?: env[key])?.toBooleanStrictOrNull() ?: default

    // mail
    val mailHost by lazy { envStr("MAIL_SMTP_HOST") }
    val mailPort by lazy { envInt("MAIL_SMTP_PORT") }
    val mailUsername by lazy { envStr("SMTP_USERNAME") }
    val mailPassword by lazy { envStr("SMTP_PASSWORD") }
    val mailSsl by lazy { envBool("MAIL_SMTP_SSL", true) }
    val mailStarttls by lazy { envBool("MAIL_SMTP_STARTTLS", false) }
    val mailFrom by lazy { envStr("MAIL_FROM") }
    val mailReplyTo by lazy { System.getenv("MAIL_REPLY_TO") ?: env["MAIL_REPLY_TO"] }

    val mailConnectTimeout by lazy { envInt("MAIL_SMTP_CONNECT_TIMEOUT_MS", 8000) }
    val mailWriteTimeout   by lazy { envInt("MAIL_SMTP_WRITE_TIMEOUT_MS", 8000) }
    val mailReadTimeout   by lazy { envInt("MAIL_SMTP_READ_TIMEOUT_MS", 8000) }

    // security
    val tokenPepper by lazy { envStr("TOKEN_PEPPER") }
    val bcryptCost by lazy { envInt("BCRYPT_COST", 12) }
    val verificationTtlMinutes by lazy { envInt("SECURITY_VERIFICATION_CODE_TTL_MINUTES", 3) }
    val verificationMaxAttempts by lazy { envInt("SECURITY_VERIFICATION_CODE_MAX_ATTEMPTS", 6) }
    val passwordResetTtlMinutes by lazy { envInt("SECURITY_PASSWORD_RESET_TTL_MINUTES", 15) }
    val passwordResetSessionTtlMinutes by lazy { envInt("SECURITY_PASSWORD_RESET_SESSION_TTL_MINUTES", 5) }

    // app
    val baseUrl by lazy { envStr("APP_BASE_URL") }

    // db
    val dbHost by lazy { envStr("DB_HOST", envStr("POSTGRES_HOST", "localhost")) }
    val dbPort by lazy { (envStr("DB_PORT", envStr("POSTGRES_PORT", "5432"))).toInt() }
    val dbName by lazy { envStr("DB_NAME", envStr("POSTGRES_DB", "usersdb")) }
    val dbUser by lazy { envStr("DB_USER", envStr("POSTGRES_USER", "app")) }
    val dbPassword by lazy { envStr("DB_PASSWORD", envStr("POSTGRES_PASSWORD")) }

    val jwtSecret by lazy { envStr("JWT_SECRET") }
    val jwtIssuer by lazy { envStr("JWT_ISSUER", "org.interns.project") }
    val jwtAudience by lazy { envStr("JWT_AUDIENCE", "users") }
    val jwtExpiresMin by lazy { envInt("JWT_EXPIRES_MIN", 60) }
}
