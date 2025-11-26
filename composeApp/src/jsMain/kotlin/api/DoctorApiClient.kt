package api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.interns.project.dto.*

class DoctorApiClient {
    private val client = ApiConfig.httpClient

    suspend fun searchDoctors(filter: DoctorSearchFilterDto): Result<List<DoctorSearchResultDto>> =
        withContext(Dispatchers.Default) {
            try {
                val response = client.get(ApiConfig.Endpoints.DOCTOR_SEARCH) {

                    filter.specializationIds?.forEach { parameter("specialization_ids", it) }
                    filter.city?.let { parameter("city", it) }
                    filter.region?.let { parameter("region", it) }
                    filter.metro?.let { parameter("metro", it) }
                    if (filter.onlineOnly) parameter("online_only", true)
                    filter.minPrice?.let { parameter("min_price", it) }
                    filter.maxPrice?.let { parameter("max_price", it) }
                    filter.minRating?.let { parameter("min_rating", it) }
                    filter.gender?.let { parameter("gender", it) }
                    filter.minAge?.let { parameter("min_age", it) }
                    filter.maxAge?.let { parameter("max_age", it) }
                    filter.minExperience?.let { parameter("min_experience", it) }
                    filter.maxExperience?.let { parameter("max_experience", it) }
                    filter.date?.let { parameter("date", it) }

                    parameter("limit", filter.limit)
                    parameter("offset", filter.offset)
                }

                if (!response.status.isSuccess()) {
                    return@withContext Result.failure(
                        Exception("HTTP error: ${response.status.value}")
                    )
                }

                val apiResp = response.body<ApiResponse<List<DoctorSearchResultDto>>>()

                if (apiResp.success) {
                    Result.success(apiResp.data ?: emptyList())
                } else {
                    Result.failure(Exception(apiResp.error ?: "Unknown doctor search error"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
