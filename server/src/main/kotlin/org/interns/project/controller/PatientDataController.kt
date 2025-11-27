package org.interns.project.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.interns.project.dto.ApiResponse
import org.interns.project.dto.ComplaintCreateRequest
import org.interns.project.dto.ComplaintPatchRequest
import org.interns.project.dto.ComplaintResponse
import org.interns.project.dto.DoctorNoteCreateRequest
import org.interns.project.dto.DoctorNotePatchRequest
import org.interns.project.dto.DoctorNoteResponse
import org.interns.project.dto.NoteVisibilityDto
import org.interns.project.users.model.ComplaintIn
import org.interns.project.users.model.ComplaintOut
import org.interns.project.users.model.ComplaintPatch
import org.interns.project.users.model.NoteIn
import org.interns.project.users.model.NoteOut
import org.interns.project.users.model.NotePatch
import org.interns.project.users.model.NoteVisibility
import org.interns.project.users.repo.ApiUserRepo

class PatientDataController(
    private val apiUserRepo: ApiUserRepo
) {
    fun registerRoutes(route: Route) {
        route.route("/api/patient") {
            route("/{patientId}") {
                route("/complaints") {
                    post {
                        val patientId = call.parameters["patientId"]?.toLongOrNull()
                            ?: return@post respondBadRequest(call, "Invalid patient id")
                        val request = call.receive<ComplaintCreateRequest>()

                        runCatching {
                            apiUserRepo.createComplaint(
                                patientId = patientId,
                                input = request.toModel()
                            ).toDto()
                        }.onSuccess { dto ->
                            call.respond(HttpStatusCode.Companion.Created, ApiResponse(success = true, data = dto))
                        }.onFailure { error ->
                            call.respond(
                                HttpStatusCode.Companion.InternalServerError,
                                ApiResponse<ComplaintResponse>(
                                    success = false,
                                    error = error.message ?: "Failed to create complaint"
                                )
                            )
                        }
                    }

                    get {
                        val patientId = call.parameters["patientId"]?.toLongOrNull()
                            ?: return@get respondBadRequest(call, "Invalid patient id")

                        runCatching {
                            apiUserRepo.listComplaints(patientId, null).map { it.toDto() }
                        }.onSuccess { list ->
                            call.respond(HttpStatusCode.Companion.OK, ApiResponse(success = true, data = list))
                        }.onFailure { error ->
                            call.respond(
                                HttpStatusCode.Companion.InternalServerError,
                                ApiResponse<List<ComplaintResponse>>(
                                    success = false,
                                    error = error.message ?: "Failed to load complaints"
                                )
                            )
                        }
                    }
                }

                route("/notes") {
                    post {
                        val patientId = call.parameters["patientId"]?.toLongOrNull()
                            ?: return@post respondBadRequest(call, "Invalid patient id")
                        val request = call.receive<DoctorNoteCreateRequest>()

                        runCatching {
                            apiUserRepo.createNote(patientId = patientId, input = request.toModel()).toDto()
                        }.onSuccess { dto ->
                            call.respond(HttpStatusCode.Companion.Created, ApiResponse(success = true, data = dto))
                        }.onFailure { error ->
                            call.respond(
                                HttpStatusCode.Companion.InternalServerError,
                                ApiResponse<DoctorNoteResponse>(
                                    success = false,
                                    error = error.message ?: "Failed to create note"
                                )
                            )
                        }
                    }

                    get {
                        val patientId = call.parameters["patientId"]?.toLongOrNull()
                            ?: return@get respondBadRequest(call, "Invalid patient id")
                        val includeInternal = call.request.queryParameters["include_internal"]?.toBoolean() != false

                        runCatching {
                            apiUserRepo.listNotes(patientId, includeInternal).map { it.toDto() }
                        }.onSuccess { list ->
                            call.respond(HttpStatusCode.Companion.OK, ApiResponse(success = true, data = list))
                        }.onFailure { error ->
                            call.respond(
                                HttpStatusCode.Companion.InternalServerError,
                                ApiResponse<List<DoctorNoteResponse>>(
                                    success = false,
                                    error = error.message ?: "Failed to load notes"
                                )
                            )
                        }
                    }
                }
            }

            route("/complaints/{complaintId}") {
                patch {
                    val complaintId = call.parameters["complaintId"]?.toLongOrNull()
                        ?: return@patch respondBadRequest(call, "Invalid complaint id")
                    val request = call.receive<ComplaintPatchRequest>()

                    runCatching {
                        apiUserRepo.patchComplaint(complaintId, request.toModel()).toDto()
                    }.onSuccess { dto ->
                        call.respond(HttpStatusCode.Companion.OK, ApiResponse(success = true, data = dto))
                    }.onFailure { error ->
                        call.respond(
                            HttpStatusCode.Companion.InternalServerError,
                            ApiResponse<ComplaintResponse>(
                                success = false,
                                error = error.message ?: "Failed to update complaint"
                            )
                        )
                    }
                }

                delete {
                    val complaintId = call.parameters["complaintId"]?.toLongOrNull()
                        ?: return@delete respondBadRequest(call, "Invalid complaint id")

                    runCatching { apiUserRepo.deleteComplaint(complaintId) }
                        .onSuccess { deleted ->
                            if (deleted) {
                                call.respond(HttpStatusCode.Companion.OK, ApiResponse(success = true, data = true))
                            } else {
                                call.respond(
                                    HttpStatusCode.Companion.NotFound,
                                    ApiResponse<Boolean>(success = false, error = "Complaint not found")
                                )
                            }
                        }
                        .onFailure { error ->
                            call.respond(
                                HttpStatusCode.Companion.InternalServerError,
                                ApiResponse<Boolean>(
                                    success = false,
                                    error = error.message ?: "Failed to delete complaint"
                                )
                            )
                        }
                }
            }

            route("/notes/{noteId}") {
                patch {
                    val noteId = call.parameters["noteId"]?.toLongOrNull()
                        ?: return@patch respondBadRequest(call, "Invalid note id")
                    val request = call.receive<DoctorNotePatchRequest>()

                    runCatching {
                        apiUserRepo.patchNote(noteId, request.toModel()).toDto()
                    }.onSuccess { dto ->
                        call.respond(HttpStatusCode.Companion.OK, ApiResponse(success = true, data = dto))
                    }.onFailure { error ->
                        call.respond(
                            HttpStatusCode.Companion.InternalServerError,
                            ApiResponse<DoctorNoteResponse>(
                                success = false,
                                error = error.message ?: "Failed to update note"
                            )
                        )
                    }
                }

                delete {
                    val noteId = call.parameters["noteId"]?.toLongOrNull()
                        ?: return@delete respondBadRequest(call, "Invalid note id")

                    runCatching { apiUserRepo.deleteNote(noteId) }
                        .onSuccess { deleted ->
                            if (deleted) {
                                call.respond(HttpStatusCode.Companion.OK, ApiResponse(success = true, data = true))
                            } else {
                                call.respond(
                                    HttpStatusCode.Companion.NotFound,
                                    ApiResponse<Boolean>(success = false, error = "Note not found")
                                )
                            }
                        }
                        .onFailure { error ->
                            call.respond(
                                HttpStatusCode.Companion.InternalServerError,
                                ApiResponse<Boolean>(success = false, error = error.message ?: "Failed to delete note")
                            )
                        }
                }
            }
        }
    }

    private suspend fun respondBadRequest(call: ApplicationCall, message: String) {
        call.respond(HttpStatusCode.Companion.BadRequest, ApiResponse<Unit>(success = false, error = message))
    }

    // --------- mappers (без статусов) ---------

    private fun ComplaintCreateRequest.toModel() = ComplaintIn(
        title = title,
        body = body
    )

    private fun ComplaintPatchRequest.toModel() = ComplaintPatch(
        title = title,
        body = body
    )

    private fun ComplaintOut.toDto() = ComplaintResponse(
        id = id,
        patientId = patientId,
        title = title,
        body = body,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun DoctorNoteCreateRequest.toModel() = NoteIn(
        doctorId = doctorId,
        note = note,
        visibility = visibility.toModel()
    )

    private fun DoctorNotePatchRequest.toModel() = NotePatch(
        note = note,
        visibility = visibility?.toModel()
    )

    private fun NoteOut.toDto() = DoctorNoteResponse(
        id = id,
        patientId = patientId,
        doctorId = doctorId,
        note = note,
        visibility = visibility.toDto(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun NoteVisibilityDto.toModel(): NoteVisibility = when (this) {
        NoteVisibilityDto.INTERNAL -> NoteVisibility.INTERNAL
        NoteVisibilityDto.PATIENT -> NoteVisibility.PATIENT
    }

    private fun NoteVisibility.toDto(): NoteVisibilityDto = when (this) {
        NoteVisibility.INTERNAL -> NoteVisibilityDto.INTERNAL
        NoteVisibility.PATIENT -> NoteVisibilityDto.PATIENT
    }
}