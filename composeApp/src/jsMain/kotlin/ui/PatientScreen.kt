package ui

import api.ApiConfig
import api.BookingApiClient
import io.kvision.core.Container
import io.kvision.core.onClick

import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.h3
import io.kvision.html.h4
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.panel.vPanel
import io.kvision.utils.perc
import io.kvision.toast.Toast
import io.kvision.types.toDateF
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import state.PatientState
import kotlin.js.Date


fun Container.patientScreen(onLogout: () -> Unit = { Navigator.showHome() }) = vPanel(spacing = 12) {
    val state = PatientState
    val patientId = Session.userId
    val bookingClient = BookingApiClient()
    val scope = MainScope()

    headerBar(
        mode = HeaderMode.PATIENT,
        active = NavTab.PROFILE,
        onLogout = {
            ApiConfig.clearToken()
            Session.clear()
            Navigator.showHome()
        }
    )

    fun renderDashboard(container: Container) {
        container.removeAll()
        with(container) {
            h1("Аккаунт", className = "account title")

            val dashboard = state.dashboardData

            val upcomingAppointments =
                dashboard?.appointments?.filter { it.status == "BOOKED" } ?: emptyList()
            val recentMedicalRecords = dashboard?.medicalRecords?.take(3) ?: emptyList()
            val nextAppointmentDetails = dashboard?.nextAppointment
            val nextAppointment = nextAppointmentDetails?.let { details ->
                dashboard.appointments.firstOrNull { it.id == details.appointmentId }
            } ?: upcomingAppointments.firstOrNull()

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

                nextAppointmentDetails?.let { details ->
                    val doctorName = listOfNotNull(
                        details.doctorSurname,
                        details.doctorName,
                        details.doctorPatronymic
                    ).filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifBlank { "Врач не указан" }

                    val appointmentId = details.appointmentId
                    val appointment = nextAppointment

                    // Кликабельная карточка приёма (как у врача)
                    div(className = "appointment card full next-appointment-card") {
                        onClick { Navigator.showAppointmentDetails(appointmentId) }

                        // Внутреннее содержимое карточки
                        div(className = "appointment row") {
                            // Аватар-иконка пациента/врача — можно просто эмодзи
                            div(className = "appointment avatar colored") { +"📅" }

                            // Основная информация
                            div(className = "appointment info") {
                                h4("Запись #$appointmentId", className = "appointment-title")
                                p("Врач: $doctorName", className = "appointment-doctor")
                                details.doctorProfession?.takeIf { it.isNotBlank() }?.let { profession ->
                                    p("Специальность: $profession", className = "appointment-profession")
                                }
                                details.slotStart?.let { start ->
                                    val date = Date(start)
                                    p("Начало: ${formatDateTime(date)}", className = "appointment-time")
                                }

                                appointment?.comments
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { comments ->
                                        p("Комментарии: $comments", className = "appointment-comments")
                                    }
                            }

                            // Кнопка отмены
                            div(className = "appointment actions") {
                                appointment?.let { appointmentToCancel ->
                                    button("Отменить", className = "btn-outline") {
                                        onClick {
                                            val id = appointmentToCancel.id
                                            val userId = patientId
                                            if (userId == null) {
                                                Toast.danger("Не удалось определить пользователя")
                                                return@onClick
                                            }

                                            scope.launch {
                                                val result = bookingClient.cancelAppointment(id)
                                                result.onSuccess { success ->
                                                    if (success) {
                                                        Toast.success("Запись отменена")
                                                        state.loadPatientDashboard(userId)
                                                    } else {
                                                        Toast.danger("Не удалось отменить запись")
                                                    }
                                                }.onFailure { error ->
                                                    Toast.danger(error.message ?: "Ошибка отмены записи")
                                                }
                                            }
                                        }
                                    }
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

    lateinit var content: Container

    patientAccountLayout(
        active = PatientSection.OVERVIEW,
        onLogout = onLogout
    ) {
        content = this
        renderDashboard(this)
    }

    state.onUpdate = {
        renderDashboard(content)
    }

    if (patientId != null) {
        state.loadPatientDashboard(patientId)
    }
}

private fun Container.statisticsCard(value: String, label: String, icon: String) {
    div(className = "statistics card") {
        span(icon, className = "statistics icon")
        h3(value, className = "statistics value")
        span(label, className = "statistics label")
    }
}

private fun parseDate(value: String?): Date? {
    if (value == null) return null
    return runCatching { Date(value) }.getOrNull()
        ?.takeUnless { it.toString() == "Invalid Date" }
}

private fun formatDateTime(date: Date): String {
    val day = date.getDate().toString().padStart(2, '0')
    val month = (date.getMonth() + 1).toString().padStart(2, '0')
    val hours = date.getHours().toString().padStart(2, '0')
    val minutes = date.getMinutes().toString().padStart(2, '0')
    return "$day.$month.${date.getFullYear()} • $hours:$minutes"
}
