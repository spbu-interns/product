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
    CONFIRMED("подтверждено", "status confirmed"),
    PENDING("ожидание", "status pending"),
    COMPLETED("завершено", "status completed")
}

private data class Appointment(
    val doctorName: String,
    val specialty: String,
    val date: String,
    val time: String,
    val location: String,
    val status: AppointmentStatus
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

    patientAccountLayout(active = PatientSection.APPOINTMENTS, onLogout = onLogout) {
        h1("Мои приёмы", className = "account title appointments-title")

        div(className = "appointments tabs") {
            val upcomingTab = button("Предстоящие", className = "tab-button is-active")
            val pastTab = button("Прошедшие", className = "tab-button")

            val upcomingList = div(className = "appointments list") {
                // TODO: Заменить на реальные данные из API
                p("Нет предстоящих приёмов", className = "empty-state")
            }

            val pastList = div(className = "appointments list") {
                // TODO: Заменить на реальные данные из API
                p("Нет завершённых приёмов", className = "empty-state")
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
                        button("Перенести", className = "btn ghost small")
                        button("Отменить", className = "btn danger small")
                    }
                } else {
                    p("Завершённый приём", className = "appointment note")
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
                span("Завершено", className = "status completed")
            }
        }

        div(className = "appointment details") {
            div(className = "details column") {
                span("Диагноз", className = "details title")
                span("Лечение мигрени", className = "details text")
            }
            div(className = "details column") {
                span("Лечение", className = "details title")
                span("Назначены лекарственные препараты", className = "details text")
            }
        }
    }
}