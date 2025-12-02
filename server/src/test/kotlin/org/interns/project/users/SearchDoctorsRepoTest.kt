package org.interns.project.users

import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.interns.project.users.model.DoctorSearchFilter
import org.interns.project.users.repo.AppointmentSlotsTable
import org.interns.project.users.repo.ClinicsTable
import org.interns.project.users.repo.DoctorSearchRepository
import org.interns.project.users.repo.DoctorSpecializationsTable
import org.interns.project.users.repo.DoctorsTable
import org.interns.project.users.repo.SearchUsersTable
import org.interns.project.users.repo.SpecializationsTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class SearchDoctorsRepoTest {
    private lateinit var db: Database
    private lateinit var repository: DoctorSearchRepository

    @BeforeTest
    fun setup() {
        Class.forName("org.h2.Driver")
        db = Database.connect(
            url = "jdbc:h2:mem:doctors;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )
        transaction(db) {
            SchemaUtils.create(
                SearchUsersTable,
                ClinicsTable,
                DoctorsTable,
                SpecializationsTable,
                DoctorSpecializationsTable,
                AppointmentSlotsTable
            )
        }
        repository = DoctorSearchRepository(db)
    }

    @AfterTest
    fun tearDown() {
        if (::db.isInitialized) {
            transaction(db) {
                SchemaUtils.drop(
                    AppointmentSlotsTable,
                    DoctorSpecializationsTable,
                    SpecializationsTable,
                    DoctorsTable,
                    ClinicsTable,
                    SearchUsersTable
                )
            }
        }
    }

    @Test
    fun `searchDoctors reads data from test tables`() = runBlocking {
        transaction(db) {
            SearchUsersTable.insert {
                it[id] = 10
                it[name] = "Анна"
                it[surname] = "Петрова"
                it[gender] = "FEMALE"
                it[dateOfBirth] = LocalDate.of(1980, 5, 5)
            }
            SearchUsersTable.insert {
                it[id] = 20
                it[name] = "Борис"
                it[surname] = "Иванов"
                it[gender] = "MALE"
                it[dateOfBirth] = LocalDate.of(2000, 1, 1)
            }

            ClinicsTable.insert {
                it[id] = 3
                it[city] = "Москва"
                it[region] = "ЦАО"
                it[metro] = "Охотный Ряд"
            }
            ClinicsTable.insert {
                it[id] = 4
                it[city] = "Санкт-Петербург"
                it[region] = "Центральный"
                it[metro] = "Невский проспект"
            }

            DoctorsTable.insert {
                it[id] = 1
                it[userId] = 10
                it[clinicId] = 3
                it[profession] = "Кардиолог"
                it[info] = "Работает по вечерам"
                it[isConfirmed] = true
                it[rating] = 4.8
                it[experience] = 12
                it[price] = 1500.0
                it[onlineAvailable] = true
            }
            DoctorsTable.insert {
                it[id] = 2
                it[userId] = 20
                it[clinicId] = 4
                it[profession] = "Терапевт"
                it[info] = "Только очно"
                it[isConfirmed] = true
                it[rating] = 3.5
                it[experience] = 2
                it[price] = 800.0
                it[onlineAvailable] = false
            }

            SpecializationsTable.insert {
                it[id] = 1
                it[name] = "Кардиолог"
            }
            SpecializationsTable.insert {
                it[id] = 2
                it[name] = "Терапевт"
            }
            SpecializationsTable.insert {
                it[id] = 3
                it[name] = "Офтальмолог"
            }

            DoctorSpecializationsTable.insert {
                it[doctorId] = 1
                it[specializationId] = 1
            }
            DoctorSpecializationsTable.insert {
                it[doctorId] = 1
                it[specializationId] = 2
            }
            DoctorSpecializationsTable.insert {
                it[doctorId] = 2
                it[specializationId] = 3
            }

            AppointmentSlotsTable.insert {
                it[id] = 100
                it[doctorId] = 1
                it[startTime] = LocalDateTime.of(2024, 12, 1, 10, 0)
                it[isBooked] = false
            }
            AppointmentSlotsTable.insert {
                it[id] = 200
                it[doctorId] = 2
                it[startTime] = LocalDateTime.of(2024, 12, 1, 10, 0)
                it[isBooked] = true
            }
        }

        val filter = DoctorSearchFilter(
            specializationIds = listOf(1, 2),
            city = "Москва",
            region = "ЦАО",
            metro = "Охотный Ряд",
            onlineOnly = true,
            minPrice = 1000.0,
            maxPrice = null,
            minRating = 4.0,
            gender = "FEMALE",
            minAge = 25,
            maxAge = null,
            minExperience = 5,
            maxExperience = 20,
            date = "2024-12-01",
            limit = 25,
            offset = 0
        )

        val results = repository.search(filter)

        assertEquals(1, results.size)
        val doctor = results.first()
        assertEquals(1, doctor.id)
        assertEquals(10, doctor.userId)
        assertEquals(3, doctor.clinicId)
        assertEquals("Кардиолог", doctor.profession)
        assertEquals(listOf("Кардиолог", "Терапевт"), doctor.specializationNames)
        assertTrue(doctor.onlineAvailable)
        assertEquals("Москва", doctor.city)
        assertEquals("ЦАО", doctor.region)
        assertEquals("Охотный Ряд", doctor.metro)
    }
}