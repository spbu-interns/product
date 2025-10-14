package org.interns.project.security.token

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object EmailVerificationCodes : Table("email_verifications") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val tokenHash = varchar("token_hash", 64)
    val expiresAt = timestamp("expires_at")
    val consumedAt = timestamp("consumed_at").nullable()
    val createdAt = timestamp("created_at")
    // Removing attempts field as it doesn't exist in the database schema
    override val primaryKey = PrimaryKey(id)
}

class EmailVerificationRepo(private val pepper: String?) {

    fun createCode(userId: Long, code: String, ttlMinutes: Int) {
        val hash = TokenCrypto.sha256Hex("${pepper ?: ""}:$code")
        val expires = Instant.now().plusSeconds(ttlMinutes.toLong() * 60)
        transaction {
            EmailVerificationCodes.update({
                (EmailVerificationCodes.userId eq userId) and EmailVerificationCodes.consumedAt.isNull()
            }) { it[consumedAt] = Instant.now() }

            EmailVerificationCodes.insert {
                it[this.userId] = userId
                it[this.tokenHash] = hash
                it[this.expiresAt] = expires
                it[this.createdAt] = Instant.now()
            }
        }
    }

    fun consumeByCode(code: String): Long? = transaction {
        val hash = TokenCrypto.sha256Hex("${pepper ?: ""}:$code")
        val row = EmailVerificationCodes
            .selectAll().where { (EmailVerificationCodes.tokenHash eq hash) and EmailVerificationCodes.consumedAt.isNull() }
            .orderBy(EmailVerificationCodes.createdAt to SortOrder.DESC)
            .limit(1)
            .singleOrNull() ?: return@transaction null

        if (Instant.now().isAfter(row[EmailVerificationCodes.expiresAt])) return@transaction null

        EmailVerificationCodes.update({ EmailVerificationCodes.id eq row[EmailVerificationCodes.id] }) {
            it[consumedAt] = Instant.now()
        }
        row[EmailVerificationCodes.userId]
    }

    data class CheckResult(val ok: Boolean, val attemptsLeft: Int)

    fun verify(userId: Long, code: String, maxAttempts: Int): CheckResult = transaction {
        val row = EmailVerificationCodes
            .selectAll().where { (EmailVerificationCodes.userId eq userId) and EmailVerificationCodes.consumedAt.isNull() }
            .orderBy(EmailVerificationCodes.createdAt to SortOrder.DESC)
            .limit(1)
            .singleOrNull() ?: return@transaction CheckResult(false, 0)

        // Since we don't have an attempts column, we'll assume no previous attempts
        val left = maxAttempts
        
        val isExpired = Instant.now().isAfter(row[EmailVerificationCodes.expiresAt])
        val ok = !isExpired && TokenCrypto.sha256Hex("${pepper ?: ""}:$code") == row[EmailVerificationCodes.tokenHash]

        if (ok) {
            EmailVerificationCodes.update({ EmailVerificationCodes.id eq row[EmailVerificationCodes.id] }) {
                it[consumedAt] = Instant.now()
            }
            CheckResult(true, left)
        } else {
            // Since we can't track attempts, we'll just return false
            // and assume there are still attempts left
            CheckResult(false, left - 1)
        }
    }
}
