package ui

import i18n.t
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
        active = NavTab.HOME,
        onLogout = {
            Session.isLoggedIn = false
            Navigator.showHome()
        }
    )

    div(className = "hero") {
        div(className = "hero_content container") {
            h1(t("home.hero.title"), className = "hero_title")
            p(t("home.hero.subtitle"), className = "hero_subtitle")

            div(className = "searchbar") {
                div(className = "searchbar_icon") {
                    +"\uD83D\uDD0D"
                }
                val search = text {
                    type = InputType.SEARCH
                    placeholder = t("home.search.placeholder")
                    addCssClass("searchbar_input")
                }
                button(t("home.search.button"), className = "searchbar_button").onClick {
                    console.log("Search: ${search.value}")
                }
            }
        }
    }

    div(className = "container") {
        h2(t("home.section.featured"), className = "section_title")
        p(t("home.section.browse"), className = "section_subtitle")

        div(className = "specialties_grid") {
            specialtyCard(
                title = t("home.specialties.cardiology.title"),
                subtitle = t("home.specialties.cardiology.subtitle"),
                icon = "❤",
                imagePath = "images/cardiology.jpg"
            )
            specialtyCard(
                title = t("home.specialties.pediatrics.title"),
                subtitle = t("home.specialties.pediatrics.subtitle"),
                icon = "👶",
                imagePath = "images/pediatrics.jpg"
            )
            specialtyCard(
                title = t("home.specialties.neurology.title"),
                subtitle = t("home.specialties.neurology.subtitle"),
                icon = "🧠",
                imagePath = "images/neurology.jpg"
            )
            specialtyCard(
                title = t("home.specialties.ophthalmology.title"),
                subtitle = t("home.specialties.ophthalmology.subtitle"),
                icon = "👁️",
                imagePath = "images/ophthalmology.jpg"
            )
            specialtyCard(
                title = t("home.specialties.orthopedics.title"),
                subtitle = t("home.specialties.orthopedics.subtitle"),
                icon = "🦴",
                imagePath = "images/orthopedics.jpg"
            )
            specialtyCard(
                title = t("home.specialties.general.title"),
                subtitle = t("home.specialties.general.subtitle"),
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