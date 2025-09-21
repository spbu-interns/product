package org.interns.project.config

import io.ktor.server.application.*
import kotlin.properties.Delegates

object SecurityConfig {
    var bcryptCost by Delegates.notNull<Int>()
        private set

    fun initConfig(environment: ApplicationEnvironment) {
        bcryptCost = environment.config
            .propertyOrNull("security.passwordHash.bcryptCost")
            ?.getString()?.toInt()
            ?: error("Missing config: security.bcryptCost")
    }
}

