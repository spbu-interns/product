package org.interns.project.users

import kotlinx.coroutines.runBlocking
import org.interns.project.users.model.*
import org.interns.project.users.repo.ApiUserRepo
import kotlin.test.*

@Ignore
class DoctorClientEditIntegrationTest {

    private val baseUrl = "http://127.0.0.1:8001"

    @Test
    fun client_edit_realServer() = runBlocking {
        val repo = ApiUserRepo(baseUrl)
        try {
            val uniq = System.currentTimeMillis().toString()
            val email = "cl_edit_$uniq@example.com"
            val login = "cl_edit_$uniq"
            val password = "secret123"

            val clientUser = repo.saveByApi(UserInDto(email, login, password, role = "CLIENT"))
            assertTrue(clientUser.id > 0)

            val client = repo.findClientByUserId(clientUser.id) ?: fail("client not found after registration")
            val clientId = client.id

            val patched1 = repo.patchClientByUserId(
                userId = clientUser.id,
                patch = ClientPatch(
                    height = 182.0,
                    weight = 78.0,
                    address = "ул. Пушкина, 1",
                    emergencyContactName = "мама",
                    emergencyContactNumber = "+7 900 000-00-00",
                    bloodType = "O+",
                    dmsOms = "ОМС 77-123456"
                )
            )

            assertEquals(clientId, patched1.id)
            assertEquals(clientUser.id, patched1.userId)
            assertEquals(182.0, patched1.height)       // out dto: Double? — ок
            assertEquals(78.0, patched1.weight)
            assertEquals("ул. Пушкина, 1", patched1.address)
            assertEquals("мама", patched1.emergencyContactName)
            assertEquals("+7 900 000-00-00", patched1.emergencyContactNumber)
            assertEquals("O+", patched1.bloodType)
            assertEquals("ОМС 77-123456", patched1.dmsOms)

            val patched2 = repo.patchClientByUserId(
                userId = clientUser.id,
                patch = ClientPatch(address = "пр-т Колотушкина, 2")
            )

            assertEquals("пр-т Колотушкина, 2", patched2.address)
            assertEquals(182.0, patched2.height)
            assertEquals(78.0, patched2.weight)
            assertEquals("мама", patched2.emergencyContactName)
        } finally {
            repo.close()
        }
    }

    @Test
    fun doctor_edit_realServer() = runBlocking {
        val repo = ApiUserRepo(baseUrl)
        try {
            val uniq = System.currentTimeMillis().toString()
            val email = "dr_edit_$uniq@example.com"
            val login = "dr_edit_$uniq"
            val password = "secret123"

            val doctorUser = repo.saveByApi(
                UserInDto(
                    email = email,
                    login = login,
                    password = password,
                    role = "DOCTOR",
                    firstName = "John",
                    lastName = "Doe"
                )
            )
            assertTrue(doctorUser.id > 0)

            val doctor = repo.findDoctorByUserId(doctorUser.id) ?: fail("doctor not found after registration")
            val doctorId = doctor.id

            val patched1 = repo.patchDoctorByUserId(
                userId = doctorUser.id,
                patch = DoctorPatch(
                    profession = "therapist",
                    info = "приём пн–пт 10:00–18:00",
                    experience = 7,
                    price = 1500.0,
                    isConfirmed = true,
                    clinicId = 1
                )
            )

            assertEquals(doctorId, patched1.id)
            assertEquals(doctorUser.id, patched1.userId)
            assertEquals("therapist", patched1.profession)
            assertEquals("приём пн–пт 10:00–18:00", patched1.info)
            assertEquals(7, patched1.experience)
            assertEquals(1500.0, patched1.price)
            assertEquals(true, patched1.isConfirmed)

            val patched2 = repo.patchDoctorByUserId(
                userId = doctorUser.id,
                patch = DoctorPatch(price = 2000.0)
            )

            assertEquals(2000.0, patched2.price)
            assertEquals("therapist", patched2.profession)
        } finally {
            repo.close()
        }
    }

    @Test
    fun patchClientByUserId_notFound_shouldFail_realServer() = runBlocking {
        val repo = ApiUserRepo(baseUrl)
        try {
            assertFailsWith<IllegalArgumentException> {
                repo.patchClientByUserId(
                    userId = 9_000_000_000_000L,
                    patch = ClientPatch(address = "addr")
                )
            }
            Unit // <-- чтобы runBlocking вернул Unit
        } finally {
            repo.close()
        }
    }

    @Test
    fun patchDoctorByUserId_notFound_shouldFail_realServer() = runBlocking {
        val repo = ApiUserRepo(baseUrl)
        try {
            assertFailsWith<IllegalArgumentException> {
                repo.patchDoctorByUserId(
                    userId = 9_000_000_000_000L,
                    patch = DoctorPatch(info = "n/a")
                )
            }
            Unit // <-- чтобы runBlocking вернул Unit
        } finally {
            repo.close()
        }
    }
}
