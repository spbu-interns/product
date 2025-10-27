package ui.components

import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.html.*

enum class SidebarTab { OVERVIEW, APPOINTMENTS, RECORDS, MYRECORDS }

fun Container.patientSidebar(
    patientId: Long,
    active: SidebarTab,
    onOverview: () -> Unit,
    onAppointments: () -> Unit,
    onMedicalRecords: () -> Unit,
    onMyRecords: () -> Unit,
    onFindDoctor: () -> Unit
) {
    div(className = "sidebar card") {
        div(className = "avatar circle") { +"NS" }
        h3("Name Surname", className = "account name")
        p("Patient ID: $patientId", className = "account id")

        nav {
            ul(className = "side menu") {
                li(className = "side_item" + if (active == SidebarTab.OVERVIEW) " is-active" else "") {
                    span("Overview"); span("\uD83D\uDC64", className = "side icon")
                    onClick { onOverview() }
                }
                li(className = "side_item" + if (active == SidebarTab.APPOINTMENTS) " is-active" else "") {
                    span("Appointments"); span("\uD83D\uDCC5", className = "side icon")
                    onClick { onAppointments() }
                }
                li(className = "side_item" + if (active == SidebarTab.RECORDS) " is-active" else "") {
                    span("Medical Records"); span("\uD83D\uDCC4", className = "side icon")
                    onClick { onMedicalRecords() }
                }
                li(className = "side_item" + if (active == SidebarTab.MYRECORDS) " is-active" else "") {
                    span("My Records"); span("\uD83D\uDCDD", className = "side icon")
                    onClick { onMyRecords() }
                }
            }
        }

        div(className = "side button")
        button("Find New Doctor", className = "btn-primary-lg").onClick { onFindDoctor() }
    }
}
