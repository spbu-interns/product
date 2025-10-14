package org.interns.project.users.repo

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.interns.project.users.model.User
import org.interns.project.users.model.UserInDto
import org.interns.project.users.model.UserOutDto
import org.interns.project.users.model.UserCreateRequest
import java.time.Instant
import org.interns.project.users.dto.ApiResponse

class ApiUserRepo(
    private val baseUrl: String = "http://127.0.0.1:8000",
    private val client: HttpClient = defaultClient()
) {
    companion object {
        fun defaultClient() = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                        explicitNulls = false
                    }
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 5_000
                connectTimeoutMillis = 2_000
                socketTimeoutMillis  = 5_000
            }
        }
    }

    fun close() = client.close()

    private fun fromOutDto(d: UserOutDto): User {
        val created = try { d.createdAt?.let { Instant.parse(it) } } catch (_: Exception) { null }
        val updated = try { d.updatedAt?.let { Instant.parse(it) } } catch (_: Exception) { null }

        return User(
            id = d.id ?: 0L,
            email = d.email,
            login = d.login,
            passwordHash = "",
            role = d.role,
            firstName = d.firstName,
            lastName = d.lastName,
            patronymic = null,
            phoneNumber = null,
            isActive = d.isActive,
            clinicId = d.clinicId,
            createdAt = created,
            updatedAt = updated
        )
    }

    private fun urlEncode(s: String) = java.net.URLEncoder.encode(s, Charsets.UTF_8.name())

    private suspend fun <T> doGet(path: String, parse: suspend (HttpResponse) -> T?): T? {
        val resp = client.get("$baseUrl$path")
        if (resp.status != HttpStatusCode.OK) return null
        val text = resp.bodyAsText()
        if (text == "null" || text.isBlank()) return null
        return parse(resp)
    }

    private suspend fun <T> doPost(
        path: String,
        body: Any?,
        successCodes: Set<HttpStatusCode> = setOf(HttpStatusCode.Created, HttpStatusCode.OK),
        parse: suspend (HttpResponse) -> T
    ): T {
        val resp = client.post("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            setBody(body ?: emptyMap<String, Any>())
        }

        println("🔵 Request to: $baseUrl$path")
        println("🔵 Request body: ${body.toString()}")
        println("🔵 Response status: ${resp.status}")
        println("🔵 Response body: ${resp.bodyAsText()}")

        return when (resp.status) {
            in successCodes -> parse(resp)
            HttpStatusCode.UnprocessableEntity ->
                throw IllegalArgumentException("422 Unprocessable Entity: ${resp.bodyAsText()}")
            HttpStatusCode.Conflict ->
                throw IllegalStateException("409 Conflict: email or login already exists")
            HttpStatusCode.BadRequest ->
                throw IllegalArgumentException("400 Bad request: ${resp.bodyAsText()}")
            HttpStatusCode.Unauthorized ->
                throw IllegalArgumentException("401 Unauthorized: invalid credentials")
            else ->
                throw RuntimeException("Unexpected response: ${resp.status} ${resp.bodyAsText()}")
        }
    }

    suspend fun saveByApi(input: UserInDto): User {
        val dto = UserCreateRequest(
            email = input.email,
            login = input.login,
            password = input.password,
            role = input.role,
            username = input.login,
            firstName = input.firstName,
            lastName  = input.lastName,
            clinicId  = input.clinicId,
            isActive  = input.isActive
        )
        return doPost("/users", dto) { resp ->
            resp.body<UserOutDto>().let(::fromOutDto)
        }
    }

    suspend fun findByEmail(email: String): User? =
        doGet("/users/by-email/${urlEncode(email)}") { it.body<UserOutDto>().let(::fromOutDto) }

    suspend fun findByLogin(login: String): User? =
        doGet("/users/by-login/${urlEncode(login)}") { it.body<UserOutDto>().let(::fromOutDto) }

    suspend fun login(loginOrEmail: String, password: String): ApiResponse {
        val body = mapOf(
            "login_or_email" to loginOrEmail,
            "password" to password
        )
        
        return doPost(
            path = "/auth/login",
            body = body,
            successCodes = setOf(HttpStatusCode.OK)
        ) { resp -> resp.body<ApiResponse>() }
    }

    suspend fun createUser(request: UserCreateRequest): Long {
        val userInDto = UserInDto(
            email = request.email,
            login = request.login,
            password = request.password,
            role = request.role,
            firstName = request.firstName,
            lastName = request.lastName,
            clinicId = request.clinicId?.toInt(),
            isActive = request.isActive?: true
        )
        
        val user = saveByApi(userInDto)
        return user.id
    }
}
