package ui

import api.ApiConfig
import io.kvision.core.Container
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.h3
import io.kvision.html.h4
import io.kvision.html.span
import io.kvision.panel.vPanel

fun Container.patientScreen(onLogout: () -> Unit = { Navigator.showHome() }) = vPanel(spacing = 12) {
    headerBar(
        mode = HeaderMode.PATIENT,
        active = NavTab.NONE,
        onLogout = {
            ApiConfig.clearToken()
            Session.clear()
            Navigator.showHome()
        }
    )

    patientAccountLayout(active = PatientSection.OVERVIEW) {
        h1("Account Overview", className = "account title")

        div(className = "statistics grid") {
            statisticsCard("X", "Upcoming", "\uD83D\uDCC5")
            statisticsCard("Y", "Records", "\uD83D\uDCC4")
            statisticsCard("Z", "Doctors", "\uD83D\uDC64")
        }

        div(className = "card block appointment-block") {
            h4("Next Appointment", className = "block title")
            div(className = "appointment card") {
                div(className = "appointment row") {
                    div(className = "appointment avatar") { +"👤" }

                    div(className = "appointment info") {
                        span("Dr. X", className = "appointment doctor")
                        span("Cardiology", className = "appointment appointment-specialty")
                        div(className = "appointment meta") {
                            span("📅 Date")
                            span("⏰ Time")
                        }
                    }

                    div(className = "appointment actions") {
                        span("confirmed", className = "status success")
                    }
                }
                button("Find New Doctor", className = "btn-primary-lg").onClick {
                    Navigator.showFind()
                }
            }
        }

        h4("Recent Medical Records", className = "block title")

        div(className = "card block") {
            div(className = "records list") {
                recordItem("Test 1", "Dr. X • Date", "Status 1")
                recordItem("Test 2", "Dr. Y • Date", "Status 2")
                recordItem("Test 3", "Dr. Z • Date", "Status 3")
            }
        }
    }
}

private fun Container.statisticsCard(value: String, label: String, icon: String) {
    div(className = "statistics card") {
        span(icon, className = "statistics icon")
        h3(value, className = "statistics value")
        span(label, className = "statistics label")
    }
}

private fun Container.recordItem(title: String, subtitle: String, status: String) {
    div(className = "record item") {
        vPanel {
            span(title, className = "record title")
            span(subtitle, className = "record subtitle")
        }
        div(className = "spacer")
        val statusClass = if (status == "Reviewed") "status info" else "status neutral"
        span(status, className = statusClass)
    }
}
