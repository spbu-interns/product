package org.interns.project.users.repo

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.interns.project.dto.AppointmentCreateRequest
import org.interns.project.dto.AppointmentDto
import org.interns.project.dto.SlotCreateRequest
import org.interns.project.dto.UserResponseDto
import org.interns.project.dto.ClientProfileDto
import org.interns.project.dto.DoctorPatientDto
import org.interns.project.dto.DoctorProfileDto
import org.interns.project.dto.MedicalRecordDto
import org.interns.project.users.model.User
import org.interns.project.users.model.UserInDto
import org.interns.project.users.model.UserOutDto
import org.interns.project.users.model.UserCreateRequest
import java.time.Instant
import org.interns.project.users.dto.ApiResponse
import org.interns.project.security.token.JwtService
import org.interns.project.users.model.*
import java.time.LocalDate

class ApiUserRepo(
    private val baseUrl: String = "http://127.0.0.1:8001",
    private val client: HttpClient = defaultClient()
) {
    companion object {
        fun defaultClient() = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                        explicitNulls = false
                    }
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 5_000
                connectTimeoutMillis = 2_000
                socketTimeoutMillis  = 5_000
            }
        }
    }

    fun close() = client.close()

    private fun String?.toInstantOrNull(): Instant? =
        this?.let { raw ->
            val text = raw.trim()
            runCatching { Instant.parse(text) }
                .getOrElse {
                    // если нет часового пояса — пробуем добавить 'Z'
                    runCatching { Instant.parse(text + "Z") }.getOrNull()
                }
        }

    private fun String?.toLocalDateOrNull(): LocalDate? =
        this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private fun fromOutDto(d: UserOutDto): User {
        val created = d.createdAt.toInstantOrNull()
        val updated = d.updatedAt.toInstantOrNull()
        val dob = d.dateOfBirth.toLocalDateOrNull()
        val emailVerified = d.emailVerifiedAt.toInstantOrNull()
        val passwordChanged = d.passwordChangedAt.toInstantOrNull()

        return User(
            id = d.id,
            email = d.email,
            login = d.login,
            passwordHash = "",          // пароль не приходит из api
            role = d.role,

            name = d.name,
            surname = d.surname,
            patronymic = d.patronymic,
            phoneNumber = d.phoneNumber,
            clinicId = d.clinicId,

            dateOfBirth = dob,
            avatar = d.avatar,
            gender = d.gender,

            isActive = d.isActive,
            createdAt = created,
            updatedAt = updated,
            emailVerifiedAt = emailVerified,
            passwordChangedAt = passwordChanged
        )
    }

    private fun fromSlotDto(dto: SlotOutDto): Slot {
        val start = dto.startTime.toInstantOrNull()
        val end = dto.endTime.toInstantOrNull()
        val created = dto.createdAt.toInstantOrNull()
        val updated = dto.updatedAt.toInstantOrNull()

        return Slot(
            id = dto.id,
            doctorId = dto.doctorId,
            startTime = start,
            endTime = end,
            durationMinutes = dto.duration,
            isBooked = dto.isBooked,
            createdAt = created,
            updatedAt = updated
        )
    }

    private fun fromAppointmentDto(dto: AppointmentOut): Appointment {
        val created = dto.createdAt.toInstantOrNull()
        val updated = dto.updatedAt.toInstantOrNull()
        val canceled = dto.canceledAt.toInstantOrNull()
        val completed = dto.completedAt.toInstantOrNull()

        return Appointment(
            id = dto.id,
            slotId = dto.slotId,
            clientId = dto.clientId,
            status = dto.status,
            comments = dto.comments,
            createdAt = created,
            updatedAt = updated,
            canceledAt = canceled,
            completedAt = completed,
            appointmentTypeId = dto.appointmentTypeId
        )
    }

    private fun urlEncode(s: String) = java.net.URLEncoder.encode(s, Charsets.UTF_8.name())

    private suspend fun <T> doGet(path: String, parse: suspend (HttpResponse) -> T?): T? {
        val resp = client.get("$baseUrl$path")
        if (resp.status != HttpStatusCode.OK) return null
        val text = resp.bodyAsText()
        if (text == "null" || text.isBlank()) return null
        return parse(resp)
    }

    private suspend fun <T> doPost(
        path: String,
        body: Any?,
        successCodes: Set<HttpStatusCode> = setOf(HttpStatusCode.Created, HttpStatusCode.OK),
        parse: suspend (HttpResponse) -> T
    ): T {
        val resp = client.post("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            setBody(body ?: emptyMap<String, Any>())
        }

        println("🔵 Request to: $baseUrl$path")
        println("🔵 Request body: ${body.toString()}")
        println("🔵 Response status: ${resp.status}")
        println("🔵 Response body: ${resp.bodyAsText()}")

        return when (resp.status) {
            in successCodes -> parse(resp)
            HttpStatusCode.UnprocessableEntity ->
                throw IllegalArgumentException("422 Unprocessable Entity: ${resp.bodyAsText()}")
            HttpStatusCode.Conflict ->
                throw IllegalStateException("409 Conflict: email or login already exists")
            HttpStatusCode.BadRequest ->
                throw IllegalArgumentException("400 Bad request: ${resp.bodyAsText()}")
            HttpStatusCode.Unauthorized ->
                throw IllegalArgumentException("401 Unauthorized: invalid credentials")
            else ->
                throw RuntimeException("Unexpected response: ${resp.status} ${resp.bodyAsText()}")
        }
    }

    private suspend fun <T> doPatch(
        path: String,
        body: Any?,
        parse: suspend (HttpResponse) -> T
    ): T {
        // Логируем тело запроса
        println("=== DEBUG DO PATCH ===")
        println("Path: $path")
        println("Body object: $body")

        val resp = client.patch("$baseUrl$path") {
            contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8)) // Явно указываем кодировку
            accept(ContentType.Application.Json.withCharset(Charsets.UTF_8))
            setBody(body ?: emptyMap<String, Any>())
        }

        println("🟣 PATCH $baseUrl$path -> ${resp.status}")
        println("🟣 Response: ${resp.bodyAsText()}")

        return when (resp.status) {
            HttpStatusCode.OK -> parse(resp)
            HttpStatusCode.UnprocessableEntity ->
                throw IllegalArgumentException("422 Unprocessable Entity: ${resp.bodyAsText()}")
            HttpStatusCode.BadRequest ->
                throw IllegalArgumentException("400 Bad request: ${resp.bodyAsText()}")
            HttpStatusCode.Unauthorized ->
                throw IllegalArgumentException("401 Unauthorized")
            HttpStatusCode.NotFound ->
                throw IllegalArgumentException("404 Not Found: ${resp.bodyAsText()}")
            else ->
                throw RuntimeException("Unexpected response: ${resp.status} ${resp.bodyAsText()}")
        }
    }

    private suspend fun doDelete(path: String): Boolean {
        val resp = client.delete("$baseUrl$path")
        println("🔴 DELETE $baseUrl$path -> ${resp.status}")
        return when (resp.status) {
            HttpStatusCode.NoContent -> true
            HttpStatusCode.NotFound -> false
            else -> throw RuntimeException("Unexpected response: ${resp.status} ${resp.bodyAsText()}")
        }
    }

    suspend fun saveByApi(input: UserInDto): User {
        val role = input.role.uppercase()

        val registration = when (role) {
            // клиент: users + пустая строка в clients
            "CLIENT" -> RegistrationRequest(
                username = input.login,
                password = input.password,
                email = input.email,
                role = role,
                isActive = input.isActive,
                client = ClientRegData()
            )

            // доктор: users + запись в doctors
            // profession обязательна на стороне fastapi,
            // поэтому, пока нет отдельного поля, кладем заглушку.
            // если потом появится конкретная специализация в dto — просто подставь её сюда.
            "DOCTOR" -> RegistrationRequest(
                username = input.login,
                password = input.password,
                email = input.email,
                role = role,
                isActive = input.isActive,
                doctor = DoctorRegData(
                    clinicId = input.clinicId?.toLong(),
                    profession = "doctor"
                )
            )

            // админ: users + запись в admins
            // admin.clinic_id обязателен; используем переданный clinicId,
            // а если его нет — базовую клинику (id = 1 из 002_seed.sql).
            "ADMIN" -> RegistrationRequest(
                username = input.login,
                password = input.password,
                email = input.email,
                role = role,
                isActive = input.isActive,
                admin = AdminRegData(
                    clinicId = input.clinicId ?.toLong(),
                )
            )

            // fallback — пусть будет как клиент
            else -> RegistrationRequest(
                username = input.login,
                password = input.password,
                email = input.email,
                role = role,
                isActive = input.isActive,
                client = ClientRegData()
            )
        }

        return doPost("/register", registration) { resp ->
            resp.body<UserOutDto>().let(::fromOutDto)
        }
    }

    suspend fun findByEmail(email: String): User? =
        doGet("/users/by-email/${urlEncode(email)}") { it.body<UserOutDto>().let(::fromOutDto) }

    suspend fun findByLogin(login: String): User? =
        doGet("/users/by-login/${urlEncode(login)}") { it.body<UserOutDto>().let(::fromOutDto) }

    suspend fun listUsers(role: String? = null): List<User> {
        val resp = client.get("$baseUrl/users") {
            role?.takeIf { it.isNotBlank() }?.let { parameter("role", it) }
        }
        if (resp.status != HttpStatusCode.OK) {
            throw RuntimeException("Unexpected response: ${resp.status} ${resp.bodyAsText()}")
        }
        val payload = resp.body<List<UserOutDto>>()
        return payload.map(::fromOutDto)
    }

    suspend fun login(loginOrEmail: String, password: String): ApiResponse {
        val apiResp: ApiResponse = doPost(
            path = "/auth/login",
            body = mapOf("login_or_email" to loginOrEmail, "password" to password),
            successCodes = setOf(HttpStatusCode.OK)
        ) { resp -> resp.body<ApiResponse>() }

        // Если Python сказал, что логин неуспешен — просто возвращаем его ответ наверх
        if (!apiResp.success) return apiResp

        // Здесь мы уже доверяем, что пароль ок и email подтверждён
        val user = if ('@' in loginOrEmail) {
            findByEmail(loginOrEmail)
        } else {
            findByLogin(loginOrEmail)
        }

        if (user == null) {
            // на всякий случай — юзер куда-то делся между запросами
            return ApiResponse(success = false, error = "User not found")
        }

        val subject = user.id.toString()
        val login   = user.login
        val role    = apiResp.role
        val email   = user.email

        val token = JwtService.issue(
            subject = subject,
            login   = login,
            role    = role,
            email   = email
        )

        return apiResp.copy(token = token)
    }

    suspend fun createUser(request: UserCreateRequest): Long {
        val userInDto = UserInDto(
            email = request.email,
            login = request.login,
            password = request.password,
            role = request.role,
            firstName = request.firstName,
            lastName = request.lastName,
            clinicId = request.clinicId?.toInt(),
            isActive = request.isActive
        )

        val user = saveByApi(userInDto)
        return user.id
    }

    // GET /users/{id}/profile — полный профиль
    suspend fun getUserProfile(userId: Long): UserResponseDto? =
        doGet("/users/$userId/profile") { resp ->
            resp.body<UserResponseDto>()
        }

    suspend fun findClientByUserId(userId: Long): ClientProfileDto? {
        val path = "/clients/by-user/$userId"
        val resp = client.get("$baseUrl$path")
        return when (resp.status) {
            HttpStatusCode.OK -> resp.body()
            HttpStatusCode.NotFound -> null
            else -> throw RuntimeException("Unexpected response: ${resp.status} ${resp.bodyAsText()}")
        }
    }

    suspend fun searchDoctors(filter: DoctorSearchFilter): List<DoctorSearchResult> {
        println("🎯 Starting searchDoctors with filter: $filter")
        val resp = client.get("$baseUrl/doctors/search") {
            filter.specializationIds?.forEach { parameter("specialization_ids", it) }
            filter.city?.let { parameter("city", it) }
            filter.region?.let { parameter("region", it) }
            filter.metro?.let { parameter("metro", it) }
            if (filter.onlineOnly) parameter("online_only", true)

            filter.minPrice?.let { parameter("min_price", it) }
            filter.maxPrice?.let { parameter("max_price", it) }
            filter.minRating?.let { parameter("min_rating", it) }
            filter.gender?.let { parameter("gender", it) }
            filter.minAge?.let { parameter("min_age", it) }
            filter.maxAge?.let { parameter("max_age", it) }
            filter.minExperience?.let { parameter("min_experience", it) }
            filter.maxExperience?.let { parameter("max_experience", it) }
            filter.date?.let { parameter("date", it) }

            parameter("limit", filter.limit)
            parameter("offset", filter.offset)
        }
        println("🔍 Response status: ${resp.status}")
        val responseBody = resp.bodyAsText()
        println("🔍 Raw response body: $responseBody")

        if (resp.status != HttpStatusCode.OK) {
            throw RuntimeException("Unexpected response: ${resp.status} ${resp.bodyAsText()}")
        }

        return resp.body()
    }

    // PATCH /users/{id}/profile — частичное обновление профиля
    suspend fun patchUserProfile(userId: Long, patch: UserProfilePatch): UserResponseDto {
        val patchMap = mutableMapOf<String, Any?>()

        patch.firstName?.let { patchMap["name"] = it }
        patch.lastName?.let { patchMap["surname"] = it }
        patch.patronymic?.let { patchMap["patronymic"] = it }
        patch.phoneNumber?.let { patchMap["phone_number"] = it }
        patch.clinicId?.let { patchMap["clinic_id"] = it }
        patch.dateOfBirth?.let { patchMap["date_of_birth"] = it }
        patch.avatar?.let { patchMap["avatar"] = it }
        patch.gender?.let { patchMap["gender"] = it }

        // Убираем null значения из map
        val cleanPatchMap = patchMap.filterValues { it != null } as Map<*, *>

        println("=== PATCH USER PROFILE ===")
        println("User ID: $userId")
        println("Clean patch map: $cleanPatchMap")

        if (cleanPatchMap.isEmpty()) {
            throw IllegalArgumentException("No fields to update")
        }

        return doPatch("/users/$userId/profile", cleanPatchMap) { resp ->
            resp.body<UserResponseDto>()
        }
    }

    // === clients ===
    suspend fun patchClientByUserId(userId: Long, patch: ClientPatch): ClientProfileDto {
        val patchMap = mutableMapOf<String, Any?>()

        patch.bloodType?.let { patchMap["blood_type"] = it }
        patch.height?.let { patchMap["height"] = it }
        patch.weight?.let { patchMap["weight"] = it }
        patch.emergencyContactName?.let { patchMap["emergency_contact_name"] = it }
        patch.emergencyContactNumber?.let { patchMap["emergency_contact_number"] = it }
        patch.address?.let { patchMap["address"] = it }
        patch.snils?.let { patchMap["snils"] = it }
        patch.passport?.let { patchMap["passport"] = it }
        patch.dmsOms?.let { patchMap["dms_oms"] = it }

        val cleanPatchMap = patchMap.filterValues { it != null } as Map<*, *>

        println("=== PATCH CLIENT PROFILE ===")
        println("User ID: $userId")
        println("Clean patch map: $cleanPatchMap")

        if (cleanPatchMap.isEmpty()) {
            throw IllegalArgumentException("No client fields to update")
        }

        return doPatch("/clients/by-user/$userId", cleanPatchMap) { it.body() }
    }

    // === doctors ===
    suspend fun patchDoctorByUserId(userId: Long, patch: DoctorPatch): DoctorProfileDto {
        val patchMap = mutableMapOf<String, Any?>()

        patch.clinicId?.let { patchMap["clinic_id"] = it }
        patch.profession?.let { patchMap["profession"] = it }
        patch.info?.let { patchMap["info"] = it }
        patch.experience?.let { patchMap["experience"] = it }
        patch.price?.let { patchMap["price"] = it }

        val cleanPatchMap = patchMap.filterValues { it != null } as Map<*, *>

        println("=== PATCH DOCTOR PROFILE ===")
        println("User ID: $userId")
        println("Clean patch map: $cleanPatchMap")

        if (cleanPatchMap.isEmpty()) {
            throw IllegalArgumentException("No doctor fields to update")
        }

        return doPatch("/doctors/by-user/$userId", cleanPatchMap) { it.body() }
    }

    // ===== ДЛЯ ЖАЛОБ ПАЦИЕНТА =====
    // POST /patients/{id}/complaints
    suspend fun createComplaint(patientId: Long, input: ComplaintIn): ComplaintOut =
        doPost("/patients/$patientId/complaints", input) { it.body() }

    // GET /patients/{id}/complaints?status=OPEN|IN_PROGRESS|CLOSED
    suspend fun listComplaints(patientId: Long, status: ComplaintStatus? = null): List<ComplaintOut> {
        val resp = client.get("$baseUrl/patients/$patientId/complaints") {
            status?.let { parameter("status", it.name) }
        }
        if (resp.status != HttpStatusCode.OK) {
            throw RuntimeException("Unexpected response: ${resp.status} ${resp.bodyAsText()}")
        }
        return resp.body()
    }

    // PATCH /complaints/{id}
    suspend fun patchComplaint(complaintId: Long, patch: ComplaintPatch): ComplaintOut =
        doPatch("/complaints/$complaintId", patch) { it.body() }

    // DELETE /complaints/{id}
    suspend fun deleteComplaint(complaintId: Long): Boolean =
        doDelete("/complaints/$complaintId")

    //===== ДЛЯ ЗАПИСЕЙ ВРАЧЕЙ ====
    suspend fun findDoctorByUserId(userId: Long): DoctorProfileDto? {
        val path = "/doctors/by-user/$userId"
        val resp = client.get("$baseUrl$path")
        println("🟢 GET $baseUrl$path -> ${resp.status}")
        return when (resp.status) {
            HttpStatusCode.OK -> resp.body()
            HttpStatusCode.NotFound -> null
            else -> throw RuntimeException("Unexpected response: ${resp.status} ${resp.bodyAsText()}")
        }
    }
    // POST /patients/{id}/notes
    suspend fun createNote(patientId: Long, input: NoteIn): NoteOut {
        val doctor = findDoctorByUserId(input.doctorId)
            ?: throw IllegalArgumentException("doctor not found for user_id=${input.doctorId}")

        val payload = NoteIn(
            doctorId = doctor.id,
            note = input.note,
            visibility = input.visibility
        )

        return doPost("/patients/$patientId/notes", payload) { resp ->
            val raw = resp.body<NoteOut>()
            // raw.doctorId = doctors.id, наружу возвращаем userId,
            // чтобы тесты и фронт жили в пространстве users.id
            raw.copy(doctorId = doctor.userId)
        }
    }

    // GET /patients/{id}/notes?include_internal=true|false
    suspend fun listNotes(patientId: Long, includeInternal: Boolean = true): List<NoteOut> {
        val resp = client.get("$baseUrl/patients/$patientId/notes") {
            parameter("include_internal", includeInternal)
        }
        if (resp.status != HttpStatusCode.OK) {
            throw RuntimeException("Unexpected response: ${resp.status} ${resp.bodyAsText()}")
        }
        return resp.body()
    }

    // PATCH /notes/{id}
    suspend fun patchNote(noteId: Long, patch: NotePatch): NoteOut =
        doPatch("/notes/$noteId", patch) { it.body() }

    // DELETE /notes/{id}
    suspend fun deleteNote(noteId: Long): Boolean =
        doDelete("/notes/$noteId")

    // ===== appointments & records =====

    suspend fun listAppointmentsForClient(clientId: Long): List<AppointmentDto> {
        val path = "/clients/$clientId/appointments"
        val resp = client.get("$baseUrl$path")
        if (resp.status != HttpStatusCode.OK) {
            throw RuntimeException("unexpected response: ${resp.status} ${resp.bodyAsText()}")
        }
        return resp.body()
    }

    suspend fun listMedicalRecordsForClient(clientId: Long): List<MedicalRecordDto> {
        val path = "/clients/$clientId/medical-records"
        val resp = client.get("$baseUrl$path")
        if (resp.status != HttpStatusCode.OK) {
            throw RuntimeException("unexpected response: ${resp.status} ${resp.bodyAsText()}")
        }
        return resp.body()
    }

    suspend fun listAppointmentsForDoctor(doctorId: Long): List<AppointmentDto> {
        val path = "/doctors/$doctorId/appointments"
        val resp = client.get("$baseUrl$path")
        if (resp.status != HttpStatusCode.OK) {
            throw RuntimeException("unexpected response: ${resp.status} ${resp.bodyAsText()}")
        }
        return resp.body()
    }

    suspend fun listPatientsForDoctor(doctorId: Long): List<DoctorPatientDto> {
        val path = "/doctors/$doctorId/patients"
        val resp = client.get("$baseUrl$path")
        if (resp.status != HttpStatusCode.OK) {
            throw RuntimeException("unexpected response: ${resp.status} ${resp.bodyAsText()}")
        }
        return resp.body()
    }

    // ===== Slots / appointments =====
    suspend fun createSlot(doctorId: Long, input: SlotCreateRequest): Slot {
        val payload = mapOf(
            "doctor_id" to doctorId,
            "start_time" to input.startTime,
            "end_time" to input.endTime
        )

        return doPost("/doctors/$doctorId/slots", payload) { resp ->
            fromSlotDto(resp.body())
        }
    }

    suspend fun listSlots(doctorId: Long, date: String?): List<Slot> {
        val query = date?.let { "?date=$it" } ?: ""
        return doGet("/doctors/$doctorId/slots$query") { resp ->
            resp.body<List<SlotOutDto>>().map(::fromSlotDto)
        } ?: emptyList()
    }

    suspend fun deleteSlot(doctorId: Long, slotId: Long): Boolean =
        doDelete("/doctors/$doctorId/slots/$slotId")

    suspend fun bookAppointment(input: AppointmentCreateRequest): Appointment =
        doPost("/appointments", input) { resp -> fromAppointmentDto(resp.body()) }

    suspend fun cancelAppointment(appointmentId: Long): Boolean =
        doPost(
            path = "/appointments/$appointmentId/cancel",
            body = null,
            successCodes = setOf(HttpStatusCode.NoContent, HttpStatusCode.OK)
        ) { true }
}