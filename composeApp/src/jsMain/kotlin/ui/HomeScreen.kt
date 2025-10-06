package ui

import io.kvision.core.AlignItems
import io.kvision.core.Container
import io.kvision.core.style
import io.kvision.form.text.text
import io.kvision.html.*
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel

fun Container.homeScreen() {
    headerBar(
        mode = if (Session.isLoggedIn) HeaderMode.PATIENT else HeaderMode.PUBLIC,
        onLogout = {
            Session.isLoggedIn = false
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
                val search = text {
                    type = InputType.SEARCH
                    placeholder = "Find a doctor by specialty, location, or rating"
                    addCssClass("searchbar_input")
                }
                button("Find Doctor", className = "searchbar_button").onClick {
                    console.log("Search: ${search.value}")
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
                imagePath = "images/cardiology.jpg"
            )
            specialtyCard(
                title = "Pediatrics",
                subtitle = "Children’s health and development",
                icon = "👶",
                imagePath = "images/pediatrics.jpg"
            )
            specialtyCard(
                title = "Neurology",
                subtitle = "Brain and nervous system care",
                icon = "🧠",
                imagePath = "images/neurology.jpg"
            )
            specialtyCard(
                title = "Ophthalmology",
                subtitle = "Eye and vision care",
                icon = "👁️",
                imagePath = "images/ophthalmology.jpg"
            )
            specialtyCard(
                title = "Orthopedics",
                subtitle = "Bone and joint care",
                icon = "🦴",
                imagePath = "images/orthopedics.jpg"
            )
            specialtyCard(
                title = "General Medicine",
                subtitle = "Primary healthcare services",
                icon = "🩺",
                imagePath = "images/general.jpg"
            )
        }
    }

    footer {
        addCssClass("footer")
        span("© 2025 Interns Health")
    }
}

object Session { var isLoggedIn = false }

private fun Container.specialtyCard(
    title: String,
    subtitle: String,
    icon: String,
    imagePath: String
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
    }
}