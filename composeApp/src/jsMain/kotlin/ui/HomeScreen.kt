package ui

import api.ApiConfig
import api.PatientApiClient
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.form.text.text
import io.kvision.html.*
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.interns.project.dto.UserResponseDto

fun Container.homeScreen() {
    headerBar(
        mode = when {
            Session.accountType.equals("DOCTOR", ignoreCase = true) -> HeaderMode.DOCTOR
            Session.isLoggedIn -> HeaderMode.PATIENT
            else -> HeaderMode.PUBLIC
        },
        active = NavTab.HOME,
        onLogout = {
            ApiConfig.clearToken()
            Session.clear()
            Navigator.showHome()
        }
    )

    div(className = "hero") {
        div(className = "hero_content container") {
            h1("Найдите подходящего врача для вас", className = "hero_title")
            p("Свяжитесь с квалифицированными медицинскими специалистами. Ищите по специальности, местоположению или рейтингу, чтобы найти идеального врача для ваших потребностей.", className = "hero_subtitle")

            div(className = "searchbar") {
                div(className = "searchbar_icon") {
                    +"\uD83D\uDD0D"
                }
                val searchField = text {
                    type = InputType.SEARCH
                    placeholder = "Найдите врача по специальности, местоположению или рейтингу"
                    addCssClass("searchbar_input")
                }
                button("Найти врача", className = "searchbar_button").onClick {
                    Session.pendingDoctorSearchQuery = searchField.value
                    Navigator.showFind()
                }
            }
        }
    }

    div(className = "container") {
        h2("Популярные специальности", className = "section_title")
        p("Просмотрите врачей по медицинским специальностям", className = "section_subtitle")

        div(className = "specialties_grid") {
            specialtyCard(
                title = "Кардиология",
                subtitle = "Забота о сердце и сердечно-сосудистой системе",
                icon = "❤",
                imagePath = "/images/cardiology.jpg",
                onSelect = { Navigator.showFind() }
            )
            specialtyCard(
                title = "Педиатрия",
                subtitle = "Здоровье и развитие детей",
                icon = "👶",
                imagePath = "/images/pediatrics.jpg",
                onSelect = { Navigator.showFind() }
            )
            specialtyCard(
                title = "Неврология",
                subtitle = "Забота о мозге и нервной системе",
                icon = "🧠",
                imagePath = "/images/neurology.jpg",
                onSelect = { Navigator.showFind() }
            )
            specialtyCard(
                title = "Офтальмология",
                subtitle = "Забота о глазах и зрении",
                icon = "👁️",
                imagePath = "/images/ophthalmology.jpg",
                onSelect = { Navigator.showFind() }
            )
            specialtyCard(
                title = "Ортопедия",
                subtitle = "Забота о костях и суставах",
                icon = "🦴",
                imagePath = "/images/orthopedics.jpg",
                onSelect = { Navigator.showFind() }
            )
            specialtyCard(
                title = "Общая терапия",
                subtitle = "Первичная медицинская помощь",
                icon = "🩺",
                imagePath = "/images/general.jpg",
                onSelect = { Navigator.showFind() }
            )
        }
    }

    footer {
        addCssClass("footer")
        span("© 2025 Interns Health")
    }
}

object Session {
    // ---- Auth ----
    var isLoggedIn: Boolean = false
    var token: String? = null
    var userId: Long? = null
    var email: String? = null
    var accountType: String? = null  // DOCTOR / PATIENT / ADMIN

    // ---- Profile ----
    var firstName: String? = null
    var lastName: String? = null
    var patronymic: String? = null
    var phoneNumber: String? = null
    var avatar: String? = null
    var gender: String? = null        // M/F
    var dateOfBirth: String? = null   // YYYY-MM-DD
    var isActive: Boolean = true
    var hasNoPatronymic: Boolean = false
    var pendingDoctorSearchQuery: String? = null
    var pendingRegistration: PendingRegistration? = null

