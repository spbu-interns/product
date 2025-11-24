package ui

import api.ApiConfig
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.panel.vPanel

private enum class AppointmentStatus(val label: String, val cssClass: String) {
    CONFIRMED("confirmed", "status confirmed"),
    PENDING("pending", "status pending"),
    COMPLETED("completed", "status completed")
}

private data class Appointment(
    val doctorName: String,
    val specialty: String,
    val date: String,
    val time: String,
    val location: String,
    val status: AppointmentStatus
)

private val upcomingAppointments = listOf(
    Appointment(
        doctorName = "Dr. Sarah Johnson",
        specialty = "Cardiology",
        date = "September 18, 2025",
        time = "10:00 AM",
        location = "Heart Center",
        status = AppointmentStatus.CONFIRMED,
    ),
    Appointment(
        doctorName = "Dr. Michael Chen",
        specialty = "Pediatrics",
        date = "September 25, 2025",
        time = "2:30 PM",
        location = "Children's Medical Center",
        status = AppointmentStatus.PENDING,
    ),
)

private val pastAppointments = listOf(
    Appointment(
        doctorName = "Dr. Anna Smith",
        specialty = "Dermatology",
        date = "August 2, 2025",
        time = "4:00 PM",
        location = "Sunrise Clinic",
        status = AppointmentStatus.COMPLETED,
    ),
    Appointment(
        doctorName = "Dr. James Patel",
        specialty = "Orthopedics",
        date = "July 14, 2025",
        time = "11:15 AM",
        location = "City Hospital",
        status = AppointmentStatus.COMPLETED,
    ),
)

fun Container.patientAppointmentsScreen(onLogout: () -> Unit = { Navigator.showHome() }) = vPanel(spacing = 12) {
    headerBar(
        mode = HeaderMode.PATIENT,
        active = NavTab.NONE,
        onLogout = {
            ApiConfig.clearToken()
            Session.clear()
            Navigator.showHome()
        }
    )

    patientAccountLayout(active = PatientSection.APPOINTMENTS) {
        h1("My Appointments", className = "account title appointments-title")

        div(className = "appointments tabs") {
            val upcomingTab = button("Upcoming", className = "tab-button is-active")
            val pastTab = button("Past", className = "tab-button")

            val upcomingList = div(className = "appointments list") {
                upcomingAppointments.forEach { appointmentCard(it, isPast = false) }
            }

            val pastList = div(className = "appointments list") {
                pastAppointments.forEach {
                    pastAppointmentCard(it)
                }
            }.apply { visible = false }

            fun activateUpcoming() {
                upcomingList.visible = true
                pastList.visible = false
                upcomingTab.addCssClass("is-active")
                pastTab.removeCssClass("is-active")
            }

            fun activatePast() {
                upcomingList.visible = false
                pastList.visible = true
                pastTab.addCssClass("is-active")
                upcomingTab.removeCssClass("is-active")
            }

            upcomingTab.onClick { activateUpcoming() }
            pastTab.onClick { activatePast() }

            div(className = "tab-buttons") {
                add(upcomingTab)
                add(pastTab)
            }

            add(upcomingList)
            add(pastList)
        }
    }
}

private fun Container.appointmentCard(appointment: Appointment, isPast: Boolean) {
    div(className = "appointment card full") {
        div(className = "appointment row") {
            div(className = "appointment avatar colored") { + "👤" }

            div(className = "appointment info") {
                span(appointment.doctorName, className = "appointment doctor")
                span(appointment.specialty, className = "appointment appointment-specialty")

                div(className = "appointment meta") {
                    span("📅 ${appointment.date}")
                    span("⏰ ${appointment.time}")
                    span("📍 ${appointment.location}")
                }
            }

            div(className = "appointment actions") {
                span(appointment.status.label, className = appointment.status.cssClass)

                if (!isPast) {
                    div(className = "appointment buttons") {
                        button("Reschedule", className = "btn ghost small")
                        button("Cancel", className = "btn danger small")
                    }
                } else {
                    p("Completed appointment", className = "appointment note")
                }
            }
        }
    }
}

private fun Container.pastAppointmentCard(appointment: Appointment) {
    div(className = "appointment card full") {

        div(className = "appointment row") {
            div(className = "appointment avatar colored") { +"👤" }

            div(className = "appointment info") {
                span(appointment.doctorName, className = "appointment doctor")
                span(appointment.specialty, className = "appointment appointment-specialty")

                div(className = "appointment meta") {
                    span("📅 ${appointment.date}")
                    span("⏰ ${appointment.time}")
                    span("📍 ${appointment.location}")
                }
            }

            div(className = "appointment actions") {
                span("Completed", className = "status completed")
            }
        }

        div(className = "appointment details") {
            div(className = "details column") {
                span("Diagnosis", className = "details title")
                span("Migraine management", className = "details text")
            }
            div(className = "details column") {
                span("Treatment", className = "details title")
                span("Prescription medication provided", className = "details text")
            }
        }
    }
}