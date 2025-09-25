package org.interns.project.users

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.interns.project.users.model.User
import org.interns.project.users.repo.InMemoryUserRepo
import java.time.Instant

class RepoTest {
    @Test
    fun saveAndFindByEmailAndLogin() {
        val repo = InMemoryUserRepo()
        val id = repo.nextId()
        val now = Instant.now()
        val user = User(
            id = id,
            email = "repo@example.com",
            login = "repoUser",
            passwordHash = "hashy",
            role = "CLIENT",
            firstName = "A",
            lastName = "B",
            patronymic = null,
            phoneNumber = "+70000000000",
            isActive = true,
            clinicId = null,
            createdAt = now,
            updatedAt = now
        )
        repo.save(user)
        val byEmail = repo.findByEmail("repo@example.com")
        val byLogin = repo.findByLogin("repoUser")
        assertNotNull(byEmail)
        assertNotNull(byLogin)
        assertEquals(user.id, byEmail.id)
        assertEquals(user.login, byLogin.login)
        repo.clear()
        assertEquals(null, repo.findByEmail("repo@example.com"))
    }
}
