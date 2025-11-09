package ui

import api.ApiConfig
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.h3
import io.kvision.html.h4
import io.kvision.html.li
import io.kvision.html.nav
import io.kvision.html.span
import io.kvision.html.ul
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel

fun Container.doctorScreen(onLogout: () -> Unit = { Navigator.showHome() }) = vPanel(spacing = 12) {
    headerBar(
        mode = HeaderMode.DOCTOR,
        active = NavTab.NONE,
        onLogout = {
            ApiConfig.clearToken()
            Session.clear()
            onLogout()
        }
    )

    div(className = "doctor container") {
        div(className = "doctor grid") {
            div(className = "sidebar card doctor-sidebar") {
                div(className = "avatar circle doctor-avatar") { +"SJ" }
                h3("Dr. Sarah Johnson", className = "account name")
                span("Cardiologist", className = "doctor-specialty")

                div(className = "doctor-tags") {
                    span("New York, USA", className = "doctor-tag")
                    span("15 Years Experience", className = "doctor-tag")
                }

                nav {
                    ul(className = "side menu") {
                        li(className = "side_item is-active") {
                            span("Overview")
                            span("\uD83D\uDCC8", className = "side icon")
                        }
                        li(className = "side_item") {
                            span("Schedule")
                            span("\uD83D\uDCC5", className = "side icon")
                            onClick { Navigator.showStub("Расписание в разработке") }
                        }
                        li(className = "side_item") {
                            span("Patients")
                            span("\uD83D\uDC65", className = "side icon")
                            onClick { Navigator.showDoctorPatient(101) }
                        }
                        li(className = "side_item") {
                            span("My Records")
                            span("\uD83D\uDCDD", className = "side icon")
                            onClick { Navigator.showStub("Профиль в разработке") }
                        }
                    }
                }

                div(className = "side button")
                button("Create Appointment", className = "btn-primary-lg").onClick {
                    Navigator.showStub("Создание приема скоро будет доступно")
                }
            }

            div(className = "doctor-main column") {
                div(className = "doctor-header") {
                    div {
                        h1("Dashboard Overview", className = "doctor-title")
                        span("Today: September 14, 2025", className = "doctor-date")
                    }
                    div(className = "doctor-status") {
                        span("Patients today: 4", className = "doctor-status-pill")
                    }
                }

                div(className = "doctor-metrics grid") {
                    doctorMetric("4", "Today", "Appointments")
                    doctorMetric("127", "Patients", "Active in Care")
                    doctorMetric("4.9", "Rating", "Avg. Feedback")
                    doctorMetric("15", "Years", "Experience")
                }

                div(className = "doctor-columns") {
                    div(className = "card block doctor-appointments") {
                        h4("Today's Appointments", className = "block title")
                        div(className = "doctor-appointment-list") {
                            doctorAppointment(
                                initials = "JS",
                                name = "John Smith",
                                notes = "09:00 AM · Consultation · 30 min",
                                status = "confirmed"
                            )
                            doctorAppointment(
                                initials = "SW",
                                name = "Sarah Wilson",
                                notes = "10:30 AM · Routine Check-up · 15 min",
                                status = "confirmed"
                            )
                            doctorAppointment(
                                initials = "MJ",
                                name = "Mike Johnson",
                                notes = "01:00 PM · Follow-up · 20 min",
                                status = "pending"
                            )
                            doctorAppointment(
                                initials = "ED",
                                name = "Emma Davis",
                                notes = "03:30 PM · Consultation · 45 min",
                                status = "confirmed"
                            )
                        }
                    }

                    div(className = "doctor-aside") {
                        div(className = "card block doctor-recent-patients") {
                            h4("Recent Patients", className = "block title")
                            div(className = "doctor-patient-list") {
                                doctorRecentPatient("JS", "John Smith", "Hypertension", "Active", 101)
                                doctorRecentPatient("SW", "Sarah Wilson", "Diabetes", "Active", 102)
                                doctorRecentPatient("MJ", "Mike Johnson", "Anxiety", "Follow-up", 103)
                            }
                        }

                        div(className = "card block doctor-week-summary") {
                            h4("This Week", className = "block title")
                            div(className = "doctor-week-list") {
                                doctorWeekStat("Total Appointments", "18")
                                doctorWeekStat("New Patients", "3")
                                doctorWeekStat("Follow-ups", "7")
                                doctorWeekStat("Consultations", "8")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Container.doctorMetric(value: String, label: String, subtitle: String) {
    div(className = "doctor-metric card") {
        hPanel(className = "doctor-metric-header") {
            span(value, className = "doctor-metric-value")
            span(label, className = "doctor-metric-label")
        }
        span(subtitle, className = "doctor-metric-subtitle")
    }
}

private fun Container.doctorAppointment(
    initials: String,
    name: String,
    notes: String,
    status: String
) {
    val statusClass = when (status.lowercase()) {
        "confirmed" -> "status success"
        "pending" -> "status warning"
        "cancelled" -> "status danger"
        else -> "status neutral"
    }

    div(className = "doctor-appointment") {
        div(className = "doctor-appointment-avatar") { +initials }
        div(className = "doctor-appointment-info") {
            span(name, className = "doctor-appointment-name")
            span(notes, className = "doctor-appointment-notes")
        }
        span(status, className = statusClass)
    }
}

private fun Container.doctorRecentPatient(
    initials: String,
    name: String,
    condition: String,
    status: String,
    patientId: Long
) {
    div(className = "doctor-patient-item") {
        div(className = "doctor-patient-avatar") { +initials }
        div(className = "doctor-patient-info") {
            span(name, className = "doctor-patient-name")
            span(condition, className = "doctor-patient-condition")
        }
        span(status, className = "status info")
        onClick { Navigator.showDoctorPatient(patientId) }
    }
}

private fun Container.doctorWeekStat(label: String, value: String) {
    div(className = "doctor-week-item") {
        span(label, className = "doctor-week-label")
        span(value, className = "doctor-week-value")
    }
}