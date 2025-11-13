package api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import org.interns.project.dto.ApiResponse
import org.interns.project.dto.ClientProfileDto
import org.interns.project.dto.ComplaintCreateRequest
import org.interns.project.dto.ComplaintPatchRequest
import org.interns.project.dto.ComplaintResponse
import org.interns.project.dto.DoctorNoteCreateRequest
import org.interns.project.dto.DoctorNotePatchRequest
import org.interns.project.dto.DoctorNoteResponse
import org.interns.project.dto.UserResponseDto

class PatientApiClient {
    private val client = ApiConfig.httpClient

    // ---- helpers ----

    private suspend inline fun <reified T> parseOne(
        response: HttpResponse,
        emptyMessage: String,
        failureMessage: String
    ): T {
        if (!response.status.isSuccess()) {
            throw IllegalStateException("HTTP error: ${response.status.value}")
        }
        val body = response.body<ApiResponse<T>>()
        if (body.success) {
            return body.data ?: throw IllegalStateException(emptyMessage)
        } else {
            throw IllegalStateException(body.error ?: failureMessage)
        }
    }

    private suspend inline fun <reified T> parseList(
        response: HttpResponse,
        failureMessage: String
    ): List<T> {
        if (!response.status.isSuccess()) {
            throw IllegalStateException("HTTP error: ${response.status.value}")
        }
        val body = response.body<ApiResponse<List<T>>>()
        if (body.success) {
            return body.data ?: emptyList()
        } else {
            throw IllegalStateException(body.error ?: failureMessage)
        }
    }

    private suspend fun parseDelete(response: HttpResponse): Boolean {
        return when {
            response.status == HttpStatusCode.NoContent -> true
            response.status == HttpStatusCode.NotFound -> false
            response.status.isSuccess() -> response.body<ApiResponse<Boolean>>().success
            else -> throw IllegalStateException("HTTP error: ${response.status.value}")
        }
    }

    private suspend inline fun <reified T> parseNullable(
        response: HttpResponse,
        failureMessage: String
    ): T? {
        if (response.status == HttpStatusCode.NotFound) {
            return null
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("HTTP error: ${response.status.value}")
        }
        val body = response.body<ApiResponse<T?>>()
        if (body.success) {
            return body.data
        } else {
            throw IllegalStateException(body.error ?: failureMessage)
        }
    }

    // ---- complaints ----

    suspend fun listComplaints(
        patientId: Long
    ): Result<List<ComplaintResponse>> = runCatching {
        val response = client.get(ApiConfig.Endpoints.patientComplaints(patientId))
        parseList<ComplaintResponse>(response, "Failed to load complaints")
    }

    suspend fun createComplaint(
        patientId: Long,
        request: ComplaintCreateRequest
    ): Result<ComplaintResponse> = runCatching {
        val response = client.post(ApiConfig.Endpoints.patientComplaints(patientId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        parseOne(response, emptyMessage = "Empty complaint response", failureMessage = "Failed to create complaint")
    }

    suspend fun updateComplaint(
        complaintId: Long,
        request: ComplaintPatchRequest
    ): Result<ComplaintResponse> = runCatching {
        val response = client.patch(ApiConfig.Endpoints.patientComplaint(complaintId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        parseOne(response, emptyMessage = "Empty complaint response", failureMessage = "Failed to update complaint")
    }

    suspend fun deleteComplaint(complaintId: Long): Result<Boolean> = runCatching {
        val response = client.delete(ApiConfig.Endpoints.patientComplaint(complaintId))
        parseDelete(response)
    }

    // ---- doctor notes ----

    suspend fun listNotes(
        patientId: Long,
        includeInternal: Boolean = true
    ): Result<List<DoctorNoteResponse>> = runCatching {
        val response = client.get(ApiConfig.Endpoints.patientNotes(patientId)) {
            parameter("include_internal", includeInternal)
        }
        parseList<DoctorNoteResponse>(response, "Failed to load notes")
    }

    suspend fun createNote(
        patientId: Long,
        request: DoctorNoteCreateRequest
    ): Result<DoctorNoteResponse> = runCatching {
        val response = client.post(ApiConfig.Endpoints.patientNotes(patientId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        parseOne(response, emptyMessage = "Empty note response", failureMessage = "Failed to create note")
    }

    suspend fun updateNote(
        noteId: Long,
        request: DoctorNotePatchRequest
    ): Result<DoctorNoteResponse> = runCatching {
        val response = client.patch(ApiConfig.Endpoints.patientNote(noteId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        parseOne(response, emptyMessage = "Empty note response", failureMessage = "Failed to update note")
    }

    suspend fun deleteNote(noteId: Long): Result<Boolean> = runCatching {
        val response = client.delete(ApiConfig.Endpoints.patientNote(noteId))
        parseDelete(response)
    }

    // ---- patients ----

    suspend fun listPatients(): Result<List<UserResponseDto>> = runCatching {
        val response = client.get(ApiConfig.Endpoints.users()) {
            parameter("role", "CLIENT")
        }
        parseList<UserResponseDto>(response, "Failed to load patients")
    }

    suspend fun getPatientProfile(userId: Long): Result<UserResponseDto> = runCatching {
        val response = client.get(ApiConfig.Endpoints.userProfile(userId))
        parseOne(response, emptyMessage = "Empty patient profile", failureMessage = "Failed to load patient profile")
    }

    suspend fun getClientProfile(userId: Long): Result<ClientProfileDto?> = runCatching {
        val response = client.get(ApiConfig.Endpoints.clientByUser(userId))
        parseNullable<ClientProfileDto>(response, failureMessage = "Failed to load client profile")
    }
}
