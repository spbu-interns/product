package api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json


class AuthApiClient {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    suspend fun login(login: String, password: String): Result<LoginResponse> = withContext(Dispatchers.Default) {
        try {
            console.log("Making login request to: ${ApiConfig.API_BASE_URL}/auth/login")
            console.log("Request body: ${LoginRequest(login, password)}")
            
            val response = client.post("${ApiConfig.API_BASE_URL}/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(login, password))
            }
            
            console.log("Login response status: ${response.status}")
            console.log("Login response body: ${response.bodyAsText()}")
            
            if (response.status.isSuccess()) {
                val loginResponse = response.body<LoginResponse>()
                loginResponse.token?.let { ApiConfig.saveToken(it) }
                loginResponse.role?.let { ApiConfig.saveRole(it) }
                Result.success(loginResponse)
            } else {
                val errorMessage = try {
                    response.body<LoginResponse>().error ?: "Login failed with status: ${response.status}"
                } catch (e: Exception) {
                    "Login failed with status: ${response.status}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            console.error("Login error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun register(username: String, password: String, email: String): Result<RegisterResponse> = withContext(Dispatchers.Default) {
        try {
            val emailExists = checkEmailExists(email)
            if (emailExists.isSuccess && emailExists.getOrNull() == true) {
                return@withContext Result.failure(Exception("Email already exists"))
            }
            
            val loginExists = checkLoginExists(username)
            if (loginExists.isSuccess && loginExists.getOrNull() == true) {
                return@withContext Result.failure(Exception("Username already exists"))
            }
            
            console.log("Making registration request to: ${ApiConfig.API_BASE_URL}/api/users/register")
            val requestBody = RegisterRequest(
                username = username,
                password = password,
                email = email
            )
            console.log("Request body: $requestBody")
            
            val response = client.post("${ApiConfig.API_BASE_URL}/api/users/register") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            console.log("Registration response status: ${response.status}")
            console.log("Registration response body: ${response.bodyAsText()}")
            
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val errorBody = try {
                    response.body<RegisterResponse>().error ?: "Registration failed with status: ${response.status}"
                } catch (e: Exception) {
                    "Registration failed with status: ${response.status}"
                }
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            console.error("Registration error: ${e.message}")
            Result.failure(e)
        }
    }
    

    private suspend fun checkEmailExists(email: String): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            val response = client.get("${ApiConfig.API_BASE_URL}/api/users/exists/email/$email")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to check email existence"))
            }
        } catch (e: Exception) {
            console.error("Check email error: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun checkLoginExists(login: String): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            val response = client.get("${ApiConfig.API_BASE_URL}/api/users/exists/login/$login")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to check login existence"))
            }
        } catch (e: Exception) {
            console.error("Check login error: ${e.message}")
            Result.failure(e)
        }
    }

    fun logout() {
        ApiConfig.clearToken()
        ApiConfig.clearRole()
    }

    fun isLoggedIn(): Boolean {
        return ApiConfig.getToken() != null
    }

    fun getUserRole(): String? {
        return ApiConfig.getRole()
    }
}

external val console: Console

external interface Console {
    fun log(vararg message: Any?)
    fun error(vararg message: Any?)
    fun warn(vararg message: Any?)
    fun info(vararg message: Any?)
}