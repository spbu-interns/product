package org.interns.project.security.token

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class PasswordResetRepo(private val pepper: String?) {
    data class PlainToken(val plain: String)

    fun create(userId: Long, ttlMinutes: Int): PlainToken {
        val plain = TokenCrypto.randomToken(32)
        val hash = TokenCrypto.sha256Hex("${pepper ?: ""}:$plain")
        val expires = Instant.now().plusSeconds(ttlMinutes.toLong() * 60)

        transaction {
            PasswordResetTokens.update({
                (PasswordResetTokens.userId eq userId) and PasswordResetTokens.consumedAt.isNull()
            }) { it[consumedAt] = Instant.now() }

            PasswordResetTokens.insert {
                it[this.userId] = userId
                it[this.tokenHash] = hash
                it[this.expiresAt] = expires
                it[this.createdAt] = Instant.now()
            }
        }
        return PlainToken(plain)
    }

    fun consume(plain: String): Long? = transaction {
        val hash = TokenCrypto.sha256Hex("${pepper ?: ""}:$plain")
        val row = PasswordResetTokens
            .selectAll().where { (PasswordResetTokens.tokenHash eq hash) and PasswordResetTokens.consumedAt.isNull() }
            .singleOrNull() ?: return@transaction null

        if (Instant.now().isAfter(row[PasswordResetTokens.expiresAt])) return@transaction null

        PasswordResetTokens.update({ PasswordResetTokens.id eq row[PasswordResetTokens.id] }) {
            it[consumedAt] = Instant.now()
        }
        row[PasswordResetTokens.userId]
    }
}
