package org.interns.project.security.token

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object PasswordResetTokens : Table("password_reset_tokens") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val tokenHash = varchar("token_hash", 64)
    val expiresAt = timestamp("expires_at")
    val consumedAt = timestamp("consumed_at").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}