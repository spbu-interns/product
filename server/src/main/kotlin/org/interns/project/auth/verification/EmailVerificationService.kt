package org.interns.project.auth.verification

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import org.interns.project.auth.UsersTable
import org.interns.project.notifications.Mailer
import org.interns.project.notifications.EmailTemplates
import org.interns.project.security.token.EmailVerificationRepo
import org.jetbrains.exposed.sql.selectAll

class EmailVerificationService(
    private val repo: EmailVerificationRepo,
    private val mailer: Mailer,
    private val ttlMinutes: Int,
    private val maxAttempts: Int
) {
    fun sendCode(userId: Long) {
        val (email, verified) = transaction {
            UsersTable.selectAll().where { UsersTable.id eq userId }.single().let {
                it[UsersTable.email] to (it[UsersTable.emailVerifiedAt] != null)
            }
        }
        if (verified) return

        val code = (100000..999999).random().toString()
        repo.createCode(userId, code, ttlMinutes)
        mailer.send(EmailTemplates.emailVerificationCode(to = email, code = code, ttlMinutes = ttlMinutes))
    }

    fun sendCodeByEmail(email: String): Boolean {
        val row = transaction { UsersTable.selectAll().where { UsersTable.email eq email }.singleOrNull() } ?: return false
        val userId = row[UsersTable.id]
        val already = row[UsersTable.emailVerifiedAt] != null
        if (already) return false
        sendCode(userId)
        return true
    }

    fun verifyByToken(token: String): Boolean {
        val userId = repo.consumeByCode(token) ?: return false
        transaction {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[emailVerifiedAt] = Instant.now()
            }
        }
        return true
    }

    fun verifyCode(userId: Long, code: String): Boolean =
        repo.verify(userId, code, maxAttempts).ok
}
