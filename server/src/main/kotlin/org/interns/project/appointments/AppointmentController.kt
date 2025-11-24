package org.interns.project.appointments

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.interns.project.dto.ApiResponse
import org.interns.project.dto.AppointmentCreateRequest
import org.interns.project.dto.AppointmentResponse
import org.interns.project.dto.SlotCreateRequest
import org.interns.project.dto.SlotResponse
import org.interns.project.users.model.Appointment
import org.interns.project.users.model.Slot
import org.interns.project.users.repo.ApiUserRepo

class AppointmentController(
    private val apiUserRepo: ApiUserRepo
) {
    fun registerRoutes(route: Route) {
        route.route("/api/doctors/{doctorId}/slots") {
            post {
                val doctorId = call.parameters["doctorId"]?.toLongOrNull()
                    ?: return@post respondBadRequest(call, "Invalid doctor id")
                val request = call.receive<SlotCreateRequest>()

                runCatching { apiUserRepo.createSlot(doctorId, request).toDto() }
                    .onSuccess { slot ->
                        call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = slot))
                    }
                    .onFailure { error ->
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<SlotResponse>(success = false, error = error.message ?: "Failed to create slot")
                        )
                    }
            }

            get {
                val doctorId = call.parameters["doctorId"]?.toLongOrNull()
                    ?: return@get respondBadRequest(call, "Invalid doctor id")
                val date = call.request.queryParameters["date"]

                runCatching { apiUserRepo.listSlots(doctorId, date).map { it.toDto() } }
                    .onSuccess { slots ->
                        call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = slots))
                    }
                    .onFailure { error ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<List<SlotResponse>>(success = false, error = error.message ?: "Failed to load slots")
                        )
                    }
            }

            delete("/{slotId}") {
                val doctorId = call.parameters["doctorId"]?.toLongOrNull()
                    ?: return@delete respondBadRequest(call, "Invalid doctor id")
                val slotId = call.parameters["slotId"]?.toLongOrNull()
                    ?: return@delete respondBadRequest(call, "Invalid slot id")

                runCatching { apiUserRepo.deleteSlot(doctorId, slotId) }
                    .onSuccess { deleted ->
                        if (deleted) {
                            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = true))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Boolean>(success = false, error = "Slot not removable"))
                        }
                    }
                    .onFailure { error ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Boolean>(success = false, error = error.message ?: "Failed to delete slot")
                        )
                    }
            }
        }

        route.route("/api/appointments") {
            post {
                val request = call.receive<AppointmentCreateRequest>()

                runCatching { apiUserRepo.bookAppointment(request).toDto() }
                    .onSuccess { appointment ->
                        call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = appointment))
                    }
                    .onFailure { error ->
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<AppointmentResponse>(success = false, error = error.message ?: "Slot not available")
                        )
                    }
            }

            post("/{appointmentId}/cancel") {
                val appointmentId = call.parameters["appointmentId"]?.toLongOrNull()
                    ?: return@post respondBadRequest(call, "Invalid appointment id")

                runCatching { apiUserRepo.cancelAppointment(appointmentId) }
                    .onSuccess { canceled ->
                        if (canceled) {
                            call.respond(HttpStatusCode.NoContent, ApiResponse<Boolean>(success = true, data = true))
                        } else {
                            call.respond(HttpStatusCode.NotFound, ApiResponse<Boolean>(success = false, error = "Appointment not found"))
                        }
                    }
                    .onFailure { error ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Boolean>(success = false, error = error.message ?: "Failed to cancel appointment")
                        )
                    }
            }
        }
    }

    private suspend fun respondBadRequest(call: ApplicationCall, message: String) {
        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = message))
    }

    private fun Slot.toDto() = SlotResponse(
        id = id,
        doctorId = doctorId,
        startTime = startTime?.toString() ?: "",
        endTime = endTime?.toString() ?: "",
        duration = durationMinutes,
        isBooked = isBooked,
        createdAt = createdAt?.toString() ?: "",
        updatedAt = updatedAt?.toString() ?: ""
    )

    private fun Appointment.toDto() = AppointmentResponse(
        id = id,
        slotId = slotId,
        clientId = clientId,
        status = status,
        comments = comments,
        createdAt = createdAt?.toString() ?: "",
        updatedAt = updatedAt?.toString() ?: "",
        canceledAt = canceledAt?.toString(),
        completedAt = completedAt?.toString(),
        appointmentTypeId = appointmentTypeId
    )
}