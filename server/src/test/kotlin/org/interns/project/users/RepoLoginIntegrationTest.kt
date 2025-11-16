package org.interns.project.users

import kotlinx.coroutines.runBlocking
import org.interns.project.users.model.UserInDto
import org.interns.project.users.repo.ApiUserRepo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertNotNull

class RepoLoginIntegrationTest {

    @Test
    fun createUser_thenLogin_realServer() = runBlocking {
        val repo = ApiUserRepo(baseUrl = "http://127.0.0.1:8001")
        try {
            val uniq = System.currentTimeMillis().toString()
            val email = "it_$uniq@example.com"
            val login = "it_$uniq"
            val password = "secret123"

            val created = repo.saveByApi(
                UserInDto(email, login, password, role = "CLIENT")
            )
            assertTrue(created.id > 0)

            val res = repo.login(login, password)
            assertTrue(res.success)
            assertEquals("CLIENT", res.role)
            assertNotNull(res.token)
        } finally {
            repo.close()
        }
    }
}
