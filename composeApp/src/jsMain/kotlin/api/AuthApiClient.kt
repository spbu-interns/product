package api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.interns.project.dto.*

class UnverifiedEmailException(
    val email: String,
    val accountType: String?,
    message: String
) : Exception(message)

class AuthApiClient {
    private val client = ApiConfig.httpClient

    suspend fun login(request: LoginRequest): Result<LoginResponse> = withContext(Dispatchers.Default) {
        try {
            val response = client.post(ApiConfig.Endpoints.LOGIN) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val status = response.status
            val apiResponse = runCatching { response.body<ApiResponse<LoginResponse>>() }.getOrNull()
            val rawError = apiResponse?.error?.trim()

            val isEmailNotVerified =
                rawError.equals("EMAIL_NOT_VERIFIED", ignoreCase = true) ||
                        (rawError?.contains("email not verified", ignoreCase = true) == true)

            // 1. Явный случай: сервер сказал 403 + EMAIL_NOT_VERIFIED
            if (status == HttpStatusCode.Forbidden && isEmailNotVerified) {
                return@withContext Result.failure(
                    UnverifiedEmailException(
                        email = request.email,
                        accountType = request.accountType.takeIf { it.isNotBlank() },
                        message = rawError ?: "Email not verified"
                    )
                )
            }

            // 2. HTTP 2xx
            if (status.isSuccess()) {
                if (apiResponse == null) {
                    return@withContext Result.failure(Exception("Empty response from login endpoint"))
                }

                // 2a. success=false в теле, но HTTP 200–299
                if (!apiResponse.success) {
                    if (isEmailNotVerified) {
                        return@withContext Result.failure(
                            UnverifiedEmailException(
                                email = request.email,
                                accountType = request.accountType.takeIf { it.isNotBlank() },
                                message = rawError ?: "Email not verified"
                            )
                        )
                    }

                    val msg = rawError ?: "Login failed"
                    return@withContext Result.failure(Exception(msg))
                }

                // 2b. всё хорошо
                val data = apiResponse.data
                    ?: return@withContext Result.failure(Exception("No login data returned"))

                data.token?.let { ApiConfig.setToken(it) }
                return@withContext Result.success(data)
            } else {
                // 3. Любые другие HTTP-ошибки
                val message = rawError ?: "HTTP error: ${status.value}"

                // ⬇️ Добавь эту проверку прямо сюда:
                if ((status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden) &&
                    (message.contains("не подтверж", ignoreCase = true) || message.contains("not verified", ignoreCase = true))
                ) {
                    return@withContext Result.failure(
                        UnverifiedEmailException(
                            email = request.email,
                            accountType = request.accountType.takeIf { it.isNotBlank() },
                            message = message
                        )
                    )
                }

                return@withContext Result.failure(Exception(message))
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
                val apiResponse = runCatching { response.body<ApiResponse<RegisterResponse>>() }.getOrNull()
                val apiError = apiResponse?.error?.takeIf { it.isNotBlank() }

                val errorBody = runCatching { response.body<ApiResponse<RegisterResponse>>() }.getOrNull()

                val message = errorBody?.error ?: when (response.status) {
                    HttpStatusCode.Conflict -> "Пользователь с таким email уже существует"
                    HttpStatusCode.BadRequest -> apiError ?: "Некорректные данные для регистрации"
                    else -> apiError ?: "Ошибка регистрации: ${response.status.value}"
                }

                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Отправка письма с кодом подтверждения.
     * 404 от сервера = либо юзер не найден, либо уже подтверждён.
     */
    suspend fun startEmailVerification(email: String): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            val response = client.post(ApiConfig.Endpoints.EMAIL_START_VERIFICATION) {
                contentType(ContentType.Application.Json)
                setBody(RequestPasswordResetRequest(email)) // тот же DTO, что и на сервере
            }

            if (response.status.isSuccess()) {
                Result.success(true)
            } else {
                val api = runCatching { response.body<ApiResponse<VerifyEmailResponse>>() }.getOrNull()
                val rawError = api?.error

                val message = when (response.status) {
                    HttpStatusCode.NotFound ->
                        rawError ?: "Не удалось отправить письмо: пользователь не найден или email уже подтверждён."
                    HttpStatusCode.BadRequest ->
                        rawError ?: "Некорректный запрос при отправке письма подтверждения."
                    else ->
                        rawError ?: "Ошибка при отправке письма: ${response.status.value}"
                }

                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Подтверждение email по коду.
     * 400 = неверный / устаревший код.
     */
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
                    val rawError = verifyResponse.error?.trim()
                    val message = when {
                        rawError.equals("EMAIL_NOT_VERIFIED", ignoreCase = true) ->
                            // это как минимум человекочитаемо
                            "Не удалось подтвердить email. Попробуйте войти ещё раз и запросить новый код."
                        !rawError.isNullOrBlank() ->
                            rawError
                        else ->
                            "Не удалось подтвердить email."
                    }
                    Result.failure(Exception(message))
                }
            } else {
                val api = runCatching { response.body<ApiResponse<VerifyEmailResponse>>() }.getOrNull()
                val rawError = api?.error?.trim()

                val message = when (response.status) {
                    HttpStatusCode.BadRequest ->
                        rawError ?: "Неверный или устаревший код подтверждения."
                    HttpStatusCode.NotFound ->
                        rawError ?: "Пользователь для этого кода не найден."
                    else ->
                        rawError ?: "Не удалось подтвердить email. Ошибка: ${response.status.value}"
                }

                Result.failure(Exception(message))
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
                val api = runCatching {
                    response.body<ApiResponse<ResetPasswordResponse>>()
                }.getOrNull()

                val rawError = api?.error
                val message = when {
                    rawError.equals("Invalid or expired token", ignoreCase = true) ->
                        "Ссылка для восстановления пароля недействительна или устарела"
                    !rawError.isNullOrBlank() ->
                        rawError
                    response.status == HttpStatusCode.BadRequest ->
                        "Не удалось сменить пароль: некорректный запрос"
                    else ->
                        "Не удалось сменить пароль. ошибка: ${response.status.value}"
                }

                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        ApiConfig.clearToken()
    }
}
