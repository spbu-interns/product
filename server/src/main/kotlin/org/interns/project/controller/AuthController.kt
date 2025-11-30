package org.interns.project.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.interns.project.auth.reset.PasswordResetService
import org.interns.project.auth.verification.EmailVerificationPort
import org.interns.project.dto.ApiResponse
import org.interns.project.dto.LoginRequest
import org.interns.project.dto.LoginResponse
import org.interns.project.dto.RegisterRequest
import org.interns.project.dto.RegisterResponse
import org.interns.project.dto.RequestPasswordResetRequest
import org.interns.project.dto.RequestPasswordResetResponse
import org.interns.project.dto.ResetPasswordRequest
import org.interns.project.dto.ResetPasswordResponse
import org.interns.project.dto.VerifyEmailRequest
import org.interns.project.dto.VerifyEmailResponse
import org.interns.project.security.token.JwtService
import org.interns.project.users.model.User
import org.interns.project.users.model.UserCreateRequest
import org.interns.project.users.repo.ApiUserRepo
import org.slf4j.LoggerFactory
import java.util.Base64

class AuthController(
    private val apiUserRepo: ApiUserRepo,
    private val verificationService: EmailVerificationPort,
    private val passwordResetService: PasswordResetService
) {
    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    private fun generateToken(user: User): String {
        return Base64.getEncoder().encodeToString(
            "user:${user.id}:${user.email}:${System.currentTimeMillis()}".toByteArray()
        )
    }

    private fun mapRoleToDbRole(role: String): String = when (role) {
        "Пациент" -> "CLIENT"
        "Медицинский работник" -> "DOCTOR"
        "Администратор" -> "ADMIN"
        else -> role
    }

    private fun mapRoleToDisplayName(role: String): String = when (role.uppercase()) {
        "CLIENT" -> "Пациент"
        "DOCTOR" -> "Медицинский работник"
        "ADMIN" -> "Администратор"
        else -> role
    }

    fun registerRoutes(route: Route) {
        route.route("/api/auth") {
            post("/login") {
                val apiRequest = call.receive<LoginRequest>()

                println("📝 Login attempt: email=${apiRequest.email}, accountType=${apiRequest.accountType}")

                try {
                    val mappedRole = mapRoleToDbRole(apiRequest.accountType).uppercase()
                    println("📝 Mapped role: ${apiRequest.accountType} -> $mappedRole")

                    // 1. Логинимся во внешнем Python-сервисе
                    val apiResponse = apiUserRepo.login(
                        loginOrEmail = apiRequest.email,
                        password = apiRequest.password
                    )

                    val rawError = apiResponse.error?.trim()

                    // 2. Python сказал, что логин неуспешен
                    if (!apiResponse.success) {
                        val isEmailNotVerified =
                            rawError.equals("EMAIL_NOT_VERIFIED", ignoreCase = true) ||
                                    (rawError?.contains("email not verified", ignoreCase = true) == true)

                        val status = if (isEmailNotVerified) HttpStatusCode.Forbidden else HttpStatusCode.Unauthorized
                        val errorMessage =
                            if (!rawError.isNullOrBlank()) rawError
                            else if (isEmailNotVerified) "EMAIL_NOT_VERIFIED"
                            else "Invalid email or password"

                        println("⛔ Login failed from Python: status=$status, error=$errorMessage")

                        call.respond(
                            status,
                            ApiResponse<LoginResponse>(
                                success = false,
                                error = errorMessage
                            )
                        )
                        return@post
                    }

                    // 3. Тянем пользователя, чтобы проверить emailVerifiedAt и роль
                    val user = apiUserRepo.findByEmail(apiRequest.email)
                    if (user == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiResponse<LoginResponse>(
                                success = false,
                                error = "Invalid email or password"
                            )
                        )
                        return@post
                    }
                    val actualRole = (apiResponse.role ?: user.role).uppercase()

                    // 4. Проверка роли (как было)
                    if (mappedRole.isNotBlank() && mappedRole != actualRole) {
                        val targetName = mapRoleToDisplayName(mappedRole)
                        val actualName = mapRoleToDisplayName(actualRole)
                        val message = if (mappedRole == "DOCTOR") {
                            "Ваш аккаунт зарегистрирован как \"$actualName\". Вход для роли \"$targetName\" недоступен."
                        } else {
                            "Вход доступен только для роли \"$actualName\"."
                        }
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ApiResponse<LoginResponse>(
                                success = false,
                                error = message
                            )
                        )
                        return@post
                    }

                    // 5. Токен
                    val token = apiResponse.token?.takeIf { it.isNotBlank() }
                        ?: JwtService.issue(
                            subject = user.id.toString(),
                            login = user.email,
                            role = actualRole,
                            email = user.email
                        )

                    val loginResponse = LoginResponse(
                        token = token,
                        userId = user.id,
                        email = user.email,
                        accountType = actualRole,
                        firstName = user.firstName,
                        lastName = user.lastName
                    )

                    println("🔵 Response: ${loginResponse.userId}")
                    println("🔵 Response body: ${loginResponse.email}")
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = loginResponse
                        )
                    )
                } catch (e: Exception) {
                    println("❌ Login failed: ${e.message}")
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ApiResponse<LoginResponse>(
                            success = false,
                            error = "Invalid email or password"
                        )
                    )
                }
            }

            post("/register") {
                val apiRequest = call.receive<RegisterRequest>()

                // Если пользователь с таким email уже зарегистрирован:
                val existingUser = apiUserRepo.findByEmail(apiRequest.email)
                if (existingUser != null) {
                    if (existingUser.emailVerifiedAt != null) {
                            // Пользователь уже зарегистрирован **и верифицирован** – предлагаем войти в аккаунт
                            call.respond(HttpStatusCode.Conflict, ApiResponse<RegisterResponse>(
                                    success = false,
                                    error = "Пользователь с таким email уже зарегистрирован. Войдите в аккаунт."
                                        ))
                        } else {
                            // Пользователь уже зарегистрирован, **но не верифицирован** – высылаем код повторно
                            val emailSent = try {
                                    verificationService.sendCodeByEmail(apiRequest.email)
                                } catch (e: Exception) {
                                    logger.error("event=resend_verification_failed email={} error={}", apiRequest.email, e.message, e)
                                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<RegisterResponse>(
                                            success = false,
                                            error = "Не удалось отправить письмо с кодом подтверждения"
                                                ))
                                    return@post
                                }
                            if (!emailSent) {
                                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<RegisterResponse>(
                                            success = false,
                                            error = "Не удалось отправить письмо с кодом подтверждения"
                                                ))
                                } else {
                                    // Отправлено успешно – возвращаем успешный ответ, как при новой регистрации
                                    val response = RegisterResponse(
                                            success = true,
                                            message = "Письмо с подтверждением отправлено повторно.",
                                            userId = existingUser.id,
                                            requiresEmailVerification = true
                                                )
                                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = response))
                                }
                        }
                    return@post
                }

                val internalRequest = UserCreateRequest(
                    email = apiRequest.email,
                    login = apiRequest.email,
                    password = apiRequest.password, // Отправляем пароль без хеширования, Python-сервис сам хеширует
                    role = mapRoleToDbRole(apiRequest.accountType),
                    username = apiRequest.email
                )

                try {
                    val userId = apiUserRepo.createUser(internalRequest)

                    val emailSent = try {
                        verificationService.sendCodeByEmail(apiRequest.email)
                    } catch (emailError: Exception) {
                        logger.error(
                            "event=verification_db_failed email={} errorType={} message={}",
                            apiRequest.email,
                            emailError::class.qualifiedName ?: emailError::class.simpleName,
                            emailError.message,
                            emailError
                        )
                        throw emailError
                    }

                    if (!emailSent) {
                        logger.error(
                            "event=smtp_send_failed email={} reason={} message={}",
                            apiRequest.email,
                            "verification_service_returned_false",
                            "User not found or already verified"
                        )
                        throw IllegalStateException("Failed to send verification email")
                    }

                    logger.info(
                        "event=verification_email_sent status=success email={} userId={} message={}",
                        apiRequest.email,
                        userId,
                        "initial_registration_dispatch"
                    )

                    val response = RegisterResponse(
                        success = true,
                        message = "User registered successfully. A verification email has been sent once.",
                        userId = userId,
                        requiresEmailVerification = true
                    )

                    call.respond(
                        HttpStatusCode.Created,
                        ApiResponse(success = true, data = response)
                    )
                } catch (e: IllegalStateException) {
                    logger.warn(
                        "event=registration_conflict email={} message={}",
                        apiRequest.email,
                        e.message
                    )
                    val message = "Пользователь с таким email уже существует"

                    call.respond(
                        HttpStatusCode.Conflict,
                        ApiResponse<RegisterResponse>(
                            success = false,
                            error = message
                        )
                    )
                } catch (e: IllegalArgumentException) {
                    logger.warn(
                        "event=registration_validation_failed email={} message={}",
                        apiRequest.email,
                        e.message
                    )
                    val validationDetails = e.message
                        ?.substringAfter(":", missingDelimiterValue = e.message ?: "")
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: ""

                    val message = listOf("Некорректные данные регистрации", validationDetails)
                        .filter { it.isNotBlank() }
                        .joinToString(": ")

                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<RegisterResponse>(
                            success = false,
                            error = message
                        )
                    )
                } catch (e: Exception) {
                    logger.error(
                        "event=registration_failed email={} errorType={} message={}",
                        apiRequest.email,
                        e::class.qualifiedName ?: e::class.simpleName,
                        e.message,
                        e
                    )
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<RegisterResponse>(
                            success = false,
                            error = "Failed to register user: ${e.message}"
                        )
                    )
                }
            }

            route("/email") {
                post("/start") {
                    val apiRequest = call.receive<RequestPasswordResetRequest>()

                    val ok = verificationService.sendCodeByEmail(apiRequest.email)
                    if (ok) {
                        logger.info(
                            "event=verification_email_sent status=success email={} message={}",
                            apiRequest.email,
                            "manual_trigger"
                        )
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = VerifyEmailResponse(
                                    success = true,
                                    message = "Письмо с подтверждением отправлено повторно"
                                )
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<VerifyEmailResponse>(
                                success = false,
                                error = "Не удалось отправить письмо подтверждения"
                            )
                        )
                    }
                }

                post("/verify") {
                    val apiRequest = call.receive<VerifyEmailRequest>()
                    val ok = verificationService.verifyByToken(apiRequest.token.trim())

                    if (ok) {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = VerifyEmailResponse(
                                    success = true,
                                    message = "Email verified successfully"
                                )
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<VerifyEmailResponse>(
                                success = false,
                                error = "Invalid or expired token"
                            )
                        )
                    }
                }
            }

            route("/password") {
                post("/forgot") {
                    val apiRequest = call.receive<RequestPasswordResetRequest>()

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = RequestPasswordResetResponse(
                                success = true,
                                message = "If the email exists, a password reset link has been sent"
                            )
                        )
                    )
                }

                post("/reset") {
                    val apiRequest = call.receive<ResetPasswordRequest>()
                    val userId = passwordResetService.verifyLink(apiRequest.token)

                    if (userId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<ResetPasswordResponse>(
                                success = false,
                                error = "Invalid or expired token"
                            )
                        )
                    } else {
                        passwordResetService.completeReset(userId, apiRequest.newPassword)

                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = ResetPasswordResponse(
                                    success = true,
                                    message = "Password changed successfully"
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}
