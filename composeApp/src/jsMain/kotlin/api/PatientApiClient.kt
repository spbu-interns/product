package api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import org.interns.project.dto.ApiResponse
import org.interns.project.dto.AppointmentDto
import org.interns.project.dto.ClientProfileDto
import org.interns.project.dto.ComplaintCreateRequest
import org.interns.project.dto.ComplaintPatchRequest
import org.interns.project.dto.ComplaintResponse
import org.interns.project.dto.DoctorNoteCreateRequest
import org.interns.project.dto.DoctorNotePatchRequest
import org.interns.project.dto.DoctorNoteResponse
import org.interns.project.dto.FullUserProfileDto
import org.interns.project.dto.MedicalRecordDto
import org.interns.project.dto.PatientDashboardDto
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

    suspend fun getFullUserProfile(userId: Long): Result<FullUserProfileDto?> = runCatching {
        val response = client.get("${ApiConfig.BASE_URL}/api/users/$userId/full")
        parseNullable<FullUserProfileDto>(response, "Failed to load full user profile")
    }

    // Получение предстоящих записей через существующий endpoint
    suspend fun getUpcomingAppointments(clientId: Long): Result<List<AppointmentDto>> = runCatching {
        val response = client.get("${ApiConfig.BASE_URL}/clients/$clientId/appointments")
        if (response.status.isSuccess()) {
            val appointments = response.body<List<AppointmentDto>>()
            // Фильтруем предстоящие записи на фронте
            val upcoming = appointments.filter { it.status == "BOOKED" }
            upcoming
        } else {
            emptyList()
        }
    }

    // Получение истории записей
    suspend fun getAppointmentHistory(clientId: Long): Result<List<AppointmentDto>> = runCatching {
        val response = client.get("${ApiConfig.BASE_URL}/clients/$clientId/appointments")
        if (response.status.isSuccess()) {
            val appointments = response.body<List<AppointmentDto>>()
            // Фильтруем историю на фронте
            val history = appointments.filter { it.status in listOf("COMPLETED", "CANCELED", "NO_SHOW") }
            history
        } else {
            emptyList()
        }
    }

    // Получение медицинских записей
    suspend fun getMedicalRecords(clientId: Long): Result<List<MedicalRecordDto>> = runCatching {
        val response = client.get("${ApiConfig.BASE_URL}/clients/$clientId/medical-records")
        if (response.status.isSuccess()) {
            response.body<List<MedicalRecordDto>>()
        } else {
            emptyList()
        }
    }

    // Получение количества предстоящих записей
    suspend fun getAppointmentsCount(clientId: Long): Result<Int> = runCatching {
        val result = getUpcomingAppointments(clientId)
        result.getOrThrow().size
    }

    // Получение количества медицинских записей
    suspend fun getMedicalRecordsCount(clientId: Long): Result<Int> = runCatching {
        val result = getMedicalRecords(clientId)
        result.getOrThrow().size
    }

    // Получение следующей записи
    suspend fun getNextAppointment(clientId: Long): Result<AppointmentDto?> = runCatching {
        val result = getUpcomingAppointments(clientId)
        val upcoming = result.getOrThrow()
        upcoming.minByOrNull { it.createdAt }
    }

    // Получение последних медицинских записей
    suspend fun getRecentMedicalRecords(clientId: Long, limit: Int = 3): Result<List<MedicalRecordDto>> = runCatching {
        val result = getMedicalRecords(clientId)
        result.getOrThrow().take(limit)
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    // Получение clientId по userId
    suspend fun getClientId(userId: Long): Result<Long?> = runCatching {
        val response = client.get(ApiConfig.Endpoints.clientByUser(userId))
        if (response.status.isSuccess()) {
            val apiResponse = response.body<ApiResponse<ClientProfileDto?>>()
            if (apiResponse.success) {
                apiResponse.data?.id
            } else {
                null
            }
        } else {
            null
        }
    }

    // Получение всех данных для дашборда
    suspend fun getPatientDashboardData(userId: Long): Result<PatientDashboardDto> = runCatching {
        // Получаем полный профиль
        val fullProfile = getFullUserProfile(userId).getOrThrow()
            ?: throw IllegalStateException("User profile not found")

        // Получаем clientId
        val clientId = getClientId(userId).getOrThrow()
            ?: throw IllegalStateException("Client not found")

        // Получаем дополнительные данные
        val upcomingCount = getAppointmentsCount(clientId).getOrThrow()
        val recordsCount = getMedicalRecordsCount(clientId).getOrThrow()
        val nextAppointment = getNextAppointment(clientId).getOrThrow()
        val recentRecords = getRecentMedicalRecords(clientId).getOrThrow()

        PatientDashboardDto(
            profile = fullProfile.user,
            clientInfo = fullProfile.client,
            upcomingAppointmentsCount = upcomingCount,
            medicalRecordsCount = recordsCount,
            nextAppointment = nextAppointment,
            recentMedicalRecords = recentRecords
        )
    }


    // Получение статистики для дашборда
    suspend fun getPatientStats(userId: Long): Result<PatientStats> = runCatching {
        // Получаем clientId
        val clientId = getClientId(userId).getOrThrow()
            ?: throw IllegalStateException("Client not found for user $userId")

        // Параллельно загружаем все данные
        val upcomingAppointments = getUpcomingAppointments(clientId).getOrThrow()
        val allAppointments = getAllAppointments(clientId).getOrThrow()
        val medicalRecords = getMedicalRecords(clientId).getOrThrow()

        val upcomingCount = upcomingAppointments.size
        val totalAppointments = allAppointments.size
        val uniqueDoctors = allAppointments.map { it.id }.distinct().size
        val medicalRecordsCount = medicalRecords.size

        PatientStats(
            upcomingAppointments = upcomingCount,
            totalAppointments = totalAppointments,
            uniqueDoctors = uniqueDoctors,
            medicalRecords = medicalRecordsCount
        )
    }

    // Получение всех записей (для статистики)
    suspend fun getAllAppointments(clientId: Long): Result<List<AppointmentDto>> = runCatching {
        val response = client.get("${ApiConfig.BASE_URL}/clients/$clientId/appointments")
        if (response.status.isSuccess()) {
            response.body<List<AppointmentDto>>()
        } else {
            emptyList()
        }
    }

    suspend fun getPatientDashboard(userId: Long): Result<FullUserProfileDto> = runCatching {
        val response = client.get("${ApiConfig.BASE_URL}/api/users/$userId/dashboard")

        if (!response.status.isSuccess()) {
            throw IllegalStateException("HTTP error: ${response.status.value}")
        }

        val body = response.body<ApiResponse<FullUserProfileDto>>()
        if (body.success) {
            body.data ?: throw IllegalStateException("Empty dashboard response")
        } else {
            throw IllegalStateException(body.error ?: "Failed to load dashboard data")
        }
    }
    // DTO для статистики
    data class PatientStats(
        val upcomingAppointments: Int,
        val totalAppointments: Int,
        val uniqueDoctors: Int,
        val medicalRecords: Int
    )
}
