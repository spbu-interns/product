package org.interns.project.users

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.interns.project.dto.ApiResponse
import org.interns.project.dto.ClientProfileDto
import org.interns.project.dto.UserResponseDto
import org.interns.project.users.model.ClientOut
import org.interns.project.users.model.User
import org.interns.project.users.repo.ApiUserRepo

class UserController(
    private val apiUserRepo: ApiUserRepo
) {
    fun registerRoutes(route: Route) {
        route.route("/api/users") {
            get {
                val role = call.request.queryParameters["role"]?.takeIf { it.isNotBlank() }
                runCatching { apiUserRepo.listUsers(role) }
                    .onSuccess { users ->
                        val payload = users.map { it.toDto() }
                        call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = payload))
                    }
                    .onFailure { error ->
                        call.application.log.error("Failed to load users", error)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<List<UserResponseDto>>(success = false, error = error.message ?: "Failed to load users")
                        )
                    }
            }

            get("/{userId}/profile") {
                val userId = call.parameters["userId"]?.toLongOrNull()
                    ?: return@get respondBadRequest(call, "Invalid user id")

                runCatching { apiUserRepo.getUserProfile(userId) }
                    .onSuccess { user ->
                        if (user == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<UserResponseDto>(success = false, error = "User not found")
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(success = true, data = user.toDto())
                            )
                        }
                    }
                    .onFailure { error ->
                        call.application.log.error("Failed to load user profile", error)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<UserResponseDto>(success = false, error = error.message ?: "Failed to load user profile")
                        )
                    }
            }
        }

        route.get("/api/clients/by-user/{userId}") {
            val userId = call.parameters["userId"]?.toLongOrNull()
                ?: return@get respondBadRequest(call, "Invalid user id")

            runCatching { apiUserRepo.findClientByUserId(userId) }
                .onSuccess { client ->
                    if (client == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<ClientProfileDto?>(success = false, error = "Client not found")
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(success = true, data = client.toDto())
                        )
                    }
                }
                .onFailure { error ->
                    call.application.log.error("Failed to load client profile", error)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<ClientProfileDto?>(success = false, error = error.message ?: "Failed to load client profile")
                    )
                }
        }
    }

    private suspend fun respondBadRequest(call: ApplicationCall, message: String) {
        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = message))
    }

    private fun User.toDto() = UserResponseDto(
        id = id,
        email = email,
        login = login,
        role = role,
        firstName = firstName,
        lastName = lastName,
        patronymic = patronymic,
        phoneNumber = phoneNumber,
        clinicId = clinicId,
        name = name,
        surname = surname,
        dateOfBirth = dateOfBirth?.toString(),
        avatar = avatar,
        gender = gender,
        isActive = isActive,
        emailVerifiedAt = emailVerifiedAt?.toString(),
        passwordChangedAt = passwordChangedAt?.toString(),
        createdAt = createdAt?.toString(),
        updatedAt = updatedAt?.toString()
    )

    private fun ClientOut.toDto() = ClientProfileDto(
        id = id,
        userId = userId,
        bloodType = bloodType,
        height = height,
        weight = weight,
        emergencyContactName = emergencyContactName,
        emergencyContactNumber = emergencyContactNumber,
        address = address,
        snils = snils,
        passport = passport,
        dmsOms = dmsOms,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}