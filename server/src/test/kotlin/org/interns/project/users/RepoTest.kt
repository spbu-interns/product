package org.interns.project.users

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.interns.project.users.model.UserInDto
import org.interns.project.users.repo.ApiUserRepo

class RepoTest {
    @Test
    fun saveAndFindByEmailAndLogin() = runBlocking {
        val repo = ApiUserRepo("http://127.0.0.1:8000")
        try {
            val userIn = UserInDto(
                email = "saschavinnik06@mail.ru",
                login = "ruavee",
                password = "kukareku",
                role = "CLIENT",
                firstName = "A",
                lastName = "B",
                clinicId = null,
                isActive = true
            )

            val created = repo.saveByApi(userIn)
            assertNotNull(created)
            assertTrue(created.id > 0)

            val byEmail = assertNotNull(repo.findByEmail("saschavinnik06@mail.ru"))
            val byLogin = assertNotNull(repo.findByLogin("ruavee"))

            assertEquals(created.id, byEmail.id)
            assertEquals(created.login, byLogin.login)
        } finally {
            repo.close()
        }
    }
}
