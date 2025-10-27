package ui

import i18n.Localization
import i18n.t
import io.kvision.core.Container
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.image
import io.kvision.html.link
import io.kvision.html.nav

enum class HeaderMode { PUBLIC, PATIENT, DOCTOR }
enum class NavTab { NONE, HOME, FIND }

fun Container.headerBar(
    mode: HeaderMode = HeaderMode.PUBLIC,
    active: NavTab = NavTab.NONE,
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
                val homeClass = "topnav_link" + if (active == NavTab.HOME) " is-active" else ""
                val findClass = "topnav_link" + if (active == NavTab.FIND) " is-active" else ""

                link(t("header.home"), "#", className = homeClass).onClick {
                    it.preventDefault(); Navigator.showHome()
                }
                link(t("header.find"), "#", className = findClass).onClick {
                    it.preventDefault(); Navigator.showFind()
                }
            }

            div(className = "topnav_right") {
                button(t("header.languageToggle"), className = "btn-ghost-sm language-toggle").onClick {
                    Localization.toggleLanguage()
                }
            }

            when (mode) {
                HeaderMode.PUBLIC -> {
                    div(className = "topnav_auth") {
                        button(t("header.login"), className = "btn btn-primary").onClick {
                            Navigator.showLogin()
                        }
                        button(t("header.register"), className = "btn-ghost-sm").onClick {
                            Navigator.showRegister()
                        }
                    }
                }

                HeaderMode.PATIENT -> {
                    div(className = "topnav_auth") {
                        button(t("header.dashboard"), className = "btn-ghost-sm").onClick {
                            Navigator.showPatient()
                        }
                        button(t("header.logout"), className = "btn-logout-sm").onClick {
                            onLogout?.invoke() ?: run { Navigator.showHome() }
                        }
                    }
                }

                HeaderMode.DOCTOR -> {
                    div(className = "topnav_auth") {
                        button(t("header.dashboard"), className = "btn-ghost-sm").onClick {
                            Navigator.showDoctor()
                        }
                        button(t("header.logout"), className = "btn-logout-sm").onClick {
                            onLogout?.invoke() ?: run { Navigator.showHome() }
                        }
                    }
                }
            }
        }
    }
}