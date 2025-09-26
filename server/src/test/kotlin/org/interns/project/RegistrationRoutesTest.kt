package org.interns.project

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.jetbrains.exposed.sql.transactions.transaction
import io.ktor.server.testing.testApplication
import org.interns.project.config.SecurityConfig
import org.interns.project.users.model.UsersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class RegistrationRoutesTest {
    @Before
    fun setupDb() {
        SecurityConfig.initForTests(12)

        Database.Companion.connect(
            url = "jdbc:postgresql://localhost:5432/usersdb",
            driver = "org.postgresql.Driver",
            user = "app",
            password = "secret"
        )

        transaction {
            exec("DELETE FROM users WHERE login LIKE 'testreg_%'")
        }
    }

    @After
    fun cleanupDb() {
        transaction {
            exec("DELETE FROM users WHERE login LIKE 'testreg_%'")
        }
    }

    @Test
    fun `register should succeed with valid data`() = testApplication {
        application { module() }

        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
              "email": "testreg_success@example.com",
              "login": "testreg_success",
              "password": "strongpass",
              "role": "CLIENT"
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"success\":true"))

        transaction {
            val hash: String = UsersTable
                .selectAll().where { UsersTable.login eq "testreg_success" }
                .single()[UsersTable.passwordHash]

            assertFalse(hash.startsWith("strongpass"))
            assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"))
        }
    }

    @Test
    fun `register should fail with existing login`() = testApplication {
        application { module() }

        transaction {
            exec(
                """
                INSERT INTO users (email, login, password_hash, role)
                VALUES ('existlogin@example.com', 'testreg_exist', '${
                    BCrypt.withDefaults().hashToString(12, "somepass".toCharArray())
                }', 'CLIENT')
                """.trimIndent()
            )
        }

        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "email": "new@example.com",
                  "login": "testreg_exist",
                  "password": "anotherpass",
                  "role": "CLIENT"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Companion.Conflict, response.status)
    }

    @Test
    fun `register should fail with existing email`() = testApplication {
        application { module() }

        transaction {
            exec(
                """
                INSERT INTO users (email, login, password_hash, role)
                VALUES ('existemail@example.com', 'testreg_email', '${
                    BCrypt.withDefaults().hashToString(12, "somepass".toCharArray())
                }', 'CLIENT')
                """.trimIndent()
            )
        }

        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "email": "existemail@example.com",
                  "login": "testreg_new",
                  "password": "anotherpass",
                  "role": "CLIENT"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Companion.Conflict, response.status)
    }

    @Test
    fun `register should fail with invalid role`() = testApplication {
        application { module() }

        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "email": "testreg_invalidrole@example.com",
                  "login": "testreg_invalidrole",
                  "password": "pass123",
                  "role": "HACKER"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Companion.BadRequest, response.status)
    }

    @Test
    fun `register should fail with empty fields`() = testApplication {
        application { module() }

        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "email": "",
                  "login": "",
                  "password": "",
                  "role": "CLIENT"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Companion.BadRequest, response.status)
    }
}