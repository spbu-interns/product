package org.interns.project.auth

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*

fun Application.authRoutes() {
    val userRepository = UserRepository()
    val authService = AuthService(userRepository, jwtSecret = System.getenv("APP_JWT_SECRET") ?: "dev-secret")

    routing {
        post("/auth/login") {
            val req = call.receive<LoginRequest>()
            when (val result = authService.login(req.login, req.password)) {
                is AuthService.Result.Success -> {
                    call.respond(
                        LoginSuccessResponse(
                            token = result.token,
                            role = result.role
                        )
                    )
                }
                is AuthService.Result.Failure -> {
                    val status = if (result.message == "Login and password must not be empty") {
                        HttpStatusCode.BadRequest
                    } else {
                        HttpStatusCode.Unauthorized
                    }
                    call.respond(
                        status,
                        mapOf("success" to false, "error" to result.message)
                    )
                }
            }
        }
    }
}
