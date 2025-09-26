package org.interns.project.users.repo

import org.interns.project.users.model.User
import org.interns.project.users.model.UsersTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class PostgresUserRepo : UserRepo {
    override fun nextId(): Long {
        return 0L
    }

    override fun save(user: User): User {
        return transaction {
            val generatedId: Long = UsersTable.insert { itRow ->
                itRow[email] = user.email
                itRow[login] = user.login
                itRow[passwordHash] = user.passwordHash
                itRow[role] = user.role
                itRow[firstName] = user.firstName
                itRow[lastName] = user.lastName
                itRow[patronymic] = user.patronymic
                itRow[phoneNumber] = user.phoneNumber
                itRow[isActive] = user.isActive
                itRow[clinicId] = user.clinicId
            } get UsersTable.id

            val row = UsersTable.selectAll().where { UsersTable.id eq generatedId }.single()
            rowToUser(row)
        }
    }

    override fun findByEmail(email: String): User? = transaction {
        UsersTable.selectAll().where { UsersTable.email eq email }.map { rowToUser(it) }.singleOrNull()
    }

    override fun findByLogin(login: String): User? = transaction {
        UsersTable.selectAll().where { UsersTable.login eq login }.map { rowToUser(it) }.singleOrNull()
    }

    override fun clear() {
        transaction { UsersTable.deleteAll() }
    }

    private fun rowToUser(row: ResultRow): User {
        val createdInstant: Instant = row[UsersTable.createdAt]
        val updatedInstant: Instant = row[UsersTable.updatedAt]

        return User(
            id = row[UsersTable.id],
            email = row[UsersTable.email],
            login = row[UsersTable.login],
            passwordHash = row[UsersTable.passwordHash],
            role = row[UsersTable.role],
            firstName = row[UsersTable.firstName],
            lastName = row[UsersTable.lastName],
            patronymic = row[UsersTable.patronymic],
            phoneNumber = row[UsersTable.phoneNumber],
            isActive = row[UsersTable.isActive],
            clinicId = row[UsersTable.clinicId],
            createdAt = createdInstant,
            updatedAt = updatedInstant
        )
    }
}
