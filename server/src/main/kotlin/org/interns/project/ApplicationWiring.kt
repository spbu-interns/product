package org.interns.project

import io.ktor.server.application.*
import org.interns.project.auth.reset.PasswordResetService
import org.interns.project.auth.verification.EmailVerificationService
import org.interns.project.auth.verification.EmailVerificationPort
import org.interns.project.config.AppConfig
import org.interns.project.notifications.Mailer
import org.interns.project.notifications.SmtpMailer
import org.interns.project.security.token.EmailVerificationRepo
import org.interns.project.security.token.PasswordResetRepo

fun Application.installEmailFeatures(): Pair<EmailVerificationPort, PasswordResetService> {
    val mailer: Mailer = SmtpMailer(
        host = AppConfig.mailHost,
        port = AppConfig.mailPort,
        username = AppConfig.mailUsername,
        password = AppConfig.mailPassword,
        ssl = AppConfig.mailSsl,
        starttls = AppConfig.mailStarttls,
        connectTimeoutMs = AppConfig.mailConnectTimeout,
        writeTimeoutMs = AppConfig.mailWriteTimeout,
        readTimeoutMs = AppConfig.mailReadTimeout,
        defaultFrom = AppConfig.mailFrom,
        defaultReplyTo = AppConfig.mailReplyTo
    )

    val pepper: String = AppConfig.tokenPepper

    val verificationService = EmailVerificationService(
        repo = EmailVerificationRepo(pepper),
        mailer = mailer,
        ttlMinutes = AppConfig.verificationTtlMinutes,
        maxAttempts = AppConfig.verificationMaxAttempts
    )

    val passwordResetService = PasswordResetService(
        repo = PasswordResetRepo(pepper),
        mailer = mailer,
        baseUrl = AppConfig.baseUrl,
        ttlMinutes = AppConfig.passwordResetTtlMinutes,
        sessionTtlMinutes = AppConfig.passwordResetSessionTtlMinutes,
        bcryptCost = AppConfig.bcryptCost
    )

    return Pair(verificationService, passwordResetService)
}
