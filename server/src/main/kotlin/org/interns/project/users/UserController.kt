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
import org.interns.project.dto.*
import org.interns.project.users.model.*

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

            get("/{userId}/full") {
                val userId = call.parameters["userId"]?.toLongOrNull()
                    ?: return@get respondBadRequest(call, "Invalid user id")

                runCatching {
                    val user = apiUserRepo.getUserProfile(userId)
                        ?: return@runCatching call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<FullUserProfileDto>(
                                success = false,
                                error = "User not found",
                            )
                        )

                    // пытаемся найти client/doctor по user_id
                    val client = apiUserRepo.findClientByUserId(userId)
                    val doctor = apiUserRepo.findDoctorByUserId(userId) // пока только для when, без .toDto()

                    val (appointmentsModel, recordsModel, patientsModel) =
                        when {
                            user.role == "CLIENT" && client != null -> {
                                val apps = apiUserRepo.listAppointmentsForClient(client.id)
                                val recs = apiUserRepo.listMedicalRecordsForClient(client.id)
                                Triple(apps, recs, emptyList<DoctorPatientOut>())
                            }
                            user.role == "DOCTOR" && doctor != null -> {
                                val apps = apiUserRepo.listAppointmentsForDoctor(doctor.id)
                                val pats = apiUserRepo.listPatientsForDoctor(doctor.id)
                                Triple(apps, emptyList<MedicalRecordOut>(), pats)
                            }
                            else -> Triple(
                                emptyList<AppointmentOut>(),
                                emptyList<MedicalRecordOut>(),
                                emptyList<DoctorPatientOut>(),
                            )
                        }

                    val dto = FullUserProfileDto(
                        user = user.toDto(),
                        client = client?.toDto(),
                        doctor = doctor?.toDto(),
                        appointments = appointmentsModel.map { it.toDto() },
                        medicalRecords = recordsModel.map { it.toDto() },
                        patients = patientsModel.map { it.toDto() },
                    )

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = dto),
                    )
                }.onFailure { e ->
                    call.application.log.error("Failed to load full profile for user $userId", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<FullUserProfileDto>(
                            success = false,
                            error = e.message ?: "Failed to load full profile",
                        ),
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

    private fun AppointmentOut.toDto(): AppointmentDto =
        AppointmentDto(
            id = id,
            slotId = slotId,
            clientId = clientId,
            status = status,
            comments = comments,
            createdAt = createdAt ?: "",
            updatedAt = updatedAt ?: "",
            canceledAt = canceledAt,
            completedAt = completedAt,
            appointmentTypeId = appointmentTypeId,
        )

    private fun MedicalRecordOut.toDto(): MedicalRecordDto =
        MedicalRecordDto(
            id = id,
            clientId = clientId,
            doctorId = doctorId,
            appointmentId = appointmentId,
            diagnosis = diagnosis,
            symptoms = symptoms,
            treatment = treatment,
            recommendations = recommendations,
            createdAt = createdAt ?: "",
            updatedAt = updatedAt,
        )

    private fun DoctorPatientOut.toDto(): DoctorPatientDto =
        DoctorPatientDto(
            clientId = clientId,
            userId = userId,
            name = name,
            surname = surname,
            patronymic = patronymic,
            phoneNumber = phoneNumber,
            dateOfBirth = dateOfBirth,
            avatar = avatar,
            gender = gender,
        )
    private fun DoctorOut.toDto() = DoctorProfileDto(
        id = id,
        userId = userId,
        clinicId = clinicId,
        profession = profession,
        info = info,
        isConfirmed = isConfirmed,
        rating = rating,
        experience = experience,
        price = price,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
    
}