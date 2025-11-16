package org.interns.project.auth.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.interns.project.auth.reset.PasswordResetService
import org.interns.project.auth.verification.EmailVerificationService
import org.interns.project.dto.ApiResponse
import org.interns.project.security.token.JwtService
import org.interns.project.users.model.User
import org.interns.project.users.repo.ApiUserRepo
import java.util.*
import org.slf4j.LoggerFactory

class AuthController(
    private val apiUserRepo: ApiUserRepo,
    private val verificationService: EmailVerificationService,
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
                val apiRequest = call.receive<org.interns.project.dto.LoginRequest>()
                
                println("📝 Login attempt: email=${apiRequest.email}, accountType=${apiRequest.accountType}")
                
                try {
                    val mappedRole = mapRoleToDbRole(apiRequest.accountType).uppercase()
                    println("📝 Mapped role: ${apiRequest.accountType} -> $mappedRole")

                    val apiResponse = apiUserRepo.login(
                        loginOrEmail = apiRequest.email,
                        password = apiRequest.password
                    )
                    if (!apiResponse.success) {
                        val error = apiResponse.error ?: "Invalid email or password"
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiResponse<org.interns.project.dto.LoginResponse>(
                                success = false,
                                error = error
                            )
                        )
                        return@post
                    }
                    val user = apiUserRepo.findByEmail(apiRequest.email)
                    if (user == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiResponse<org.interns.project.dto.LoginResponse>(
                                success = false,
                                error = "Invalid email or password"
                            )
                        )
                        return@post
                    }
                    val actualRole = (apiResponse.role ?: user.role).uppercase()

                    if (mappedRole.isNotBlank() && mappedRole != actualRole) {
                        val targetName = mapRoleToDisplayName(mappedRole)
                        val actualName = mapRoleToDisplayName(actualRole)
                        val message = if (mappedRole == "DOCTOR" && actualRole != "DOCTOR") {
                            "Ваш аккаунт зарегистрирован как \"$actualName\". Вход для роли \"$targetName\" недоступен."
                        } else {
                            "Вход доступен только для роли \"$actualName\"."
                        }
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ApiResponse<org.interns.project.dto.LoginResponse>(
                                success = false,
                                error = message
                            )
                        )
                        return@post
                    }
                    val token = apiResponse.token?.takeIf { it.isNotBlank() }
                        ?: JwtService.issue(
                            subject = user.id.toString(),
                            login = user.email,
                            role = actualRole,
                            email = user.email
                        )

                    val loginResponse = org.interns.project.dto.LoginResponse(
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
                        ApiResponse<org.interns.project.dto.LoginResponse>(
                            success = false,
                            error = "Invalid email or password"
                        )
                    )
                }
            }
            
            post("/register") {
                val apiRequest = call.receive<org.interns.project.dto.RegisterRequest>()
                
                val internalRequest = org.interns.project.users.model.UserCreateRequest(
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


                    val response = org.interns.project.dto.RegisterResponse(
                        success = true,
                        message = "User registered successfully. Please check your email for verification.",
                        userId = userId
                    )
                    
                    call.respond(
                        HttpStatusCode.Created,
                        ApiResponse(success = true, data = response)
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<org.interns.project.dto.RegisterResponse>(
                            success = false,
                            error = "Failed to register user: ${e.message}"
                        )
                    )
                }
            }
            
            route("/email") {
                post("/start") {
                    val apiRequest = call.receive<org.interns.project.dto.RequestPasswordResetRequest>()
                    
                    val ok = verificationService.sendCodeByEmail(apiRequest.email)
                    
                    if (ok) {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = org.interns.project.dto.VerifyEmailResponse(
                                    success = true,
                                    message = "Verification email sent"
                                )
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<org.interns.project.dto.VerifyEmailResponse>(
                                success = false,
                                error = "User not found or already verified"
                            )
                        )
                    }
                }
                
                post("/verify") {
                    val apiRequest = call.receive<org.interns.project.dto.VerifyEmailRequest>()
                    val ok = verificationService.verifyByToken(apiRequest.token.trim())
                    
                    if (ok) {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = org.interns.project.dto.VerifyEmailResponse(
                                    success = true,
                                    message = "Email verified successfully"
                                )
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<org.interns.project.dto.VerifyEmailResponse>(
                                success = false,
                                error = "Invalid or expired token"
                            )
                        )
                    }
                }
            }
            
            route("/password") {
                post("/forgot") {
                    val apiRequest = call.receive<org.interns.project.dto.RequestPasswordResetRequest>()
                    val ok = passwordResetService.requestByEmail(apiRequest.email)

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = org.interns.project.dto.RequestPasswordResetResponse(
                                success = true,
                                message = "If the email exists, a password reset link has been sent"
                            )
                        )
                    )
                }
                
                post("/reset") {
                    val apiRequest = call.receive<org.interns.project.dto.ResetPasswordRequest>()
                    val userId = passwordResetService.verifyLink(apiRequest.token)
                    
                    if (userId == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<org.interns.project.dto.ResetPasswordResponse>(
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
                                data = org.interns.project.dto.ResetPasswordResponse(
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