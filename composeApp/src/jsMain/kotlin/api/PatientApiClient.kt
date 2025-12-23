package api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import org.interns.project.dto.ApiResponse
import org.interns.project.dto.AppointmentDto
import org.interns.project.dto.AppointmentReviewDto
import org.interns.project.dto.AppointmentReviewRequest
import org.interns.project.dto.AppointmentWithReviewDto
import org.interns.project.dto.ClientProfileDto
import org.interns.project.dto.ComplaintCreateRequest
import org.interns.project.dto.ComplaintPatchRequest
import org.interns.project.dto.ComplaintResponse
import org.interns.project.dto.DoctorNoteResponse
import org.interns.project.dto.FullUserProfileDto
import org.interns.project.dto.MedicalRecordInDto
import org.interns.project.dto.MedicalRecordOutDto
import org.interns.project.dto.PatientDashboardDto
import org.interns.project.dto.UserResponseDto

class PatientApiClient {
    private val client = ApiConfig.httpClient
    private val reviewsBase = "http://localhost:8001"

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

//    suspend fun listMedicalRecords(
//        patientId: Long,
//        includeInternal: Boolean = true
//    ): Result<List<MedicalRecordOutDto>> = runCatching {
//        val response = client.get(ApiConfig.Endpoints.clientMedicalRecords(patientId)) {
//            parameter("include_internal", includeInternal)
//        }
//        parseList<MedicalRecordOutDto>(response, "Failed to load medical records")
//    }

    suspend fun createMedicalRecord(
        patientId: Long,
        request: MedicalRecordInDto
    ): Result<MedicalRecordOutDto> = runCatching {
        val response = client.post(ApiConfig.Endpoints.clientMedicalRecords(patientId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        parseOne(response, emptyMessage = "Empty medical record response", failureMessage = "Failed to create medical record")
    }

    suspend fun updateMedicalRecord(
        noteId: Long,
        request: MedicalRecordInDto
    ): Result<MedicalRecordOutDto> = runCatching {
        val response = client.patch(ApiConfig.Endpoints.clientMedicalRecord(noteId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        parseOne(response, emptyMessage = "Empty note response", failureMessage = "Failed to update note")
    }

    suspend fun deleteMedicalRecord(noteId: Long): Result<Boolean> = runCatching {
        val response = client.delete(ApiConfig.Endpoints.clientMedicalRecord(noteId))
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
        if (response.status == HttpStatusCode.NotFound) {
            throw IllegalStateException("Пациент не найден")
        }
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

    suspend fun getUpcomingAppointments(clientId: Long): Result<List<AppointmentDto>> = runCatching {
        val response = client.get("${ApiConfig.BASE_URL}/clients/$clientId/appointments")
        if (response.status.isSuccess()) {
            val appointments = response.body<List<AppointmentDto>>()
            val upcoming = appointments.filter { it.status == "BOOKED" }
            upcoming
        } else {
            emptyList()
        }
    }

    suspend fun getAppointmentHistory(clientId: Long): Result<List<AppointmentDto>> = runCatching {
        val response = client.get("${ApiConfig.BASE_URL}/clients/$clientId/appointments")
        if (response.status.isSuccess()) {
            val appointments = response.body<List<AppointmentDto>>()
            val history = appointments.filter { it.status in listOf("COMPLETED", "CANCELED", "NO_SHOW") }
            history
        } else {
            emptyList()
        }
    }

    suspend fun getAppointmentHistoryWithReviews(clientId: Long): Result<List<AppointmentWithReviewDto>> =
        runCatching {
            val response = client.get("$reviewsBase/clients/$clientId/appointments/history")
            if (response.status.isSuccess()) {
                response.body()
            } else {
                emptyList()
            }
        }

    suspend fun getPendingReviewAppointments(clientId: Long): Result<List<AppointmentWithReviewDto>> =
        runCatching {
            val response = client.get("$reviewsBase/clients/$clientId/appointments/pending-reviews")
            if (response.status.isSuccess()) {
                response.body()
            } else {
                emptyList()
            }
        }

    suspend fun getAppointmentReview(appointmentId: Long): Result<AppointmentReviewDto?> = runCatching {
        val response = client.get("$reviewsBase/appointments/$appointmentId/review")
        if (response.status.isSuccess()) {
            response.body<AppointmentReviewDto?>()
        } else {
            null
        }
    }

    suspend fun saveAppointmentReview(
        appointmentId: Long,
        request: AppointmentReviewRequest
    ): Result<AppointmentReviewDto> = runCatching {
        val response = client.put("$reviewsBase/appointments/$appointmentId/review") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        response.body()
    }

    suspend fun getMedicalRecords(clientId: Long): Result<List<MedicalRecordOutDto>> = runCatching {
        val response = client.get(ApiConfig.Endpoints.clientMedicalRecords(clientId))
        val responseBody = response.body<String>()
        println("=== DEBUG getMedicalRecords ===")
        println("Status: ${response.status}")
        println("Response body: $responseBody")
        println("==============================")
        if (!response.status.isSuccess()) {
            val apiError = runCatching { response.body<ApiResponse<List<MedicalRecordOutDto>>>() }
                .getOrNull()
                ?.error
            throw IllegalStateException(apiError ?: "HTTP error: ${response.status.value}")
        }

        val apiResponse = runCatching { response.body<ApiResponse<List<MedicalRecordOutDto>>>() }.getOrNull()
        if (apiResponse != null) {
            if (apiResponse.success) {
                apiResponse.data ?: emptyList()
            } else {
                throw IllegalStateException(apiResponse.error ?: "Failed to load medical records")
            }
        } else {
            response.body()
        }
    }

    suspend fun downloadMedicalRecordPdf(clientId: Long, recordId: Long): Result<ByteArray> = runCatching {
        val response = client.get(
            "${ApiConfig.BASE_URL}/clients/$clientId/medical-documents/$recordId/download"
        )

        if (!response.status.isSuccess()) {
            throw IllegalStateException("Ошибка загрузки PDF: ${response.status}")
        }

        response.body()
    }

    suspend fun getAppointmentsCount(clientId: Long): Result<Int> = runCatching {
        val result = getUpcomingAppointments(clientId)
        result.getOrThrow().size
    }

    suspend fun getMedicalRecordsCount(clientId: Long): Result<Int> = runCatching {
        val result = getMedicalRecords(clientId)
        result.getOrThrow().size
    }


    suspend fun getNextAppointment(clientId: Long): Result<AppointmentDto?> = runCatching {
        val result = getUpcomingAppointments(clientId)
        val upcoming = result.getOrThrow()
        upcoming.minByOrNull { it.createdAt }
    }

    suspend fun getRecentMedicalRecords(clientId: Long, limit: Int = 3): Result<List<MedicalRecordOutDto>> = runCatching {
        val result = getMedicalRecords(clientId)
        result.getOrThrow().take(limit)
    }


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

    suspend fun getPatientDashboardData(userId: Long): Result<PatientDashboardDto> = runCatching {
        val fullProfile = getFullUserProfile(userId).getOrThrow()
            ?: throw IllegalStateException("User profile not found")

        val clientId = getClientId(userId).getOrThrow()
            ?: throw IllegalStateException("Client not found")

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


    suspend fun getPatientStats(userId: Long): Result<PatientStats> = runCatching {
        val clientId = getClientId(userId).getOrThrow()
            ?: throw IllegalStateException("Client not found for user $userId")

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

    data class PatientStats(
        val upcomingAppointments: Int,
        val totalAppointments: Int,
        val uniqueDoctors: Int,
        val medicalRecords: Int
    )
}
