package org.interns.project.users.repo

import java.time.LocalDate
import java.time.LocalDateTime
import org.interns.project.users.model.DoctorSearchFilter
import org.interns.project.users.model.DoctorSearchResult
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.groupConcat
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.andWhere

object SearchUsersTable : Table("users") {
    val id: Column<Long> = long("id")
    val name: Column<String?> = varchar("name", 255).nullable()
    val surname: Column<String?> = varchar("surname", 255).nullable()
    val gender: Column<String?> = varchar("gender", 16).nullable()
    val dateOfBirth: Column<LocalDate?> = date("date_of_birth").nullable()
    override val primaryKey = PrimaryKey(id)
}

object ClinicsTable : Table("clinics") {
    val id: Column<Long> = long("id")
    val city: Column<String?> = varchar("city", 255).nullable()
    val region: Column<String?> = varchar("region", 255).nullable()
    val metro: Column<String?> = varchar("metro", 255).nullable()
    override val primaryKey = PrimaryKey(id)
}

object DoctorsTable : Table("doctors") {
    val id: Column<Long> = long("id")
    val userId: Column<Long> = long("user_id")
    val clinicId: Column<Long?> = long("clinic_id").nullable()
    val profession: Column<String> = varchar("profession", 255)
    val info: Column<String?> = text("info").nullable()
    val isConfirmed: Column<Boolean?> = bool("is_confirmed").nullable()
    val rating: Column<Double?> = double("rating").nullable()
    val experience: Column<Int?> = integer("experience").nullable()
    val price: Column<Double?> = double("price").nullable()
    val onlineAvailable: Column<Boolean> = bool("online_available")
    override val primaryKey = PrimaryKey(id)
}

object SpecializationsTable : Table("specializations") {
    val id: Column<Long> = long("id")
    val name: Column<String> = varchar("name", 255)
    override val primaryKey = PrimaryKey(id)
}

object DoctorSpecializationsTable : Table("doctor_specializations") {
    val doctorId: Column<Long> = long("doctor_id")
    val specializationId: Column<Long> = long("specialization_id")
}

object AppointmentSlotsTable : Table("appointment_slots") {
    val id: Column<Long> = long("id")
    val doctorId: Column<Long> = long("doctor_id")
    val startTime: Column<LocalDateTime> = datetime("start_time")
    val isBooked: Column<Boolean> = bool("is_booked")
    override val primaryKey = PrimaryKey(id)
}

