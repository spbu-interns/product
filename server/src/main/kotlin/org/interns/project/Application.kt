package org.interns.project

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.interns.project.auth.authRoutes
import org.jetbrains.exposed.sql.Database

import io.ktor.serialization.jackson.*
import io.ktor.server.plugins.contentnegotiation.*

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {

    install(ContentNegotiation) {
        jackson()
    }

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
    }

    authRoutes()
}

