package org.interns.project

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.interns.project.auth.routes.AuthController
import org.interns.project.auth.routes.fastApiCompatRoutes
import org.interns.project.config.SecurityConfig
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
    SecurityConfig.initConfig(environment)
    log.info("bcryptCost = ${SecurityConfig.bcryptCost}")

    //временные меры
    val dbHost = System.getenv("DB_HOST") ?: "localhost"
    val dbPort = System.getenv("DB_PORT") ?: "5432"
    val dbName = System.getenv("DB_NAME") ?: "usersdb"
    val dbUser = System.getenv("DB_USER") ?: "app"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "secret"
    
    val jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/$dbName"
    log.info("Connecting to database at $jdbcUrl")
    
    Database.connect(
        url = jdbcUrl,
        driver = "org.postgresql.Driver",
        user = dbUser,
        password = dbPassword
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
