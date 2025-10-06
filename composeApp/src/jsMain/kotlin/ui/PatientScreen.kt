package ui

import io.kvision.core.Container
import io.kvision.panel.vPanel

fun Container.patientScreen(onLogout: () -> Unit = { Navigator.showHome() }) = vPanel(spacing = 12) {
    headerBar(
        mode = HeaderMode.PATIENT,
        onLogout = {
            Session.isLoggedIn = false
            Navigator.showHome()
        }
    )
}