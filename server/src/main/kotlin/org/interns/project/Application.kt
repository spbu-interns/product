package org.interns.project

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.serialization.jackson.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.interns.project.auth.authRoutes
import org.interns.project.config.SecurityConfig
import org.interns.project.users.routes.userRoutes

const val SERVER_PORT = 8080

fun main() {
    embeddedServer(
        Netty,
        port = SERVER_PORT,
        host = "0.0.0.0"
    ) { module() }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        jackson {
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        }
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowCredentials = true
        allowNonSimpleContentTypes = true
        allowSameOrigin = true

        // Для разработки
        allowHost("localhost:8000", schemes = listOf("http"))
        allowHost("localhost:8080", schemes = listOf("http"))
        allowHost("127.0.0.1:8000", schemes = listOf("http"))
        allowHost("127.0.0.1:8080", schemes = listOf("http"))
    }

    SecurityConfig.initConfig(environment)

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }

        get("/api/ping") {
            call.respond(mapOf(
                "status" to "ok",
                "message" to "API server is running",
                "timestamp" to System.currentTimeMillis()
            ))
        }

        authRoutes()
        userRoutes()
    }
}
