package ui

import api.ApiConfig
import io.kvision.core.Container
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.h3
import io.kvision.html.h4
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.panel.vPanel
import io.kvision.utils.perc
import state.PatientState
import utils.normalizeGender

fun Container.patientScreen(onLogout: () -> Unit = { Navigator.showHome() }) = vPanel(spacing = 12) {
    val state = PatientState

    // Загружаем данные при создании экрана
    val patientId = Session.userId
    if (patientId != null) {
        state.loadPatientDashboard(patientId) // Используем новый метод
    }

    headerBar(
        mode = HeaderMode.PATIENT,
        active = NavTab.NONE,
        onLogout = {
            ApiConfig.clearToken()
            Session.clear()
            Navigator.showHome()
        }
    )

    patientAccountLayout(
        active = PatientSection.OVERVIEW,
        onLogout = onLogout
    ) {
        h1("Аккаунт", className = "account title")

        val dashboard = state.dashboardData

        val upcomingAppointments = dashboard?.appointments?.filter { it.status == "BOOKED" } ?: emptyList()
        val recentMedicalRecords = dashboard?.medicalRecords?.take(3) ?: emptyList()
        val nextAppointment = upcomingAppointments.firstOrNull()

        div(className = "statistics grid patient-grid") {
            statisticsCard(
                upcomingAppointments.size.toString(),
                "Предстоящие",
                "\uD83D\uDCC5"
            )
            statisticsCard(
                (dashboard?.medicalRecords?.size ?: 0).toString(),
                "Мед. записи",
                "\uD83D\uDCC4"
            )
        }

        div(className = "card block appointment-block") {
            width = 100.perc
            h4("Следующий приём", className = "block title")

            nextAppointment?.let { appointment ->
                div(className = "appointment-info") {
                    div(className = "appointment-header") {
                        h4("Запись #${appointment.id}", className = "appointment-title")
                        span(appointment.status, className = "appointment-status")
                    }
                    appointment.comments?.takeIf { it.isNotBlank() }?.let { comments ->
                        p("Комментарии: $comments", className = "appointment-comments")
                    }
                    div(className = "appointment-actions") {
                        button("Подробнее", className = "btn-secondary") {
                            onClick {
                                // TODO: Переход к деталям записи
                            }
                        }
                        button("Отменить", className = "btn-outline") {
                            onClick {
                                // TODO: Логика отмены записи
                            }
                        }
                    }
                }
            } ?: run {
                div(className = "empty-state") {
                    p("Нет предстоящих приёмов")
                    button("Найти врача", className = "btn-primary-lg").onClick {
                        Navigator.showFind()
                    }
                }
            }
        }

        h4("Последние медицинские записи", className = "block title")
        div(className = "card block") {
            width = 100.perc
            div(className = "records list") {
                if (recentMedicalRecords.isNotEmpty()) {
                    recentMedicalRecords.forEach { record ->
                        div(className = "medical-record") {
                            div(className = "record-header") {
                                h4("Запись #${record.id}", className = "record-title")
                                span(record.createdAt, className = "record-date")
                            }
                            div(className = "record-content") {
                                record.diagnosis?.takeIf { it.isNotBlank() }?.let { diagnosis ->
                                    div(className = "record-field") {
                                        span("Диагноз: ", className = "field-label")
                                        span(diagnosis, className = "field-value")
                                    }
                                }
                                record.symptoms?.takeIf { it.isNotBlank() }?.let { symptoms ->
                                    div(className = "record-field") {
                                        span("Симптомы: ", className = "field-label")
                                        span(symptoms, className = "field-value")
                                    }
                                }
                                record.treatment?.takeIf { it.isNotBlank() }?.let { treatment ->
                                    div(className = "record-field") {
                                        span("Лечение: ", className = "field-label")
                                        span(treatment, className = "field-value")
                                    }
                                }
                            }
                            div(className = "record-actions") {
                                button("Подробнее", className = "btn-text") {
                                    onClick {
                                        // TODO: Переход к полной информации о записи
                                    }
                                }
                            }
                        }
                    }
                } else {
                    div(className = "empty-state") {
                        p("Нет медицинских записей")
                    }
                }
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