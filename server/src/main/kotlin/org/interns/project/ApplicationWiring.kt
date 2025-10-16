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

fun Application.installEmailFeatures() : Pair<EmailVerificationService, PasswordResetService>{
    val cfg = environment.config

    val mailer: Mailer = SmtpMailer(
        host = cfg.property("mail.smtp.host").getString(),
        port = cfg.property("mail.smtp.port").getString().toInt(),
        username = cfg.property("mail.smtp.username").getString(),
        password = cfg.property("mail.smtp.password").getString(),
        ssl = cfg.property("mail.smtp.ssl").getString().toBoolean(),
        starttls = cfg.property("mail.smtp.starttls").getString().toBoolean(),
        connectTimeoutMs = cfg.property("mail.smtp.connectTimeoutMs").getString().toInt(),
        writeTimeoutMs = cfg.property("mail.smtp.writeTimeoutMs").getString().toInt(),
        readTimeoutMs = cfg.property("mail.smtp.readTimeoutMs").getString().toInt(),
        defaultFrom = cfg.property("mail.from").getString(),
        defaultReplyTo = cfg.propertyOrNull("mail.replyTo")?.getString()
    )

    val pepper = cfg.propertyOrNull("security.tokenPepper")?.getString()

    val verificationSvc = EmailVerificationService(
        repo = EmailVerificationRepo(pepper),
        mailer = mailer,
        ttlMinutes = cfg.property("security.verificationCode.ttlMinutes").getString().toInt(),
        maxAttempts = cfg.property("security.verificationCode.maxAttempts").getString().toInt()
    )

    val passwordResetSvc = PasswordResetService(
        repo = PasswordResetRepo(pepper),
        mailer = mailer,
        baseUrl = cfg.property("app.baseUrl").getString(),
        ttlMinutes = cfg.property("security.passwordReset.ttlMinutes").getString().toInt(),
        sessionTtlMinutes = cfg.property("security.passwordReset.sessionTtlMinutes").getString().toInt(),
        bcryptCost = SecurityConfig.bcryptCost
    )


    return Pair(verificationSvc, passwordResetSvc)
}

