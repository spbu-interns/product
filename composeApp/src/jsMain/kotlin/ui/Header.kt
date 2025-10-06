package ui

import io.kvision.core.Container
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.image
import io.kvision.html.link
import io.kvision.html.nav

enum class HeaderMode { PUBLIC, PATIENT }

fun Container.headerBar(
    mode: HeaderMode = HeaderMode.PUBLIC,
    onLogout: (() -> Unit)? = null
) {
    nav(className = "topnav") {
        div(className = "container topnav_content") {
            div(className = "brand") {
                image(src = "images/logo.jpg") {
                    addCssClass("brand_logo")
                }
                link(label = "INTERNS", url = "#", className = "brand_text").onClick { it.preventDefault(); Navigator.showHome() }
            }

            div(className = "topnav_links") {
                link("Home", url = "#", className = "topnav_link is-active").onClick { Navigator.showHome() }
                link("Find Doctors", url = "#", className = "topnav_link").onClick { it.preventDefault(); Navigator.showFind() }
            }

            when (mode) {
                HeaderMode.PUBLIC -> {
                    div(className = "topnav_auth") {
                        link("Patient Login", url = "#", className = "link-ghost").onClick { it.preventDefault(); Navigator.showLogin() }
                        link("Doctor Login", url = "#", className = "link-ghost").onClick { it.preventDefault(); Navigator.showLogin() }
                        button("Admin", className = "btn-primary-sm").onClick { /*админ*/}
                    }
                }

                HeaderMode.PATIENT -> {
                    div(className = "topnav_auth") {
                        button("Account", className = "btn-ghost-sm").onClick {
                            Navigator.showPatient()
                        }
                        button("Logout", className = "btn-logout-sm").onClick {
                            onLogout?.invoke() ?: run { Navigator.showHome() }
                        }
                    }
                }
            }
        }
    }
}