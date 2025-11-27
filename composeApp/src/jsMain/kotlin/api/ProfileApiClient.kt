package api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.interns.project.dto.ApiResponse
import org.interns.project.dto.*

class ProfileApiClient {
    private val client = ApiConfig.httpClient

    suspend fun updateProfile(
        userId: Long,
        patch: ProfileUpdateDto
    ): Result<UserResponseDto> = withContext(Dispatchers.Default) {
        try {
            val response = client.patch(ApiConfig.Endpoints.userProfile(userId)) {
                contentType(ContentType.Application.Json)
                setBody(patch)
            }

            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    Exception("HTTP error: ${response.status.value}")
                )
            }

            val apiResp = response.body<ApiResponse<UserResponseDto>>()

            if (apiResp.success) {
                val profile = apiResp.data
                    ?: return@withContext Result.failure(Exception("Empty response data"))

                Result.success(profile)  // ← НЕ null
            } else {
                Result.failure(Exception(apiResp.error ?: "Unknown profile update error"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
