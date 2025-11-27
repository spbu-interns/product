package org.interns.project.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import io.ktor.server.routing.get
import kotlinx.coroutines.async
import org.interns.project.dto.ApiResponse
import org.interns.project.dto.FullUserProfileDto
import org.interns.project.dto.ProfileUpdateDto
import org.interns.project.users.model.UserOutDto
import org.interns.project.users.model.UserProfilePatch
import org.interns.project.users.repo.ApiUserRepo

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
                        ApiResponse<UserOutDto>(
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
            // ProfileController.kt - добавляем новый метод

            /**
             * GET /api/users/{id}/dashboard
             * Получение всех данных для дашборда пациента
             */
            get("{id}/dashboard") {
                try {
                    val id = call.parameters["id"]?.toLongOrNull()
                    if (id == null) {
                        return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(
                                success = false,
                                error = "Invalid user id"
                            )
                        )
                    }

                    // Получаем базовый профиль пользователя
                    val userProfile = apiUserRepo.getUserProfile(id)
                        ?: return@get call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<Unit>(
                                success = false,
                                error = "User not found"
                            )
                        )

                    // Получаем clientId
                    val client = apiUserRepo.findClientByUserId(id)
                        ?: return@get call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<Unit>(
                                success = false,
                                error = "Client profile not found"
                            )
                        )

                    val clientId = client.id

                    val upcomingAppointments =
                        apiUserRepo.listAppointmentsForClient(clientId)
                            .filter { it.status == "BOOKED" }
                            .sortedBy { it.createdAt }


                    val medicalRecords =
                        apiUserRepo.listMedicalRecordsForClient(clientId)
                            .sortedByDescending { it.createdAt }
                            .take(3)


                    val allAppointments = apiUserRepo.listAppointmentsForClient(clientId)

                    val dashboardData = FullUserProfileDto(
                        user = userProfile,
                        client = client,
                        appointments = allAppointments,
                        medicalRecords = medicalRecords
                    )

                    call.respond(
                        ApiResponse(
                            success = true,
                            data = dashboardData
                        )
                    )

                } catch (e: Exception) {
                    call.application.log.error("Dashboard data loading error", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<Unit>(
                            success = false,
                            error = "Failed to load dashboard data: ${e.message}"
                        )
                    )
                }
            }

        }
    }
}