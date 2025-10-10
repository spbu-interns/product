package org.interns.project.auth.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.interns.project.auth.reset.PasswordResetService
import org.interns.project.auth.verification.EmailVerificationService

data class EmailStartVerificationIn(val email: String)
data class EmailVerifyIn(val token: String)
data class PasswordForgotIn(val email: String)
data class PasswordResetIn(val token: String, val newPassword: String)

fun Route.fastApiCompatRoutes(
    verification: EmailVerificationService,
    reset: PasswordResetService
) {
    route("/auth") {
        route("/email") {
            post("/start") {
                val body = call.receive<EmailStartVerificationIn>()
                val ok = verification.sendCodeByEmail(body.email)
                if (ok) call.respond(HttpStatusCode.OK, mapOf("status" to "sent"))
                else call.respond(HttpStatusCode.NotFound, mapOf("detail" to "user not found or already verified"))
            }
            post("/verify") {
                val body = call.receive<EmailVerifyIn>()
                val ok = verification.verifyByToken(body.token.trim())
                if (ok) call.respond(HttpStatusCode.OK, mapOf("status" to "verified"))
                else call.respond(HttpStatusCode.BadRequest, mapOf("detail" to "invalid or expired token"))
            }
        }
        route("/password") {
            post("/forgot") {
                val body = call.receive<PasswordForgotIn>()
                val ok = reset.requestByEmail(body.email)
                // чтобы не палить наличие пользователя — можно всегда 200
                if (ok) call.respond(HttpStatusCode.OK, mapOf("status" to "sent"))
                else call.respond(HttpStatusCode.NotFound, mapOf("detail" to "user not found or not verified"))
            }
            post("/reset") {
                val body = call.receive<PasswordResetIn>()
                val userId = reset.verifyLink(body.token)
                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("detail" to "invalid or expired token"))
                } else {
                    reset.completeReset(userId, body.newPassword)
                    call.respond(HttpStatusCode.OK, mapOf("status" to "password_changed"))
                }
            }
        }
    }
}
