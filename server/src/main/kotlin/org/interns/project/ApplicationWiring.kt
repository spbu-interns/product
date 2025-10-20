

package org.interns.project

import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.interns.project.auth.reset.PasswordResetService
import org.interns.project.auth.routes.fastApiCompatRoutes
import org.interns.project.auth.verification.EmailVerificationService
import org.interns.project.notifications.Mailer
import org.interns.project.notifications.SmtpMailer
import org.interns.project.security.token.EmailVerificationRepo
import org.interns.project.security.token.PasswordResetRepo
import org.interns.project.config.SecurityConfig


fun Application.installEmailFeatures(): Pair<EmailVerificationService, PasswordResetService> {
    val mailer: Mailer = SmtpMailer(
        host = System.getenv("MAIL_SMTP_HOST") ?: "smtp.yandex.ru",
        port = System.getenv("MAIL_SMTP_PORT")?.toIntOrNull() ?: 465,
        username = System.getenv("SMTP_USERNAME") ?: "ems-no-reply@yandex.ru",
        password = System.getenv("SMTP_PASSWORD") ?: "odcbnipekdvheflf",
        ssl = System.getenv("MAIL_SMTP_SSL")?.toBoolean() ?: true,
        starttls = System.getenv("MAIL_SMTP_STARTTLS")?.toBoolean() ?: false,
        connectTimeoutMs = System.getenv("MAIL_SMTP_CONNECT_TIMEOUT_MS")?.toIntOrNull() ?: 8000,
        writeTimeoutMs = System.getenv("MAIL_SMTP_WRITE_TIMEOUT_MS")?.toIntOrNull() ?: 8000,
        readTimeoutMs = System.getenv("MAIL_SMTP_READ_TIMEOUT_MS")?.toIntOrNull() ?: 8000,
        defaultFrom = System.getenv("MAIL_FROM") ?: "ems-no-reply@yandex.ru",
        defaultReplyTo = System.getenv("MAIL_REPLY_TO")
    )

    log.info("Mail configuration loaded from environment variables")

    val pepper = System.getenv("TOKEN_PEPPER")

    val verificationSvc = EmailVerificationService(
        repo = EmailVerificationRepo(pepper),
        mailer = mailer,
        ttlMinutes = System.getenv("SECURITY_VERIFICATION_CODE_TTL_MINUTES")?.toIntOrNull() ?: 3,
        maxAttempts = System.getenv("SECURITY_VERIFICATION_CODE_MAX_ATTEMPTS")?.toIntOrNull() ?: 6
    )

    val passwordResetSvc = PasswordResetService(
        repo = PasswordResetRepo(pepper),
        mailer = mailer,
        baseUrl = System.getenv("APP_BASE_URL") ?: "http://localhost:8000",
        ttlMinutes = System.getenv("SECURITY_PASSWORD_RESET_TTL_MINUTES")?.toIntOrNull() ?: 15,
        sessionTtlMinutes = System.getenv("SECURITY_PASSWORD_RESET_SESSION_TTL_MINUTES")?.toIntOrNull() ?: 5,
        bcryptCost = SecurityConfig.bcryptCost
    )

    // Keep this routing for backward compatibility
    // Note: We don't register routes here - they're registered in Application.kt
    // This function just initializes and returns the services

    // Return services for further use
    return Pair(verificationSvc, passwordResetSvc)
}
