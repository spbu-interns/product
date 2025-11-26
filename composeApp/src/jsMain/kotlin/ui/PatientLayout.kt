package ui

import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h3
import io.kvision.html.nav
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.html.ul
import io.kvision.html.li

enum class PatientSection { OVERVIEW, APPOINTMENTS, MEDICAL_RECORDS, MY_RECORDS, EDIT_PROFILE }

fun Container.patientAccountLayout(
    active: PatientSection,
    mainContent: Container.() -> Unit
) {
    div(className = "account container") {
        div(className = "account grid") {
            div(className = "sidebar card") {
                patientSidebar(active)
            }

            div(className = "main column") {
                mainContent()
            }
        }
    }
}

fun Container.patientSidebar(active: PatientSection) {
    div(className = "avatar circle") { +"ИИ" }
    h3(Session.fullName ?: Session.email ?: "Пользователь", className = "account name")
    p("ID пациента: ${Session.userId}", className = "account id")

    nav {
        ul(className = "side menu") {
            sideItem("Обзор", "\uD83D\uDC64", active == PatientSection.OVERVIEW) {
                Navigator.showPatient()
            }

            sideItem("Приёмы", "\uD83D\uDCC5", active == PatientSection.APPOINTMENTS) {
                Navigator.showAppointments()
            }

            sideItem("Медкарта", "\uD83D\uDCC4", active == PatientSection.MEDICAL_RECORDS) {
                Navigator.showStub("Раздел медицинской карты находится в разработке")
            }

            sideItem("Мои записи", "\uD83D\uDCDD", active == PatientSection.MY_RECORDS) {
                Navigator.showMyRecords()
            }
            sideItem("Редактировать профиль", "\uD83D\uDC64", active == PatientSection.EDIT_PROFILE) {
                Navigator.showPatientProfileEdit()
            }
        }
    }

    div(className = "side button")
    button("Найти врача", className = "btn-primary-lg").onClick {
        Navigator.showFind()
    }
}

private fun Container.sideItem(label: String, icon: String, isActive: Boolean, onClick: () -> Unit) {
    val className = "side_item" + if (isActive) " is-active" else ""
    li(className = className) {
        span(label)
        span(icon, className = "side icon")
        onClick { onClick() }
    }
}