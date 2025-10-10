package org.interns.project.auth

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

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
