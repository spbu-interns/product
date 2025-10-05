package org.interns.project.users.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.interns.project.users.model.UserInDto
import org.interns.project.users.repo.ApiUserRepo


fun Application.userRoutes() {
    val userRepo = ApiUserRepo()
    
    routing {
        route("/api/users") {

            get("/exists/email/{email}") {
                val email = call.parameters["email"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, 
                    mapOf("success" to false, "error" to "Email parameter is required")
                )
                
                try {
                    val user = userRepo.findByEmail(email)
                    call.respond(user != null)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("success" to false, "error" to "Error checking email existence: ${e.message}")
                    )
                }
            }

            get("/exists/login/{login}") {
                val login = call.parameters["login"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("success" to false, "error" to "Login parameter is required")
                )
                
                try {
                    val user = userRepo.findByLogin(login)
                    call.respond(user != null)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("success" to false, "error" to "Error checking login existence: ${e.message}")
                    )
                }
            }

            post("/register") {
                try {
                    val registerRequest = call.receive<RegisterRequest>()

                    val userInDto = UserInDto(
                        email = registerRequest.email,
                        login = registerRequest.username,
                        password = registerRequest.password,
                        role = "CLIENT",
                        firstName = null,
                        lastName = null,
                        clinicId = null,
                        isActive = registerRequest.is_active
                    )
                    
                    val user = userRepo.saveByApi(userInDto)
                    call.respond(
                        HttpStatusCode.Created,
                        mapOf(
                            "success" to true,
                            "id" to user.id
                        )
                    )
                } catch (e: IllegalStateException) {
                    call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("success" to false, "error" to "Email or username already exists")
                    )
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("success" to false, "error" to e.message)
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("success" to false, "error" to "Error during registration: ${e.message}")
                    )
                }
            }

            get("/by-email/{email}") {
                val email = call.parameters["email"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("success" to false, "error" to "Email parameter is required")
                )
                
                try {
                    val user = userRepo.findByEmail(email)
                    if (user != null) {
                        call.respond(user)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("success" to false, "error" to "User not found"))
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("success" to false, "error" to "Error retrieving user: ${e.message}")
                    )
                }
            }

            get("/by-login/{login}") {
                val login = call.parameters["login"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("success" to false, "error" to "Login parameter is required")
                )
                
                try {
                    val user = userRepo.findByLogin(login)
                    if (user != null) {
                        call.respond(user)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("success" to false, "error" to "User not found"))
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("success" to false, "error" to "Error retrieving user: ${e.message}")
                    )
                }
            }
        }
    }
}

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    val is_active: Boolean = true
)