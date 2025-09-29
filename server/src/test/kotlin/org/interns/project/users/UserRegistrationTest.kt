package org.interns.project.users

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import org.interns.project.users.repo.ApiUserRepo

class UserRegistrationTest {

    private fun mockClient(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
        HttpClient(MockEngine { request -> handler(request) }) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                        explicitNulls = false
                    }
                )
            }
        }

    @Test
    fun testSuccessfulRegistrationAndLookup() = runBlocking {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

        val responseJson = """
            {
              "id": 101,
              "email": "repo@example.com",
              "login": "repoUser",
              "role": "CLIENT",
              "first_name": "Иван",
              "last_name": "Иванов",
              "clinic_id": 1,
              "is_active": true,
              "created_at": "2025-09-29T18:00:00Z",
              "updated_at": "2025-09-29T18:00:00Z"
            }
        """.trimIndent()

        val engine = mockClient { req ->
            when (req.url.encodedPath) {
                "/users" -> respond(responseJson, HttpStatusCode.Created, jsonHeaders)
                "/users/by-email/repo%40example.com" -> respond(responseJson, HttpStatusCode.OK, jsonHeaders)
                "/users/by-login/repoUser" -> respond(responseJson, HttpStatusCode.OK, jsonHeaders)
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val repo = ApiUserRepo(baseUrl = "http://test", client = engine)

        val created = repo.saveByApi(
            org.interns.project.users.model.UserInDto(
                email = "repo@example.com",
                login = "repoUser",
                password = "pass123",
                role = "CLIENT",
                firstName = "Иван",
                lastName = "Иванов",
                clinicId = 1,
                isActive = true
            )
        )

        assertTrue(created.id > 0)
        assertEquals("repo@example.com", created.email)
        assertEquals("repoUser", created.login)
        assertEquals("CLIENT", created.role)

        val byEmail = assertNotNull(repo.findByEmail("repo@example.com"))
        val byLogin = assertNotNull(repo.findByLogin("repoUser"))
        assertEquals(created.id, byEmail.id)
        assertEquals(created.login, byLogin.login)

        repo.close()
    }

    @Test
    fun testValidationError_422() = runBlocking {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        val errorBody = """
            {"detail":[{"type":"string_too_short","loc":["body","password"],"msg":"String should have at least 6 characters","input":"short","ctx":{"min_length":6}}]}
        """.trimIndent()

        val engine = mockClient { req ->
            when (req.url.encodedPath) {
                "/users" -> respond(errorBody, HttpStatusCode.UnprocessableEntity, jsonHeaders)
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val repo = ApiUserRepo(baseUrl = "http://test", client = engine)

        val ex = assertFailsWith<IllegalArgumentException> {
            repo.saveByApi(
                org.interns.project.users.model.UserInDto(
                    email = "e@example.com",
                    login = "u",
                    password = "short",
                    role = "CLIENT",
                    firstName = null,
                    lastName = null,
                    clinicId = null,
                    isActive = true
                )
            )
        }
        assertTrue(ex.message!!.contains("422 Unprocessable Entity"))

        repo.close()
    }

    @Test
    fun testConflict_409() = runBlocking {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        val engine = mockClient { req ->
            when (req.url.encodedPath) {
                "/users" -> respond("""{"detail":"duplicate"}""", HttpStatusCode.Conflict, jsonHeaders)
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val repo = ApiUserRepo(baseUrl = "http://test", client = engine)

        val ex = assertFailsWith<IllegalStateException> {
            repo.saveByApi(
                org.interns.project.users.model.UserInDto(
                    email = "dup@example.com",
                    login = "dup",
                    password = "pass123",
                    role = "CLIENT"
                )
            )
        }
        assertTrue(ex.message!!.contains("409 Conflict"))

        repo.close()
    }
}
