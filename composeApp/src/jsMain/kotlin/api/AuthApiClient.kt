package api

import api.ApiConfig.Endpoints.EMAIL_START_VERIFICATION
import api.ApiConfig.Endpoints.EMAIL_VERIFY
import api.ApiConfig.Endpoints.LOGIN
import api.ApiConfig.Endpoints.PASSWORD_FORGOT
import api.ApiConfig.Endpoints.PASSWORD_RESET
import api.ApiConfig.Endpoints.REGISTER
import i18n.t
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.interns.project.dto.*

class AuthApiClient {
    private val client = ApiConfig.httpClient

    private fun httpError(response: HttpResponse): Exception {
        return Exception(t("api.error.http").replace("{code}", response.status.value.toString()))
    }

    private fun HttpRequestBuilder.jsonBody(payload: Any) {
        headers { append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) }
        setBody(payload)
    }

    suspend fun login(request: LoginRequest): Result<LoginResponse> = withContext(Dispatchers.Default) {
        try {
            val response = client.post(LOGIN) { jsonBody(request) }

            if (response.status.isSuccess()) {
                val loginResponse = response.body<ApiResponse<LoginResponse>>()
                if (loginResponse.success) {
                    loginResponse.data?.let { Result.success(it) } ?:
                    Result.failure(Exception(t("auth.error.login")))
                } else {
                    Result.failure(Exception(loginResponse.error ?: t("auth.error/login")))
                }
            } else {
                Result.failure(httpError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(request: RegisterRequest): Result<RegisterResponse> = withContext(Dispatchers.Default) {
        try {
            val response = client.post(REGISTER) {jsonBody(request) }

            if (response.status.isSuccess()) {
                val registerResponse = response.body<ApiResponse<RegisterResponse>>()
                if (registerResponse.success) {
                    registerResponse.data?.let { Result.success(it) } ?:
                    Result.failure(Exception(t("auth.error.registration")))
                } else {
                    Result.failure(Exception(registerResponse.error ?: t("auth.error.registration")))
                }
            } else {
                Result.failure(httpError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startEmailVerification(email: String): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            val response = client.post(EMAIL_START_VERIFICATION) {
                jsonBody(mapOf("email" to email))
            }

            if (response.status.isSuccess()) {
                Result.success(true)
            } else {
                Result.failure(httpError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyEmail(token: String): Result<VerifyEmailResponse> = withContext(Dispatchers.Default) {
        try {
            val response = client.post(EMAIL_VERIFY) {
                jsonBody(VerifyEmailRequest(token))
            }

            if (response.status.isSuccess()) {
                val verifyResponse = response.body<ApiResponse<VerifyEmailResponse>>()
                if (verifyResponse.success) {
                    verifyResponse.data?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception(t("confirm.error.generic")))
                } else {
                    Result.failure(Exception(verifyResponse.error ?: t("confirm.error.generic")))
                }
            } else {
                Result.failure(httpError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestPasswordReset(email: String): Result<RequestPasswordResetResponse> = withContext(Dispatchers.Default) {
        try {
            val response = client.post(PASSWORD_FORGOT) {
                jsonBody(RequestPasswordResetRequest(email))
            }

            if (response.status.isSuccess()) {
                val resetResponse = response.body<ApiResponse<RequestPasswordResetResponse>>()
                if (resetResponse.success) {
                    resetResponse.data?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception(t("resetPassword.stub.info")))
                } else {
                    Result.failure(Exception(resetResponse.error ?: t("resetPassword.stub.info")))
                }
            } else {
                Result.failure(httpError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(token: String, newPassword: String): Result<ResetPasswordResponse> = withContext(Dispatchers.Default) {
        try {
            val response = client.post(PASSWORD_RESET) {
                jsonBody(ResetPasswordRequest(token, newPassword))
            }

            if (response.status.isSuccess()) {
                val resetResponse = response.body<ApiResponse<ResetPasswordResponse>>()
                if (resetResponse.success) {
                    resetResponse.data?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception(t("resetPassword.stub.info")))
                } else {
                    Result.failure(Exception(resetResponse.error ?: t("resetPassword.stub.info")))
                }
            } else {
                Result.failure(httpError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        ApiConfig.clearToken()
    }
}