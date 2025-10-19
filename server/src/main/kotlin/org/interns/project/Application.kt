package org.interns.project

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.interns.project.auth.routes.AuthController
import org.interns.project.auth.routes.fastApiCompatRoutes
import org.interns.project.config.AppConfig
import org.interns.project.users.repo.ApiUserRepo
import org.jetbrains.exposed.sql.Database

const val SERVER_PORT = 8000

fun main() {
    embeddedServer(
        Netty,
        port = SERVER_PORT,
        host = "0.0.0.0"
    ) { module() }.start(wait = true)
}

fun Application.module() {
    val jdbcUrl = "jdbc:postgresql://${AppConfig.dbHost}:${AppConfig.dbPort}/${AppConfig.dbName}"
    log.info("Connecting to database at $jdbcUrl")

    Database.connect(
        url = jdbcUrl,
        driver = "org.postgresql.Driver",
        user = AppConfig.dbUser,
        password = AppConfig.dbPassword
    )
    log.info("Database connection established")

    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
    }

    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            isLenient = true
        })
    }

    val (verificationService, passwordResetService) = installEmailFeatures()
    val apiUserRepo = ApiUserRepo()

    val authController = AuthController(
        apiUserRepo = apiUserRepo,
        verificationService = verificationService,
        passwordResetService = passwordResetService
    )

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }

        authController.registerRoutes(this)

        fastApiCompatRoutes(verificationService, passwordResetService)
    }
}
