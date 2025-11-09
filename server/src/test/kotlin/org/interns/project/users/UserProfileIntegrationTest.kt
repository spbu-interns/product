package org.interns.project.users

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

                // создаём пользователя
                val created = repo.saveByApi(
                    UserInDto(
                        email = email,
                        login = login,
                        password = password,
                        role = "CLIENT"
                    )
                )

                assertTrue(created.id > 0)

                // читаем профиль
                val profile = repo.getUserProfile(created.id)
                assertNotNull(profile)

                assertEquals(created.id, profile.id)
                assertEquals(email, profile.email)
                assertEquals(login, profile.login)
                assertEquals("CLIENT", profile.role)
                // остальные поля могут быть null — это ок
            } finally {
                // на всякий случай закрываем клиент
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

                // инварианты
                assertEquals(created.id, updated.id)
                assertEquals(email, updated.email)
                assertEquals(login, updated.login)
                assertEquals("CLIENT", updated.role)

                // профиль обновился
                assertEquals("Иван", updated.firstName)
                assertEquals("Иванов", updated.lastName)
                assertEquals("Иванович", updated.patronymic)
                assertEquals("+70000000000", updated.phoneNumber)
                assertEquals("MALE", updated.gender)
                assertEquals("https://example.com/avatar_$uniq.png", updated.avatar)

                val dob = updated.dateOfBirth ?: fail("dateOfBirth должен быть задан")
                assertEquals(2000, dob.year)
                assertEquals(1, dob.monthValue)
                assertEquals(2, dob.dayOfMonth)

                if (before.updatedAt != null && updated.updatedAt != null) {
                    assertTrue(updated.updatedAt >= before.updatedAt)
                }

                // повторная проверка через getUserProfile
                val loaded = repo.getUserProfile(created.id)
                assertNotNull(loaded)
                assertEquals("Иван", loaded.firstName)
                assertEquals("Иванов", loaded.lastName)
                assertEquals("Иванович", loaded.patronymic)
                assertEquals("+70000000000", loaded.phoneNumber)
                assertEquals("MALE", loaded.gender)
                assertEquals("https://example.com/avatar_$uniq.png", loaded.avatar)
            } finally {
                // repo.close() если нужно, но обычно он общий
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

                // первый патч — задаём name/surname
                val firstPatch = UserProfilePatch(
                    firstName = "Anna",
                    lastName = "Petrova"
                )
                val afterFirst = repo.patchUserProfile(created.id, firstPatch)
                assertEquals("Anna", afterFirst.firstName)
                assertEquals("Petrova", afterFirst.lastName)

                // второй патч — только avatar
                val newAvatar = "https://example.com/new_avatar_$uniq.png"
                val secondPatch = UserProfilePatch(
                    avatar = newAvatar
                )
                val afterSecond = repo.patchUserProfile(created.id, secondPatch)

                // старые поля не трогаются
                assertEquals("Anna", afterSecond.firstName)
                assertEquals("Petrova", afterSecond.lastName)
                // новый avatar применился
                assertEquals(newAvatar, afterSecond.avatar)
            } finally {
                // optional close
            }
        }
    }

    @Test
    fun patchUserProfile_emptyPatch_shouldFail_realServer() {
        runBlocking {
            val repo = ApiUserRepo(baseUrl)
            try {
                val uniq = System.currentTimeMillis().toString()
                val email = "patch_empty_$uniq@example.com"
                val login = "patch_empty_$uniq"
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

                // fastapi требует хотя бы одно поле → 422,
                // doPatch заворачивает это в IllegalArgumentException
                assertFailsWith<IllegalArgumentException> {
                    repo.patchUserProfile(created.id, UserProfilePatch())
                }
            } finally {
                // optional close
            }
        }
    }
}
