package org.interns.project.users

import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import org.interns.project.users.model.UserInDto
import org.interns.project.users.model.UserProfilePatch
import org.interns.project.users.repo.ApiUserRepo

class UserProfileIntegrationTest {

    private val baseUrl = "http://127.0.0.1:8001"

    @Test
    fun getUserProfile_realServer() {
        runBlocking {
            val repo = ApiUserRepo(baseUrl)
            try {
                val uniq = System.currentTimeMillis().toString()
                val email = "profile_$uniq@example.com"
                val login = "profile_$uniq"
                val password = "secret123"

                val created = repo.saveByApi(
                    UserInDto(
                        email = email,
                        login = login,
                        password = password,
                        role = "CLIENT"
                    )
                )

                assertTrue(created.id > 0)

                val profile = repo.getUserProfile(created.id)
                assertNotNull(profile)

                assertEquals(created.id, profile.id)
                assertEquals(email, profile.email)
                assertEquals(login, profile.login)
                assertEquals("CLIENT", profile.role)
            } finally {
                repo.close()
            }
        }
    }

    @Test
    fun patchUserProfile_fullUpdate_realServer() {
        runBlocking {
            val repo = ApiUserRepo(baseUrl)
            try {
                val uniq = System.currentTimeMillis().toString()
                val email = "patch_full_$uniq@example.com"
                val login = "patch_full_$uniq"
                val password = "secret123"

                val created = repo.saveByApi(
                    UserInDto(
                        email = email,
                        login = login,
                        password = password,
                        role = "CLIENT"
                    )
                )
                assertTrue(created.id > 0)

                val before = repo.getUserProfile(created.id)
                assertNotNull(before)

                val patch = UserProfilePatch(
                    firstName = "Иван",
                    lastName = "Иванов",
                    patronymic = "Иванович",
                    dateOfBirth = "2000-01-02",
                    avatar = "https://example.com/avatar_$uniq.png",
                    gender = "MALE",
                    phoneNumber = "+70000000000"
                )

                val updated = repo.patchUserProfile(created.id, patch)

                assertEquals(created.id, updated.id)
                assertEquals(email, updated.email)
                assertEquals(login, updated.login)
                assertEquals("CLIENT", updated.role)

                assertEquals("Иван", updated.name)
                assertEquals("Иванов", updated.surname)
                assertEquals("Иванович", updated.patronymic)
                assertEquals("+70000000000", updated.phoneNumber)
                assertEquals("MALE", updated.gender)
                assertEquals("https://example.com/avatar_$uniq.png", updated.avatar)

                val dobStr = updated.dateOfBirth ?: fail("dateOfBirth должен быть задан")
                val dob = LocalDate.parse(dobStr)
                assertEquals(2000, dob.year)
                assertEquals(1, dob.monthValue)
                assertEquals(2, dob.dayOfMonth)

                if (before.updatedAt != null && updated.updatedAt != null) {
                    assertTrue(updated.updatedAt >= before.updatedAt)
                }

                val loaded = repo.getUserProfile(created.id)
                assertNotNull(loaded)
                assertEquals("Иван", loaded.name)
                assertEquals("Иванов", loaded.surname)
                assertEquals("Иванович", loaded.patronymic)
                assertEquals("+70000000000", loaded.phoneNumber)
                assertEquals("MALE", loaded.gender)
                assertEquals("https://example.com/avatar_$uniq.png", loaded.avatar)
            } finally {
                repo.close()
            }
        }
    }

    @Test
    fun patchUserProfile_partialUpdate_realServer() {
        runBlocking {
            val repo = ApiUserRepo(baseUrl)
            try {
                val uniq = System.currentTimeMillis().toString()
                val email = "patch_partial_$uniq@example.com"
                val login = "patch_partial_$uniq"
                val password = "secret123"

                val created = repo.saveByApi(
                    UserInDto(
                        email = email,
                        login = login,
                        password = password,
                        role = "CLIENT"
                    )
                )
                assertTrue(created.id > 0)

                val afterFirst = repo.patchUserProfile(created.id, UserProfilePatch(firstName = "Anna", lastName = "Petrova"))
                assertEquals("Anna", afterFirst.name)
                assertEquals("Petrova", afterFirst.surname)

                val newAvatar = "https://example.com/new_avatar_$uniq.png"
                val afterSecond = repo.patchUserProfile(created.id, UserProfilePatch(avatar = newAvatar))

                assertEquals("Anna", afterSecond.name)
                assertEquals("Petrova", afterSecond.surname)
                assertEquals(newAvatar, afterSecond.avatar)
            } finally {
                repo.close()
            }
        }
    }

    @Test
    fun patchUserProfile_emptyPatch_shouldFail_realServer() {
        runBlocking {
            val repo = ApiUserRepo(baseUrl)
            try {
                val uniq = System.currentTimeMillis().toString()
                val created = repo.saveByApi(
                    UserInDto(
                        email = "patch_empty_$uniq@example.com",
                        login = "patch_empty_$uniq",
                        password = "secret123",
                        role = "CLIENT"
                    )
                )
                assertTrue(created.id > 0)

                assertFailsWith<IllegalArgumentException> {
                    repo.patchUserProfile(created.id, UserProfilePatch())
                }
            } finally {
                repo.close()
            }
        }
    }
}
