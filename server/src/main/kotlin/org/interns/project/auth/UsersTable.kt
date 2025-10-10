package org.interns.project.auth

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UsersTable : Table("users") {
    val id = long("id")
    val email = varchar("email", 255)
    val login = varchar("login", 100)
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 20)
    val emailVerifiedAt = timestamp("email_verified_at").nullable()
    val passwordChangedAt = timestamp("password_changed_at")
    override val primaryKey = PrimaryKey(id)
}

