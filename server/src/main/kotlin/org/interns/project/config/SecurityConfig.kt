package org.interns.project.config

import io.ktor.server.application.*

object SecurityConfig {
    private var _bcryptCost: Int? = null

    val bcryptCost: Int
        get() = requireNotNull(_bcryptCost) { "SecurityConfig not initialized! Call initConfig() or initForTests()." }

    fun initConfig(env: ApplicationEnvironment) {
        val cfg = env.config

        val candidates = listOfNotNull(
            cfg.propertyOrNull("security.passwordHash.bcryptCost")?.getString(),
            cfg.propertyOrNull("security.bcryptCost")?.getString(),
            System.getProperty("security.passwordHash.bcryptCost"),
            System.getProperty("security.bcryptCost"),
            System.getenv("BCRYPT_COST")
        )

        val raw = candidates.firstOrNull()

        if (raw == null) {
            _bcryptCost = 12
            println("WARN: bcryptCost not found in config/system/env. Using default 12.")
        } else {
            _bcryptCost = raw.toInt()
        }

        println(
            "SECURITY_CONFIG >> bcryptCost=" + _bcryptCost +
                    " (nested=" + cfg.propertyOrNull("security.passwordHash.bcryptCost")?.getString() +
                    ", flat=" + cfg.propertyOrNull("security.bcryptCost")?.getString() +
                    ", sys.nested=" + System.getProperty("security.passwordHash.bcryptCost") +
                    ", sys.flat=" + System.getProperty("security.bcryptCost") +
                    ", env.BCRYPT_COST=" + System.getenv("BCRYPT_COST") + ")"
        )
    }

    fun initForTests(cost: Int = 12) {
        _bcryptCost = cost
    }
}

