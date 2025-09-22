package org.interns.project.config

import io.ktor.server.application.*

object SecurityConfig {
    private var _bcryptCost: Int? = null

    val bcryptCost: Int
        get() = requireNotNull(_bcryptCost) { "SecurityConfig not initialized! Call initConfig() or initForTests()." }

    fun initConfig(environment: ApplicationEnvironment) {
        if (_bcryptCost != null) return

        _bcryptCost = environment.config
            .propertyOrNull("security.passwordHash.bcryptCost")
            ?.getString()
            ?.toIntOrNull()
            ?: throw IllegalStateException("Missing config: security.bcryptCost")
    }

    fun initForTests(cost: Int = 12) {
        _bcryptCost = cost
    }
}

