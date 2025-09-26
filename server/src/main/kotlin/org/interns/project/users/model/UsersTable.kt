package org.interns.project.users.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UsersTable : Table("users") {
    val id = long("id").autoIncrement()
    val email = varchar("email", 255).uniqueIndex()
    val login = varchar("login", 128).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 32)
    val firstName = varchar("first_name", 128).nullable()
    val lastName = varchar("last_name", 128).nullable()
    val patronymic = varchar("patronymic", 128).nullable()
    val phoneNumber = varchar("phone_number", 32).nullable()
    val isActive = bool("is_active").default(true)
    val clinicId = long("clinic_id").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}