package ui

import io.kvision.core.Container
import io.kvision.html.div
import ui.components.patientSidebar

enum class PatientSection { OVERVIEW, APPOINTMENTS, MEDICAL_RECORDS, MY_RECORDS, EDIT_PROFILE }

fun Container.patientAccountLayout(
    active: PatientSection,
    mainContent: Container.() -> Unit
) {
    div(className = "account container") {
        div(className = "account grid") {
            patientSidebar(
                patientId = Session.userId,
                active = active,
                onOverview = { Navigator.showPatient() },
                onAppointments = { Navigator.showAppointments() },
                onMedicalRecords = { Navigator.showStub("Раздел медицинской карты находится в разработке") },
                onMyRecords = { Navigator.showMyRecords() },
                onFindDoctor = { Navigator.showFind() },
                onProfile = { Navigator.showPatientProfileEdit() }
            )

            div(className = "main column") {
                mainContent()
            }
        }
    }
}