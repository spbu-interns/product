package api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.interns.project.dto.*

class AuthApiClient {
    private val client = ApiConfig.httpClient

    suspend fun login(request: LoginRequest): Result<LoginResponse> = withContext(Dispatchers.Default) {
        try {
            val response = client.post(ApiConfig.Endpoints.LOGIN) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                val loginResponse = response.body<ApiResponse<LoginResponse>>()
                if (loginResponse.success) {
                    loginResponse.data?.let { loginData ->
                        ApiConfig.setToken(loginData.token)
                        Result.success(loginData)
                    } ?: Result.failure(Exception("No login data returned"))
                } else {
                    Result.failure(Exception(loginResponse.error ?: "Login failed"))
                }
            } else {
                Result.failure(Exception("HTTP error: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(request: RegisterRequest): Result<RegisterResponse> = withContext(Dispatchers.Default) {
        try {
            val response = client.post(ApiConfig.Endpoints.REGISTER) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                val registerResponse = response.body<ApiResponse<RegisterResponse>>()
                if (registerResponse.success) {
                    registerResponse.data?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("No registration data returned"))
                } else {
                    Result.failure(Exception(registerResponse.error ?: "Registration failed"))
                }
            } else {
                Result.failure(Exception("HTTP error: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startEmailVerification(email: String): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            val response = client.post(ApiConfig.Endpoints.EMAIL_START_VERIFICATION) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email))
            }

            if (response.status.isSuccess()) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP error: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyEmail(token: String): Result<VerifyEmailResponse> = withContext(Dispatchers.Default) {
        try {
            val response = client.post(ApiConfig.Endpoints.EMAIL_VERIFY) {
                contentType(ContentType.Application.Json)
                setBody(VerifyEmailRequest(token))
            }

            if (response.status.isSuccess()) {
                val verifyResponse = response.body<ApiResponse<VerifyEmailResponse>>()
                if (verifyResponse.success) {
                    verifyResponse.data?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("No verification data returned"))
                } else {
                    Result.failure(Exception(verifyResponse.error ?: "Email verification failed"))
                }
            } else {
                Result.failure(Exception("HTTP error: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestPasswordReset(email: String): Result<RequestPasswordResetResponse> = withContext(Dispatchers.Default) {
        try {
            val response = client.post(ApiConfig.Endpoints.PASSWORD_FORGOT) {
                contentType(ContentType.Application.Json)
                setBody(RequestPasswordResetRequest(email))
            }

            if (response.status.isSuccess()) {
                val resetResponse = response.body<ApiResponse<RequestPasswordResetResponse>>()
                if (resetResponse.success) {
                    resetResponse.data?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("No reset request data returned"))
                } else {
                    Result.failure(Exception(resetResponse.error ?: "Password reset request failed"))
                }
            } else {
                Result.failure(Exception("HTTP error: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(token: String, newPassword: String): Result<ResetPasswordResponse> = withContext(Dispatchers.Default) {
        try {
            val response = client.post(ApiConfig.Endpoints.PASSWORD_RESET) {
                contentType(ContentType.Application.Json)
                setBody(ResetPasswordRequest(token, newPassword))
            }

            if (response.status.isSuccess()) {
                val resetResponse = response.body<ApiResponse<ResetPasswordResponse>>()
                if (resetResponse.success) {
                    resetResponse.data?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("No reset data returned"))
                } else {
                    Result.failure(Exception(resetResponse.error ?: "Password reset failed"))
                }
            } else {
                Result.failure(Exception("HTTP error: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        ApiConfig.clearToken()
    }
}