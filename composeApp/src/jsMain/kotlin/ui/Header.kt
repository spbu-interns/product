package ui

import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.image
import io.kvision.html.link
import io.kvision.html.span
import io.kvision.html.nav
import ui.components.doctorActiveAppointmentBanner

enum class HeaderMode { PUBLIC, PATIENT, DOCTOR }
enum class NavTab { NONE, HOME, FIND, PROFILE }

fun Container.headerBar(
    mode: HeaderMode = HeaderMode.PUBLIC,
    active: NavTab = NavTab.NONE,
    onLogout: (() -> Unit)? = null
) {
    nav(className = "topnav") {
        div(className = "container topnav_content") {
            div(className = "brand") {
                setAttribute("style", "cursor: pointer;")
                onClick { Navigator.showHome() }

                image(src = "/images/logo.jpg") {
                    addCssClass("brand_logo")
                }
                span("INTERNS", className = "brand_text")
            }

            div(className = "topnav_links") {
                val homeClass = "topnav_link" + if (active == NavTab.HOME) " is-active" else ""
                val findClass = "topnav_link" + if (active == NavTab.FIND) " is-active" else ""

                link("Главная", "#", className = homeClass).onClick {
                    it.preventDefault(); Navigator.showHome()
                }
                val findLabel = if (mode == HeaderMode.DOCTOR) "Найти пациента" else "Найти врача"
                link(findLabel, "#", className = findClass).onClick {
                    it.preventDefault();
                    if (mode == HeaderMode.DOCTOR) Navigator.showFindPatient() else Navigator.showFind()
                }
            }

            when (mode) {
                HeaderMode.PUBLIC -> {
                    div(className = "topnav_auth") {
                        button("Вход", className = "btn btn-primary").onClick {
                            Navigator.showLogin()
                        }
                        button("Регистрация", className = "btn-ghost-sm").onClick {
                            Navigator.showRegister()
                        }
                    }
                }

                HeaderMode.PATIENT -> {
                    div(className = "topnav_auth") {
                        val profileClass = "btn-ghost-sm profile-nav" + if (active == NavTab.PROFILE) " is-active" else ""
                        button("Личный кабинет", className = profileClass).onClick {
                            Navigator.showPatient()
                        }
                    }
                }

                HeaderMode.DOCTOR -> {
                    div(className = "topnav_auth") {
                        val profileClass = "btn-ghost-sm profile-nav" + if (active == NavTab.PROFILE) " is-active" else ""
                        button("Личный кабинет", className = profileClass).onClick {
                            Navigator.showDoctor()
                        }
                    }
                }
            }
        }
    }

    if (mode == HeaderMode.DOCTOR) {
        doctorActiveAppointmentBanner()
    }
}