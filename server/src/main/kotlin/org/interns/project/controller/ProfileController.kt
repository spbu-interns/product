package org.interns.project.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import io.ktor.server.routing.get
import org.interns.project.dto.ApiResponse
import org.interns.project.dto.FullUserProfileDto
import org.interns.project.dto.ProfileUpdateDto
import org.interns.project.dto.UserResponseDto
import org.interns.project.users.model.ClientPatch
import org.interns.project.users.model.DoctorPatch
import org.interns.project.users.model.UserOutDto
import org.interns.project.users.model.UserProfilePatch
import org.interns.project.users.repo.ApiUserRepo

class ProfileController(
    private val apiUserRepo: ApiUserRepo
) {
    private fun hasPatientFields(dto: ProfileUpdateDto): Boolean {
        return dto.bloodType != null ||
                dto.height != null ||
                dto.weight != null ||
                dto.emergencyContactName != null ||
                dto.emergencyContactNumber != null ||
                dto.address != null ||
                dto.snils != null ||
                dto.passport != null ||
                dto.dmsOms != null
    }

    private fun hasDoctorFields(dto: ProfileUpdateDto): Boolean {
        return dto.profession != null ||
                dto.info != null ||
                dto.experience != null ||
                dto.price != null
    }

    private fun hasUserFields(dto: ProfileUpdateDto): Boolean {
        return dto.firstName != null ||
                dto.lastName != null ||
                dto.patronymic != null ||
                dto.phoneNumber != null ||
                dto.clinicId != null ||
                dto.dateOfBirth != null ||
                dto.avatar != null ||
                dto.gender != null
    }

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

                    val updatedUser = if (hasUserFields(dto)) {
                        apiUserRepo.patchUserProfile(id, patch)
                    } else {
                        apiUserRepo.getUserProfile(id) ?: return@patch call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<Unit>(
                                success = false,
                                error = "User not found"
                            )
                        )
                    }

                    if (hasPatientFields(dto)) {
                        try {
                            val clientPatch = ClientPatch(
                                bloodType = dto.bloodType,
                                height = dto.height,
                                weight = dto.weight,
                                emergencyContactName = dto.emergencyContactName,
                                emergencyContactNumber = dto.emergencyContactNumber,
                                address = dto.address,
                                snils = dto.snils,
                                passport = dto.passport,
                                dmsOms = dto.dmsOms
                            )
                            apiUserRepo.patchClientByUserId(id, clientPatch)
                        } catch (e: Exception) {
                            call.application.log.warn("Failed to update client profile for user $id: ${e.message}")
                        }
                    }
                    if (hasDoctorFields(dto)) {
                        try {
                            val doctorPatch = DoctorPatch(
                                clinicId = dto.clinicId,
                                profession = dto.profession,
                                info = dto.info,
                                experience = dto.experience,
                                price = dto.price
                            )
                            apiUserRepo.patchDoctorByUserId(id, doctorPatch)
                        } catch (iae: IllegalArgumentException) {
                            call.application.log.warn("Validation error while updating doctor profile for user $id: ${iae.message}")
                            return@patch call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(
                                    success = false,
                                    error = iae.message ?: "Некорректные данные врача"
                                )
                            )
                        } catch (e: Exception) {
                            call.application.log.warn("Failed to update doctor profile for user $id: ${e.message}", e)
                            return@patch call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<Unit>(
                                    success = false,
                                    error = "Не удалось сохранить данные врача: ${e.message}"
                                )
                            )
                        }
                    }

                    call.respond(
                        ApiResponse<UserResponseDto>(
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

            /**
             * GET /api/users/{id}/dashboard
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

                    val userProfile = apiUserRepo.getUserProfile(id)
                        ?: return@get call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<Unit>(
                                success = false,
                                error = "User not found"
                            )
                        )

                    val client = apiUserRepo.findClientByUserId(id)
                    val doctor = apiUserRepo.findDoctorByUserId(id)

                    val dashboardData = when {
                        client != null -> {
                            val clientId = client.id
                            val appointments = apiUserRepo.listAppointmentsForClient(clientId)
                            val medicalRecords = apiUserRepo.listMedicalRecordsForClient(clientId)
                            val nextAppointment = apiUserRepo.getNextAppointmentForClient(clientId)

                            FullUserProfileDto(
                                user = userProfile,
                                client = client,
                                appointments = appointments,
                                medicalRecords = medicalRecords,
                                nextAppointment = nextAppointment
                            )
                        }
                        doctor != null -> {
                            // Доктор
                            val doctorId = doctor.id
                            val appointments = apiUserRepo.listAppointmentsForDoctor(doctorId)
                            val patients = apiUserRepo.listPatientsForDoctor(doctorId)

                            FullUserProfileDto(
                                user = userProfile,
                                doctor = doctor,
                                appointments = appointments,
                                patients = patients
                            )
                        }
                        else -> {
                            FullUserProfileDto(
                                user = userProfile
                            )
                        }
                    }

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