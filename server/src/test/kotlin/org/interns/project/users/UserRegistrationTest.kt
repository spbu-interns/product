package org.interns.project.users

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.interns.project.module

class UserRegistrationTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun testSuccessfulRegistrationAndLogin() = testApplication {
        application { module() }

        val registerBody = mapOf(
            "email" to "ok@example.com",
            "login" to "okuser",
            "password" to "pass123",
            "role" to "CLIENT",
            "first_name" to "Иван",
            "last_name" to "Иванов"
        )
        val registerResponse = client.post("/api/users/register") {
            contentType(ContentType.Application.Json)
            setBody(mapper.writeValueAsString(registerBody))
        }
        assertEquals(HttpStatusCode.OK, registerResponse.status)
        val regJson = mapper.readTree(registerResponse.bodyAsText())
        assertTrue(regJson.get("success").asBoolean())
        assertEquals("CLIENT", regJson.get("role").asText())

        // login
        val loginBody = mapOf("login" to "okuser", "password" to "pass123")
        val loginResponse = client.post("/api/users/login") {
            contentType(ContentType.Application.Json)
            setBody(mapper.writeValueAsString(loginBody))
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val loginJson = mapper.readTree(loginResponse.bodyAsText())
        assertTrue(loginJson.get("success").asBoolean())
    }

    @Test
    fun testWrongEmail() = testApplication {
        application { module() }

        val bad = mapOf("email" to "eto_pashalka", "login" to "dlya_teh", "password" to "kto_chitaet")
        val r1 = client.post("/api/users/register") {
            contentType(ContentType.Application.Json)
            setBody(mapper.writeValueAsString(bad))
        }
        assertEquals(HttpStatusCode.BadRequest, r1.status)
        val body = mapper.readTree(r1.bodyAsText())
        assertEquals(false, body.get("success").asBoolean())
        assertTrue(body.get("error").asText().contains("Неверный формат email"))
    }

    @Test
    fun testDuplicateEmailAndLogin() = testApplication {
        application { module() }

        val a = mapOf("email" to "dup@example.com", "login" to "u1", "password" to "p1")
        val b = mapOf("email" to "dup@example.com", "login" to "u2", "password" to "p2")
        val c = mapOf("email" to "other@example.com", "login" to "u1", "password" to "p3")

        val r1 = client.post("/api/users/register") {
            contentType(ContentType.Application.Json)
            setBody(mapper.writeValueAsString(a))
        }
        assertEquals(HttpStatusCode.OK, r1.status)

        val r2 = client.post("/api/users/register") {
            contentType(ContentType.Application.Json)
            setBody(mapper.writeValueAsString(b))
        }
        assertEquals(HttpStatusCode.BadRequest, r2.status)
        val r2json = mapper.readTree(r2.bodyAsText())
        assertEquals("Email уже зарегистрирован", r2json.get("error").asText())

        val r3 = client.post("/api/users/register") {
            contentType(ContentType.Application.Json)
            setBody(mapper.writeValueAsString(c))
        }
        assertEquals(HttpStatusCode.BadRequest, r3.status)
        val r3json = mapper.readTree(r3.bodyAsText())
        assertEquals("Логин уже занят", r3json.get("error").asText())
    }

    @Test
    fun testEmptyFields() = testApplication {
        application { module() }

        // empty body
        val rEmpty = client.post("/api/users/register") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        // Missing required fields -> Jackson/Ktor should respond 400
        assertEquals(HttpStatusCode.BadRequest, rEmpty.status)

        // missing password
        val partial = mapOf("email" to "x@y.com", "login" to "noPass")
        val rPartial = client.post("/api/users/register") {
            contentType(ContentType.Application.Json)
            setBody(mapper.writeValueAsString(partial))
        }
        assertEquals(HttpStatusCode.BadRequest, rPartial.status)
    }

    @Test
    fun testLoginWrongPassword() = testApplication {
        application { module() }

        val register = mapOf("email" to "who@example.com", "login" to "who", "password" to "rightpass")
        client.post("/api/users/register") {
            contentType(ContentType.Application.Json)
            setBody(mapper.writeValueAsString(register))
        }

        val badLogin = mapOf("login" to "who", "password" to "wrongpass")
        val resp = client.post("/api/users/login") {
            contentType(ContentType.Application.Json)
            setBody(mapper.writeValueAsString(badLogin))
        }
        assertEquals(HttpStatusCode.Unauthorized, resp.status)
        val json = mapper.readTree(resp.bodyAsText())
        assertEquals(false, json.get("success").asBoolean())
    }
}
