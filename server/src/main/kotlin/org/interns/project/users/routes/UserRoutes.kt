package org.interns.project.users.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.interns.project.users.dto.RegisterRequest
import org.interns.project.users.repo.PostgresUserRepo
import org.interns.project.users.service.UserService

fun Application.registerUserRoutes() {

    routing {
        post("/register") {
            val req = call.receive<RegisterRequest>()
            val svc = UserService(PostgresUserRepo())
            try {
                val created = svc.registerAndReturn(req)
                call.respond(
                    HttpStatusCode.Created,
                    mapOf(
                        "success" to true,
                        "id" to created.id,
                        "email" to created.email,
                        "login" to created.login,
                        "role" to created.role
                    )
                )
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "error" to e.message))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.Conflict, mapOf("success" to false, "error" to e.message))
            }
        }
    }
}
