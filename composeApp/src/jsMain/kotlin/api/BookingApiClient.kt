package api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.interns.project.dto.*

class BookingApiClient {
    private val client = ApiConfig.httpClient
    val BASE_URL = "http://localhost:8001"

    // ---- slots ----

    suspend fun listDoctorSlots(doctorId: Long, date: String? = null): Result<List<SlotResponse>> =
        runCatching {
            val response = client.get("$BASE_URL/doctors/$doctorId/slots") {
                date?.let { parameter("date", it) }
            }
            response.body()
        }

    suspend fun createSlot(doctorId: Long, req: SlotCreateRequest): Result<SlotResponse> =
        runCatching {
            val response = client.post("$BASE_URL/doctors/$doctorId/slots") {
                setBody(req)
            }
            response.body()
        }

    suspend fun deleteSlot(doctorId: Long, slotId: Long): Result<Boolean> =
        runCatching {
            val response = client.delete("$BASE_URL/doctors/$doctorId/slots/$slotId")
            response.status.isSuccess()
        }

    // ---- appointments ----

    suspend fun bookAppointment(req: AppointmentCreateRequest): Result<AppointmentResponse> =
        runCatching {
            val response = client.post("$BASE_URL/appointments") {
                setBody(req)
            }
            response.body()
        }

    suspend fun cancelAppointment(id: Long): Result<Boolean> =
        runCatching {
            val response = client.post("$BASE_URL/appointments/$id/cancel")
            response.status.isSuccess()
        }
}
