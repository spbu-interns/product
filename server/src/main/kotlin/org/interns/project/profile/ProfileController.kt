package org.interns.project.profile

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.interns.project.dto.ApiResponse
import org.interns.project.dto.ProfileUpdateDto
import org.interns.project.users.model.UserProfilePatch
import org.interns.project.users.repo.ApiUserRepo
import org.interns.project.dto.*

class ProfileController(
    private val apiUserRepo: ApiUserRepo
) {

    fun registerRoutes(routing: Routing) {

        routing.route("/api/users") {

            /**
             * PATCH /api/users/{id}/profile
             */
            patch("{id}/profile") {
                try {
                    val id = call.parameters["id"]?.toLongOrNull()
                    if (id == null) {
                        return@patch call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(
                                success = false,
                                error = "Invalid user id"
                            )
                        )
                    }

                    val dto = try {
                        call.receive<ProfileUpdateDto>()
                    } catch (e: Exception) {
                        return@patch call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(
                                success = false,
                                error = "Invalid JSON body: ${e.message}"
                            )
                        )
                    }

                    // === ОБНОВЛЯЕМ USER ===
                    val normalizedGender = when (dto.gender?.uppercase()) {
                        "M" -> "MALE"
                        "F" -> "FEMALE"
                        "MALE" -> "MALE"
                        "FEMALE" -> "FEMALE"
                        else -> null
                    }

                    val patch = UserProfilePatch(
                        firstName = dto.firstName,
                        lastName = dto.lastName,
                        patronymic = dto.patronymic,
                        phoneNumber = dto.phoneNumber,
                        clinicId = dto.clinicId,
                        dateOfBirth = dto.dateOfBirth,
                        avatar = dto.avatar,
                        gender = normalizedGender
                    )

                    val updatedUser = apiUserRepo.patchUserProfile(id, patch)

                    // === ОБНОВЛЯЕМ CLIENT (если есть) ===
//                    apiUserRepo.findClientByUserId(id)?.let {
//                        apiUserRepo.patchClientByUserId(id, ClientPatch())
//                    }
//
//                    // === ОБНОВЛЯЕМ DOCTOR (если есть) ===
//                    apiUserRepo.findDoctorByUserId(id)?.let {
//                        apiUserRepo.patchDoctorByUserId(
//                            id,
//                            DoctorPatch(clinicId = patch.clinicId)
//                        )
//                    }

                    // успех
                    call.respond(
                        ApiResponse(
                            success = true,
                            data = updatedUser,
                            error = null
                        )
                    )

                } catch (e: Exception) {
                    call.application.log.error("Profile update error", e)

                    call.respond(
                        ApiResponse<Unit>(
                            success = false,
                            error = "Profile update failed: ${e.message}"
                        )
                    )
                }
            }
        }
    }
}
