package org.interns.project.auth.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.interns.project.auth.reset.PasswordResetService
import org.interns.project.auth.verification.EmailVerificationService
import org.interns.project.config.SecurityConfig
import org.interns.project.dto.ApiResponse
import org.interns.project.users.model.User
import org.interns.project.users.repo.ApiUserRepo
import java.util.*

class AuthController(
    private val apiUserRepo: ApiUserRepo,
    private val verificationService: EmailVerificationService,
    private val passwordResetService: PasswordResetService
) {

    private fun generateToken(user: User): String {
        return Base64.getEncoder().encodeToString(
            "user:${user.id}:${user.email}:${System.currentTimeMillis()}".toByteArray()
        )
    }

    private fun mapRoleToDbRole(role: String): String {
        return when (role) {
            "Пациент" -> "CLIENT"
            "Врач" -> "DOCTOR"
            "Администратор" -> "ADMIN"
            else -> role
        }
    }

    fun registerRoutes(route: Route) {
        route.route("/api/auth") {
            post("/login") {
                val apiRequest = call.receive<org.interns.project.dto.LoginRequest>()
                
                println("📝 Login attempt: email=${apiRequest.email}, accountType=${apiRequest.accountType}")
                
                try {
                    val mappedRole = mapRoleToDbRole(apiRequest.accountType)
                    println("📝 Mapped role: ${apiRequest.accountType} -> $mappedRole")
                    
                    val apiResponse = apiUserRepo.login(
                        loginOrEmail = apiRequest.email,
                        password = apiRequest.password
                    )
                    
                    call.respond(
                        HttpStatusCode.OK,
                        apiResponse
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
                    
                    try {
                        verificationService.sendCodeByEmail(apiRequest.email)
                    } catch (emailError: Exception) {
                        println("⚠️ Email sending failed: ${emailError.message}")
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