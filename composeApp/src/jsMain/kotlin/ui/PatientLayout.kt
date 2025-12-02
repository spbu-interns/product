package ui

import api.ApiConfig
import io.kvision.core.Container
import io.kvision.html.div
import ui.components.patientSidebar

enum class PatientSection { OVERVIEW, APPOINTMENTS, MEDICAL_RECORDS, MY_RECORDS, EDIT_PROFILE }

fun Container.patientAccountLayout(
    active: PatientSection,
    onLogout: () -> Unit = {
        ApiConfig.clearToken(); Session.clear(); Navigator.showHome()
    },
    mainContent: Container.() -> Unit
) {
    div(className = "account container") {
        div(className = "account grid") {
            patientSidebar(
                patientId = Session.userId,
                active = active,
                onOverview = { Navigator.showPatient() },
                onAppointments = { Navigator.showAppointments() },
                onMedicalRecords = { Navigator.showPatientMedicalRecords() },
                onMyRecords = { Navigator.showMyRecords() },
                onFindDoctor = { Navigator.showFind() },
                onProfile = { Navigator.showPatientProfileEdit() },
                onLogout = onLogout
            )

            div(className = "main column") {
                mainContent()
            }
        }
    }
}