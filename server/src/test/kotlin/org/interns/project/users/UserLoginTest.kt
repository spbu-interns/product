package org.interns.project.users

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import org.interns.project.users.repo.ApiUserRepo
import org.junit.jupiter.api.assertNotNull

class UserLoginTest {

    private fun mockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): HttpClient =
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
    fun testLoginOk_byLogin() = runBlocking {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

        val engine = mockClient { req ->
            val path = req.url.encodedPath
            when {
                // 1) POST /auth/login
                path == "/auth/login" -> {
                    assertEquals(HttpMethod.Post, req.method)
                    val bodyText = (req.body as TextContent).text
                    assertTrue(bodyText.contains("\"login_or_email\":\"alice\""))
                    assertTrue(bodyText.contains("\"password\":\"secret123\""))
                    respond("""{"success":true,"role":"CLIENT"}""", HttpStatusCode.OK, jsonHeaders)
                }

                // 2) GET /users/by-login/{login}  <-- без тела!
                path.startsWith("/users/by-login/") -> {
                    assertEquals(HttpMethod.Get, req.method)
                    assertTrue(path.endsWith("/alice"))
                    respond(
                        """{"id":1,"email":"alice@example.com","login":"alice","role":"CLIENT","is_active":true}""",
                        HttpStatusCode.OK,
                        jsonHeaders
                    )
                }

                else -> error("Unexpected path: $path")
            }
        }

        val repo = ApiUserRepo(baseUrl = "http://test", client = engine)
        val res = repo.login("alice", "secret123")
        assertTrue(res.success)
        assertEquals("CLIENT", res.role)
        // можно сразу проверить, что токен выдан
        assertNotNull(res.token)
        repo.close()
    }

    @Test
    fun testLoginOk_byEmail() = runBlocking {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

        val engine = mockClient { req ->
            val path = req.url.encodedPath
            when {
                // 1) POST /auth/login
                path == "/auth/login" -> {
                    assertEquals(HttpMethod.Post, req.method)
                    val bodyText = (req.body as TextContent).text
                    assertTrue(bodyText.contains("\"login_or_email\":\"u@example.com\""))
                    assertTrue(bodyText.contains("\"password\":\"secret123\""))
                    respond("""{"success":true,"role":"CLIENT"}""", HttpStatusCode.OK, jsonHeaders)
                }

                // 2) GET /users/by-email/{email}  <-- без тела!
                path.startsWith("/users/by-email/") -> {
                    assertEquals(HttpMethod.Get, req.method)
                    // e-mail в пути закодирован: u%40example.com
                    assertTrue(path.endsWith("/u%40example.com"))
                    respond(
                        """{"id":2,"email":"u@example.com","login":"user_u","role":"CLIENT","is_active":true}""",
                        HttpStatusCode.OK,
                        jsonHeaders
                    )
                }

                else -> error("Unexpected path: $path")
            }
        }

        val repo = ApiUserRepo(baseUrl = "http://test", client = engine)
        val res = repo.login("u@example.com", "secret123")
        assertTrue(res.success)
        assertEquals("CLIENT", res.role)
        assertNotNull(res.token)
        repo.close()
    }

    @Test
    fun testLoginUnauthorized_401() = runBlocking {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

        val engine = mockClient {
            respond("""{"detail":"invalid credentials"}""", HttpStatusCode.Unauthorized, jsonHeaders)
        }

        val repo = ApiUserRepo(baseUrl = "http://test", client = engine)

        assertFailsWith<IllegalArgumentException> {
            repo.login("alice", "wrong-pass")
        }

        repo.close()
    }

    @Test
    fun testLoginValidation_422() = runBlocking {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        val errorBody = """{"detail":[{"type":"string_too_short","loc":["body","password"],"msg":"String should have at least 6 characters"}]}"""

        val engine = mockClient {
            respond(errorBody, HttpStatusCode.UnprocessableEntity, jsonHeaders)
        }

        val repo = ApiUserRepo(baseUrl = "http://test", client = engine)

        assertFailsWith<IllegalArgumentException> {
            repo.login("alice", "123")
        }

        repo.close()
    }

    @Test
    fun testLoginServerError_500() = runBlocking {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

        val engine = mockClient {
            respond("""{"detail":"oops"}""", HttpStatusCode.InternalServerError, jsonHeaders)
        }

        val repo = ApiUserRepo(baseUrl = "http://test", client = engine)

        assertFailsWith<RuntimeException> {
            repo.login("alice", "secret123")
        }

        repo.close()
    }
}