    data class PendingRegistration(
        val email: String,
        val password: String,
        val accountType: String
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun fullName(): String? = listOfNotNull(lastName, firstName, patronymic)
        .joinToString(" ")
        .takeIf { it.isNotBlank()}

    fun setSession(
        token: String?,
        userId: Long?,
        email: String?,
        accountType: String?,
        firstName: String? = null,
        lastName: String? = null,
        patronymic: String? = null,
        phoneNumber: String? = null,
        avatar: String? = null,
        gender: String? = null,
        dateOfBirth: String? = null,
        isActive: Boolean = true,
        hasNoPatronymic: Boolean = false
    ) {
        this.token = token
        this.userId = userId
        this.email = email
        this.accountType = accountType?.uppercase()
        this.firstName = firstName
        this.lastName = lastName
        this.patronymic = patronymic
        this.phoneNumber = phoneNumber
        this.avatar = avatar
        this.gender = gender
        this.dateOfBirth = dateOfBirth
        this.isActive = isActive
        this.hasNoPatronymic = hasNoPatronymic
        this.isLoggedIn = true
        this.token?.let { ApiConfig.setToken(it) }
        saveToStorage()
    }

    fun updateFrom(userResponse: UserResponseDto) {
        this.firstName = userResponse.name
        this.lastName = userResponse.surname
        this.patronymic = userResponse.patronymic
        this.phoneNumber = userResponse.phoneNumber
        this.avatar = userResponse.avatar
        this.gender = userResponse.gender
        this.dateOfBirth = userResponse.dateOfBirth
        this.isActive = userResponse.isActive
        this.email = userResponse.email
        this.accountType = userResponse.role.uppercase()
        this.hasNoPatronymic = userResponse.patronymic.isNullOrBlank()
        saveToStorage()
    }

    fun clear() {
        isLoggedIn = false
        token = null
        userId = null
        email = null
        accountType = null

        firstName = null
        lastName = null
        patronymic = null
        phoneNumber = null
        avatar = null
        gender = null
        dateOfBirth = null
        isActive = true
        hasNoPatronymic = false
        pendingDoctorSearchQuery = null
        pendingRegistration = null
        ApiConfig.clearToken()
        ApiConfig.clearSessionData()
    }

    fun ensureTokenFromLink(tokenFromLink: String?) {
        val tokenValue = tokenFromLink ?: return
        this.token = tokenValue
        this.isLoggedIn = true
        ApiConfig.setToken(tokenValue)
        decodeJwtClaims(tokenValue)
        saveToStorage()
    }

    fun restoreFromStorage() {
        val storedSession = ApiConfig.getSessionData()
        val storedToken = ApiConfig.getToken()

        if (storedSession != null) {
            runCatching {
                json.decodeFromString(SessionSnapshot.serializer(), storedSession)
            }.getOrNull()?.let { snapshot ->
                applySnapshot(snapshot)
            }
        } else if (storedToken != null) {
            this.token = storedToken
            this.isLoggedIn = true
            decodeJwtClaims(storedToken)
        }

        if (token == null && storedToken != null) {
            token = storedToken
        }
    }

    suspend fun hydrateFromBackend(): Boolean {
        val id = userId ?: return false
        val api = PatientApiClient()
        val result = api.getFullUserProfile(id)
        result.onSuccess { profile ->
            profile?.user?.let { updateFrom(it) }
        }
        return result.isSuccess
    }

    fun requiresProfileCompletion(): Boolean {
        val firstMissing = firstName.isNullOrBlank()
        val lastMissing = lastName.isNullOrBlank()
        val birthMissing = dateOfBirth.isNullOrBlank()
        val patronymicMissing = patronymic.isNullOrBlank() && !hasNoPatronymic
        return firstMissing || lastMissing || birthMissing || patronymicMissing
    }

    private fun applySnapshot(snapshot: SessionSnapshot) {
        token = snapshot.token
        userId = snapshot.userId
        email = snapshot.email
        accountType = snapshot.accountType
        firstName = snapshot.firstName
        lastName = snapshot.lastName
        patronymic = snapshot.patronymic
        phoneNumber = snapshot.phoneNumber
        avatar = snapshot.avatar
        gender = snapshot.gender
        dateOfBirth = snapshot.dateOfBirth
        isActive = snapshot.isActive
        hasNoPatronymic = snapshot.hasNoPatronymic
        isLoggedIn = token != null
        snapshot.token?.let { ApiConfig.setToken(it) }
    }

    private fun decodeJwtClaims(tokenValue: String) {
        runCatching {
            val payload = tokenValue.split(".").getOrNull(1) ?: return
            val decoded = js("JSON.parse(atob(payload))")
            val claims = decoded.asDynamic()
            val userIdRaw = claims.userId
            if (userIdRaw != undefined) userId = userIdRaw.toString().toLongOrNull()
            val roleRaw = claims.role
            if (roleRaw != undefined) accountType = roleRaw.toString().uppercase()
            val emailRaw = claims.email
            if (emailRaw != undefined) email = emailRaw.toString()
            val loginRaw = claims.login
            if (loginRaw != undefined && firstName.isNullOrBlank()) firstName = loginRaw.toString()
        }.onFailure { /* ignore decode issues */ }
    }

    private fun saveToStorage() {
        val snapshot = SessionSnapshot(
            token = token,
            userId = userId,
            email = email,
            accountType = accountType,
            firstName = firstName,
            lastName = lastName,
            patronymic = patronymic,
            phoneNumber = phoneNumber,
            avatar = avatar,
            gender = gender,
            dateOfBirth = dateOfBirth,
            isActive = isActive,
            hasNoPatronymic = hasNoPatronymic
        )

        val serialized = json.encodeToString(snapshot)
        ApiConfig.setSessionData(serialized)
        token?.let { ApiConfig.setToken(it) }
    }

    @Serializable
    private data class SessionSnapshot(
        val token: String?,
        val userId: Long?,
        val email: String?,
        val accountType: String?,
        val firstName: String?,
        val lastName: String?,
        val patronymic: String?,
        val phoneNumber: String?,
        val avatar: String?,
        val gender: String?,
        val dateOfBirth: String?,
        val isActive: Boolean,
        val hasNoPatronymic: Boolean
    )
}

private fun Container.specialtyCard(
    title: String,
    subtitle: String,
    icon: String,
    imagePath: String,
    onSelect: (() -> Unit)? = null
) {
    div(className = "specialty").apply {
        setAttribute(
            "style",
            "background-image: url('$imagePath'); background-size: cover; background-position: center;"
        )

        div(className = "specialty_content") {
            vPanel {
                hPanel (className = "specialty_row") {
                    div(className = "specialty_icon") { +icon }
                    h4(title, className = "specialty_title")
                }
                p(subtitle, className = "specialty_subtitle")
            }
        }

        onClick {
            onSelect?.invoke()
        }
    }
}