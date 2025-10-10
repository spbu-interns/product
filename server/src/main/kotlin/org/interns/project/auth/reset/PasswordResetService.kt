package org.interns.project.auth.reset

import at.favre.lib.crypto.bcrypt.BCrypt
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import org.interns.project.auth.UsersTable
import org.interns.project.notifications.EmailTemplates
import org.interns.project.notifications.Mailer
import org.interns.project.security.token.PasswordResetRepo
import org.jetbrains.exposed.sql.selectAll

class PasswordResetService(
    private val repo: PasswordResetRepo,
    private val mailer: Mailer,
    private val baseUrl: String,
    private val ttlMinutes: Int,
    private val sessionTtlMinutes: Int,
    private val bcryptCost: Int
) {
    fun sessionTtlSeconds(): Int = sessionTtlMinutes * 60

    fun requestByEmail(email: String): Boolean {
        val pair = transaction {
            UsersTable.selectAll().where { UsersTable.email eq email }
                .singleOrNull()
                ?.let { it[UsersTable.id] to (it[UsersTable.emailVerifiedAt] != null) }
        } ?: return false

        val (userId, verified) = pair
        if (!verified) return false

        val token = repo.create(userId, ttlMinutes).plain
        val link = "$baseUrl/auth/password/reset?token=$token" // под FastAPI-ручку
        mailer.send(EmailTemplates.passwordResetLink(to = email, link = link, ttlMinutes = ttlMinutes))
        return true
    }

    fun verifyLink(token: String): Long? = repo.consume(token)

    fun completeReset(userId: Long, newPassword: String) {
        val hash = BCrypt.withDefaults().hashToString(bcryptCost, newPassword.toCharArray())
        transaction {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[passwordHash] = hash
                it[passwordChangedAt] = Instant.now()
            }
        }
    }
}
