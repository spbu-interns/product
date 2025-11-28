package ui.components

import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.html.*
import ui.Session

enum class SidebarTab { OVERVIEW, APPOINTMENTS, RECORDS, MYRECORDS, PROFILE }

fun Container.patientSidebar(
    patientId: Long,
    active: SidebarTab,
    onOverview: () -> Unit,
    onAppointments: () -> Unit,
    onMedicalRecords: () -> Unit,
    onMyRecords: () -> Unit,
    onFindDoctor: () -> Unit,
    onProfile: (() -> Unit)? = null
) {
    val displayName = Session.fullName() ?: Session.email ?: "Пользователь"

    div(className = "sidebar card") {
        div(className = "avatar circle") { +"ИИ" }
        h3(displayName, className = "account name")

        nav {
            ul(className = "side menu") {
                li(className = "side_item" + if (active == SidebarTab.OVERVIEW) " is-active" else "") {
                    span("Обзор"); span("\uD83D\uDC64", className = "side icon")
                    onClick { onOverview() }
                }
                li(className = "side_item" + if (active == SidebarTab.APPOINTMENTS) " is-active" else "") {
                    span("Приёмы"); span("\uD83D\uDCC5", className = "side icon")
                    onClick { onAppointments() }
                }
                li(className = "side_item" + if (active == SidebarTab.RECORDS) " is-active" else "") {
                    span("Медкарта"); span("\uD83D\uDCC4", className = "side icon")
                    onClick { onMedicalRecords() }
                }
                li(className = "side_item" + if (active == SidebarTab.MYRECORDS) " is-active" else "") {
                    span("Мои записи"); span("\uD83D\uDCDD", className = "side icon")
                    onClick { onMyRecords() }
                }
                onProfile?.let {
                    li(className = "side_item" + if (active == SidebarTab.PROFILE) " is-active" else "") {
                        span("Профиль"); span("\uD83D\uDC64", className = "side icon")
                        onClick { it() }
                    }
                }
            }
        }

        div(className = "side button")
        button("Найти врача", className = "btn-primary-lg").onClick { onFindDoctor() }
    }
}