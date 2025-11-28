package ui.components

import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.html.*
import ui.PatientSection
import ui.Session

fun Container.patientSidebar(
    patientId: Long?,
    active: PatientSection,
    onOverview: () -> Unit,
    onAppointments: () -> Unit,
    onMedicalRecords: () -> Unit,
    onMyRecords: () -> Unit,
    onFindDoctor: () -> Unit,
    onProfile: (() -> Unit)
) {
    val displayName = Session.fullName() ?: Session.email ?: "Пользователь"

    div(className = "sidebar card") {
        div(className = "avatar circle") { +(displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "П") }
        h3(displayName, className = "account name")

        nav {
            ul(className = "side menu") {
                li(className = "side_item" + if (active == PatientSection.OVERVIEW) " is-active" else "") {
                    span("Обзор"); span("\uD83D\uDC64", className = "side icon")
                    onClick { onOverview() }
                }
                li(className = "side_item" + if (active == PatientSection.APPOINTMENTS) " is-active" else "") {
                    span("Приёмы"); span("\uD83D\uDCC5", className = "side icon")
                    onClick { onAppointments() }
                }
                li(className = "side_item" + if (active == PatientSection.MEDICAL_RECORDS) " is-active" else "") {
                    span("Медкарта"); span("\uD83D\uDCC4", className = "side icon")
                    onClick { onMedicalRecords() }
                }
                li(className = "side_item" + if (active == PatientSection.MY_RECORDS) " is-active" else "") {
                    span("Мои записи"); span("\uD83D\uDCDD", className = "side icon")
                    onClick { onMyRecords() }
                }
                li(className = "side_item" + if (active == PatientSection.EDIT_PROFILE) " is-active" else "") {
                    span("Мой профиль"); span("\uD83D\uDC64", className = "side icon")
                    onClick { onProfile() }
                }
            }
        }

        div(className = "side button")
        button("Найти врача", className = "btn-primary-lg").onClick { onFindDoctor() }
    }
}