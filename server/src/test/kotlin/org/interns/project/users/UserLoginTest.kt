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
            // правильный путь
            assertEquals("/auth/login", req.url.encodedPath)
            // тело сериализовано как ожидается
            val bodyText = (req.body as TextContent).text
            assertTrue(bodyText.contains("\"login_or_email\":\"alice\""))
            assertTrue(bodyText.contains("\"password\":\"secret123\""))
            respond("""{"success":true,"role":"CLIENT"}""", HttpStatusCode.OK, jsonHeaders)
        }

        val repo = ApiUserRepo(baseUrl = "http://test", client = engine)
        val res = repo.login("alice", "secret123")
        assertTrue(res.success)
        assertEquals("CLIENT", res.role)
        repo.close()
    }

    @Test
    fun testLoginOk_byEmail() = runBlocking {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

        val engine = mockClient { req ->
            val bodyText = (req.body as TextContent).text
            assertTrue(bodyText.contains("\"login_or_email\":\"u@example.com\""))
            assertTrue(bodyText.contains("\"password\":\"secret123\""))
            respond("""{"success":true,"role":"CLIENT"}""", HttpStatusCode.OK, jsonHeaders)
        }

        val repo = ApiUserRepo(baseUrl = "http://test", client = engine)
        val res = repo.login("u@example.com", "secret123")
        assertTrue(res.success)
        assertEquals("CLIENT", res.role)
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
