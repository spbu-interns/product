package org.interns.project

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.serialization.jackson.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import org.interns.project.auth.authRoutes
import org.jetbrains.exposed.sql.Database

import io.ktor.serialization.jackson.*
import io.ktor.server.plugins.contentnegotiation.*
import org.interns.project.users.routes.registerUserRoutes
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import io.ktor.server.response.respondText
import org.interns.project.config.SecurityConfig

const val SERVER_PORT = 8080

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {

    install(ContentNegotiation) {
        jackson {
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        }
    }
    SecurityConfig.initConfig(environment)
    
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/usersdb",
        driver = "org.postgresql.Driver",
        user = "app",
        password = "secret"
    )

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
        registerUserRoutes()
    }

    authRoutes()
}




