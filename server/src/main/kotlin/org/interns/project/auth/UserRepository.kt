package org.interns.project.auth

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object UsersTable : Table("users") {
    val id = long("id")
    val login = varchar("login", 100)
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 20)
}

open class UserRepository {

    open fun findByLogin(login: String): User? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.login eq login }
            .map {
                User(
                    id = it[UsersTable.id],
                    login = it[UsersTable.login],
                    passwordHash = it[UsersTable.passwordHash],
                    role = it[UsersTable.role]
                )
            }
            .singleOrNull()
    }
}
