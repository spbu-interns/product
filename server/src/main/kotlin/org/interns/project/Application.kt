package org.interns.project

import io.ktor.http.HttpHeaders
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
import org.interns.project.appointments.AppointmentController
import org.interns.project.controller.AuthController
import org.interns.project.auth.routes.fastApiCompatRoutes
import org.interns.project.config.AppConfig
import org.interns.project.controller.FindDoctorsController
import org.interns.project.controller.PatientDataController
import org.interns.project.controller.ProfileController
import org.interns.project.controller.UserController
import org.interns.project.users.repo.ApiUserRepo
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

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
    transaction { exec("select 1") {} }
    log.info("Database connection established (user=${AppConfig.dbUser})")

    install(CORS) {
        anyHost()
        allowCredentials = true
        allowHeader("Content-Type")
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
        allowMethod(io.ktor.http.HttpMethod.Patch)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.AcceptLanguage)
        allowHeader(HttpHeaders.Origin)
        allowHeader("X-Requested-With")
        allowHeader("X-Csrf-Token")
        allowNonSimpleContentTypes = true
        exposeHeader(HttpHeaders.Location)
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
    val patientDataController = PatientDataController(apiUserRepo)
    val appointmentController = AppointmentController(apiUserRepo)
    val userController = UserController(apiUserRepo)
    val findDoctorsController = FindDoctorsController(apiUserRepo)
    val profileController = ProfileController(apiUserRepo)

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }

        authController.registerRoutes(this)
        patientDataController.registerRoutes(this)
        userController.registerRoutes(this)
        appointmentController.registerRoutes(this)
        findDoctorsController.registerRoutes(this)
        profileController.registerRoutes(this)

        fastApiCompatRoutes(verificationService, passwordResetService)
    }
}