class DoctorSearchRepository(private val db: Database) {
    fun search(filter: DoctorSearchFilter): List<DoctorSearchResult> = transaction(db) {
        val specializationNames = SpecializationsTable
            .name
            .groupConcat(separator = ",")
            .alias("specialization_names")

        val baseQuery = DoctorsTable
            .join(SearchUsersTable, JoinType.INNER, additionalConstraint = { DoctorsTable.userId eq SearchUsersTable.id })
            .join(ClinicsTable, JoinType.LEFT, additionalConstraint = { DoctorsTable.clinicId eq ClinicsTable.id })
            .join(DoctorSpecializationsTable, JoinType.LEFT, additionalConstraint = { DoctorsTable.id eq DoctorSpecializationsTable.doctorId })
            .join(SpecializationsTable, JoinType.LEFT, additionalConstraint = { DoctorSpecializationsTable.specializationId eq SpecializationsTable.id })
            .select(
                DoctorsTable.id,
                DoctorsTable.userId,
                DoctorsTable.clinicId,
                DoctorsTable.profession,
                DoctorsTable.info,
                DoctorsTable.isConfirmed,
                DoctorsTable.rating,
                DoctorsTable.experience,
                DoctorsTable.price,
                DoctorsTable.onlineAvailable,
                SearchUsersTable.gender,
                SearchUsersTable.dateOfBirth,
                ClinicsTable.city,
                ClinicsTable.region,
                ClinicsTable.metro,
                specializationNames
            )

        filter.specializationIds?.takeIf { it.isNotEmpty() }?.let { ids ->
            baseQuery.andWhere { DoctorSpecializationsTable.specializationId inList ids.map(Int::toLong) }
        }
        filter.city?.let { city -> baseQuery.andWhere { ClinicsTable.city eq city } }
        filter.region?.let { region -> baseQuery.andWhere { ClinicsTable.region eq region } }
        filter.metro?.let { metro -> baseQuery.andWhere { ClinicsTable.metro eq metro } }
        if (filter.onlineOnly) {
            baseQuery.andWhere { DoctorsTable.onlineAvailable eq true }
        }
        filter.minPrice?.let { min -> baseQuery.andWhere { DoctorsTable.price greaterEq min } }
        filter.maxPrice?.let { max -> baseQuery.andWhere { DoctorsTable.price lessEq max } }
        filter.minRating?.let { min -> baseQuery.andWhere { DoctorsTable.rating greaterEq min } }
        filter.gender?.let { gender -> baseQuery.andWhere { SearchUsersTable.gender eq gender } }

        val today = LocalDate.now()
        filter.minAge?.let { minAge ->
            val latestBirthDate = today.minusYears(minAge.toLong())
            baseQuery.andWhere { SearchUsersTable.dateOfBirth lessEq latestBirthDate }
        }
        filter.maxAge?.let { maxAge ->
            val earliestBirthDate = today.minusYears(maxAge.toLong() + 1).plusDays(1)
            baseQuery.andWhere { SearchUsersTable.dateOfBirth greaterEq earliestBirthDate }
        }

        filter.minExperience?.let { minExp -> baseQuery.andWhere { DoctorsTable.experience greaterEq minExp } }
        filter.maxExperience?.let { maxExp -> baseQuery.andWhere { DoctorsTable.experience lessEq maxExp } }

        filter.date?.let { dateString ->
            val requestedDate = LocalDate.parse(dateString)
            baseQuery.andWhere {
                exists(
                    AppointmentSlotsTable
                        .select(AppointmentSlotsTable.id)
                        .adjustWhere { AppointmentSlotsTable.doctorId eq DoctorsTable.id }
                        .adjustWhere { AppointmentSlotsTable.startTime.date() eq requestedDate }
                        .adjustWhere { AppointmentSlotsTable.isBooked eq false }
                )
            }
        }

        baseQuery
            .groupBy(
                DoctorsTable.id,
                DoctorsTable.userId,
                DoctorsTable.clinicId,
                DoctorsTable.profession,
                DoctorsTable.info,
                DoctorsTable.isConfirmed,
                DoctorsTable.rating,
                DoctorsTable.experience,
                DoctorsTable.price,
                DoctorsTable.onlineAvailable,
                SearchUsersTable.gender,
                SearchUsersTable.dateOfBirth,
                ClinicsTable.city,
                ClinicsTable.region,
                ClinicsTable.metro
            )
            .orderBy(DoctorsTable.rating to SortOrder.DESC, DoctorsTable.price to SortOrder.ASC)
            .limit(filter.limit).offset(start = filter.offset.toLong())
            .map { row ->
                val names = row[specializationNames].split(',').filter { it.isNotBlank() }
                DoctorSearchResult(
                    id = row[DoctorsTable.id],
                    userId = row[DoctorsTable.userId],
                    clinicId = row[DoctorsTable.clinicId],
                    profession = row[DoctorsTable.profession],
                    info = row[DoctorsTable.info],
                    isConfirmed = row[DoctorsTable.isConfirmed],
                    rating = row[DoctorsTable.rating],
                    experience = row[DoctorsTable.experience],
                    price = row[DoctorsTable.price],
                    onlineAvailable = row[DoctorsTable.onlineAvailable],
                    gender = row[SearchUsersTable.gender],
                    dateOfBirth = row[SearchUsersTable.dateOfBirth]?.toString(),
                    city = row[ClinicsTable.city],
                    region = row[ClinicsTable.region],
                    metro = row[ClinicsTable.metro],
                    specializationNames = names
                )
            }
    }
}