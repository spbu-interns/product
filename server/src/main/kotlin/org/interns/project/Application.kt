package org.interns.project

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.serialization.jackson.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.interns.project.config.SecurityConfig

const val SERVER_PORT = 8000

fun main() {
    embeddedServer(
        Netty,
        port = SERVER_PORT,
        host = "0.0.0.0"
    ) { module() }.start(wait = true)
}

fun Application.module() {
    SecurityConfig.initConfig(environment)
    log.info("bcryptCost = ${SecurityConfig.bcryptCost}")

    install(ContentNegotiation) {
        jackson {
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        }
    }

    installEmailFeatures()

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
    }
}
