package org.interns.project.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = System.getenv("DATABASE_URL")
                ?: "jdbc:postgresql://localhost:5432/usersdb"
            driverClassName = "org.postgresql.Driver"
            username = System.getenv("POSTGRES_USER") ?: "app"
            password = System.getenv("POSTGRES_PASSWORD") ?: "secret"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        val ds = HikariDataSource(config)
        Database.connect(ds)
    }
}