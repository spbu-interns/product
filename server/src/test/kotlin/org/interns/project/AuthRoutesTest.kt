package org.interns.project

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.interns.project.auth.AuthService
import org.interns.project.auth.User
import org.interns.project.auth.UserRepository

class AuthRoutesTest {

    @BeforeTest
    fun setupDb() {
        Database.connect(
            url = "jdbc:postgresql://localhost:5432/usersdb",
            driver = "org.postgresql.Driver",
            user = "app",
            password = "secret"
        )

        transaction {
            exec("DELETE FROM users WHERE login LIKE 'test_%'")

            exec("""
                INSERT INTO users (email, login, password_hash, role) VALUES (
                  'valid@example.com',
                  'test_valid',
                  '${BCrypt.withDefaults().hashToString(12, "correct".toCharArray())}',
                  'CLIENT'
                )
            """.trimIndent())

            exec("""
                INSERT INTO users (email, login, password_hash, role) VALUES (
                  'wrong@example.com',
                  'test_wrong',
                  '${BCrypt.withDefaults().hashToString(12, "something".toCharArray())}',
                  'CLIENT'
                )
            """.trimIndent())
        }
    }

    @AfterTest
    fun cleanupDb() {
        transaction {
            exec("DELETE FROM users WHERE login LIKE 'test_%'")
        }
    }

    @Test
    fun `login should fail with wrong password`() = testApplication {
        application { module() }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"test_wrong","password":"wrong"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(response.bodyAsText().contains("Неверный login или пароль"))
    }

    @Test
    fun `login should succeed with correct password`() = testApplication {
        application { module() }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"test_valid","password":"correct"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"success\":true"))
        assertTrue(body.contains("\"token\""))
        assertTrue(body.contains("\"role\":\"CLIENT\""))
    }

    @Test
    fun `login should fail with non existing user`() = testApplication {
        application { module() }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"test_unknown","password":"whatever"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(response.bodyAsText().contains("Неверный login или пароль"))
    }

    @Test
    fun `login should fail with empty login`() = testApplication {
        application { module() }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"","password":"somepass"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `login should fail with empty password`() = testApplication {
        application { module() }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"test_valid","password":""}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `login should fail with missing fields`() = testApplication {
        application { module() }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"test_valid"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `login should fail with sql injection attempt`() = testApplication {
        application { module() }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"' OR 1=1 --","password":"hack"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    private class FakeUserRepository(
        private val login: String,
        private val passwordHash: String
    ) : UserRepository() {
        override fun findByLogin(login: String): User? {
            return if (login == this.login) {
                User(1, login, passwordHash, "CLIENT")
            } else null
        }
    }

    @Test
    fun `generated token should be valid`() {
        val repo = FakeUserRepository(
            "user1",
            BCrypt.withDefaults().hashToString(12, "pass".toCharArray())
        )

        val service = AuthService(repo, "test-secret", 3600000)

        val result = service.login("user1", "pass")
        assertTrue(result is AuthService.Result.Success)

        val token = (result as AuthService.Result.Success).token
        val verifier = JWT.require(Algorithm.HMAC256("test-secret")).build()
        verifier.verify(token)
    }


}
