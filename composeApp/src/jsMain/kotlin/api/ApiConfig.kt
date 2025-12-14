package api

import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

external object localStorage {
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
    fun removeItem(key: String)
}

object ApiConfig {
    const val BASE_URL = "http://localhost:8000"

    object Endpoints {
        const val LOGIN = "$BASE_URL/api/auth/login"
        const val REGISTER = "$BASE_URL/api/auth/register"
        const val EMAIL_VERIFY = "$BASE_URL/api/auth/email/verify"
        const val PASSWORD_FORGOT = "$BASE_URL/api/auth/password/forgot"
        const val PASSWORD_RESET = "$BASE_URL/api/auth/password/reset"
        const val EMAIL_START_VERIFICATION = "$BASE_URL/api/auth/email/start"
        const val DOCTOR_SEARCH = "$BASE_URL/doctors/search"

        fun patientComplaints(patientId: Long) = "$BASE_URL/api/patient/$patientId/complaints"
        fun patientComplaint(complaintId: Long) = "$BASE_URL/api/patient/complaints/$complaintId"
        fun clientMedicalRecords(clientId: Long) = "$BASE_URL/api/clients/$clientId/medical-records"
        fun clientMedicalRecord(medicalRecordId: Long) = "$BASE_URL/api/clients/medical-records/$medicalRecordId"
        fun users() = "$BASE_URL/api/users"
        fun userProfile(userId: Long) = "$BASE_URL/api/users/$userId/profile"
        fun clientByUser(userId: Long) = "$BASE_URL/api/clients/by-user/$userId"
    }

    const val TOKEN_STORAGE_KEY = "auth_token"
    const val SESSION_STORAGE_KEY = "session_state"

    val httpClient = HttpClient(Js) {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = false
                    isLenient = true
                    ignoreUnknownKeys = true
                }
            )
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            ApiConfig.getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    fun getToken(): String? =
        localStorage.getItem(TOKEN_STORAGE_KEY)

    fun setToken(token: String) =
        localStorage.setItem(TOKEN_STORAGE_KEY, token)

    fun clearToken() =
        localStorage.removeItem(TOKEN_STORAGE_KEY)

    fun getSessionData(): String? =
        localStorage.getItem(SESSION_STORAGE_KEY)

    fun setSessionData(data: String) =
        localStorage.setItem(SESSION_STORAGE_KEY, data)

    fun clearSessionData() =
        localStorage.removeItem(SESSION_STORAGE_KEY)

    fun isAuthenticated(): Boolean =
        getToken() != null
}
