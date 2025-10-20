package org.interns.project.config

object AppConfig {
    val mailHost: String = System.getenv("MAIL_SMTP_HOST")
        ?: throw IllegalStateException("MAIL_SMTP_HOST is not set")
    val mailPort: Int = System.getenv("MAIL_SMTP_PORT")?.toIntOrNull()
        ?: throw IllegalStateException("MAIL_SMTP_PORT is not set")
    val mailUsername: String = System.getenv("SMTP_USERNAME")
        ?: throw IllegalStateException("SMTP_USERNAME is not set")
    val mailPassword: String = System.getenv("SMTP_PASSWORD")
        ?: throw IllegalStateException("SMTP_PASSWORD is not set")
    val mailSsl: Boolean = System.getenv("MAIL_SMTP_SSL")?.toBoolean() ?: true
    val mailStarttls: Boolean = System.getenv("MAIL_SMTP_STARTTLS")?.toBoolean() ?: false
    val mailFrom: String = System.getenv("MAIL_FROM")
        ?: throw IllegalStateException("MAIL_FROM is not set")
    val mailReplyTo: String? = System.getenv("MAIL_REPLY_TO")
    val mailConnectTimeout: Int = System.getenv("MAIL_SMTP_CONNECT_TIMEOUT_MS")?.toIntOrNull() ?: 8000
    val mailWriteTimeout: Int = System.getenv("MAIL_SMTP_WRITE_TIMEOUT_MS")?.toIntOrNull() ?: 8000
    val mailReadTimeout: Int = System.getenv("MAIL_SMTP_READ_TIMEOUT_MS")?.toIntOrNull() ?: 8000

    val tokenPepper: String = System.getenv("TOKEN_PEPPER")
        ?: throw IllegalStateException("TOKEN_PEPPER is not set")
    val bcryptCost: Int = System.getenv("BCRYPT_COST")?.toIntOrNull() ?: 12
    val verificationTtlMinutes: Int = System.getenv("SECURITY_VERIFICATION_CODE_TTL_MINUTES")?.toIntOrNull() ?: 3
    val verificationMaxAttempts: Int = System.getenv("SECURITY_VERIFICATION_CODE_MAX_ATTEMPTS")?.toIntOrNull() ?: 6
    val passwordResetTtlMinutes: Int = System.getenv("SECURITY_PASSWORD_RESET_TTL_MINUTES")?.toIntOrNull() ?: 15
    val passwordResetSessionTtlMinutes: Int = System.getenv("SECURITY_PASSWORD_RESET_SESSION_TTL_MINUTES")?.toIntOrNull() ?: 5

    val baseUrl: String = System.getenv("APP_BASE_URL")
        ?: throw IllegalStateException("APP_BASE_URL is not set")

    val dbHost: String = System.getenv("DB_HOST") ?: throw IllegalStateException("DB_HOST is not set")
    val dbPort: Int = System.getenv("DB_PORT")?.toIntOrNull() ?: throw IllegalStateException("DB_PORT is not set")
    val dbName: String = System.getenv("DB_NAME") ?: throw IllegalStateException("DB_NAME is not set")
    val dbUser: String = System.getenv("DB_USER") ?: throw IllegalStateException("DB_USER is not set")
    val dbPassword: String = System.getenv("DB_PASSWORD") ?: throw IllegalStateException("DB_PASSWORD is not set")
}
