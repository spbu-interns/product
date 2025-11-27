package ui

import api.ApiConfig
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.form.text.text
import io.kvision.html.*
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel
import org.interns.project.dto.UserResponseDto

fun Container.homeScreen() {
    headerBar(
        mode = if (Session.isLoggedIn) HeaderMode.PATIENT else HeaderMode.PUBLIC,
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
                text {
                    type = InputType.SEARCH
                    placeholder = "Найдите врача по специальности, местоположению или рейтингу"
                    addCssClass("searchbar_input")
                }
                button("Найти врача", className = "searchbar_button").onClick {
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

    fun fullName(): String? = listOfNotNull(firstName, lastName)
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
        isActive: Boolean = true
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
        this.isLoggedIn = true
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
        this.accountType = userResponse.role
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
    }
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