package ui

import api.ApiConfig
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.form.text.text
import io.kvision.html.*
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel

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
            h1("Find the Right Doctor for You", className = "hero_title")
            p("Connect with qualified healthcare professionals. Search by specialty, location, or ratings to find the perfect doctor for your needs.", className = "hero_subtitle")

            div(className = "searchbar") {
                div(className = "searchbar_icon") {
                    +"\uD83D\uDD0D"
                }
                text {
                    type = InputType.SEARCH
                    placeholder = "Find a doctor by specialty, location, or rating"
                    addCssClass("searchbar_input")
                }
                button("Find Doctor", className = "searchbar_button").onClick {
                    Navigator.showFind()
                }
            }
        }
    }

    div(className = "container") {
        h2("Featured Specialties", className = "section_title")
        p("Browse doctors by medical specialty", className = "section_subtitle")

        div(className = "specialties_grid") {
            specialtyCard(
                title = "Cardiology",
                subtitle = "Heart and cardiovascular care",
                icon = "❤",
                imagePath = "images/cardiology.jpg",
                onSelect = { Navigator.showFind() }
            )
            specialtyCard(
                title = "Pediatrics",
                subtitle = "Children’s health and development",
                icon = "👶",
                imagePath = "images/pediatrics.jpg",
                onSelect = { Navigator.showFind() }
            )
            specialtyCard(
                title = "Neurology",
                subtitle = "Brain and nervous system care",
                icon = "🧠",
                imagePath = "images/neurology.jpg",
                onSelect = { Navigator.showFind() }
            )
            specialtyCard(
                title = "Ophthalmology",
                subtitle = "Eye and vision care",
                icon = "👁️",
                imagePath = "images/ophthalmology.jpg",
                onSelect = { Navigator.showFind() }
            )
            specialtyCard(
                title = "Orthopedics",
                subtitle = "Bone and joint care",
                icon = "🦴",
                imagePath = "images/orthopedics.jpg",
                onSelect = { Navigator.showFind() }
            )
            specialtyCard(
                title = "General Medicine",
                subtitle = "Primary healthcare services",
                icon = "🩺",
                imagePath = "images/general.jpg",
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
    var isLoggedIn: Boolean = false
    var token: String? = null
    var userId: Long? = null
    var email: String? = null
    var accountType: String? = null

    var firstName: String? = null
    var lastName: String? = null

    val fullName: String?
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .takeIf { it.isNotBlank() }

    fun setSession(
        token: String?,
        userId: Long?,
        email: String?,
        accountType: String?,
        firstName: String? = null,
        lastName: String? = null,
    ) {
        this.token = token
        this.userId = userId
        this.email = email
        this.accountType = accountType?.uppercase()
        this.firstName = firstName
        this.lastName = lastName
        this.isLoggedIn = true
    }

    fun clear() {
        isLoggedIn = false
        token = null
        userId = null
        email = null
        accountType = null
        firstName = null
        lastName = null
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