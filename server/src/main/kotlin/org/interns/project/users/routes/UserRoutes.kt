package org.interns.project.users.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.interns.project.users.dto.RegisterRequest
import org.interns.project.users.service.UserService

fun Application.registerUserRoutes(userService: UserService = UserService()) {

    routing {
        route("/api/users") {
            post("/register") {
                val req = call.receive<RegisterRequest>()
                val (status, resp) = userService.register(req)
                call.respond(status, resp)
            }

            data class LoginRequest(val login: String, val password: String)

            post("/login") {
                val req = call.receive<LoginRequest>()
                val (status, resp) = userService.login(req.login, req.password)
                call.respond(status, resp)
            }
        }
    }
}
