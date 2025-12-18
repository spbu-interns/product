package ui.components

import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.span
import io.kvision.toast.Toast
import state.ActiveAppointmentStatus
import state.DoctorState
import ui.Navigator
import ui.Session
import kotlin.js.Date

private fun formatTime(date: Date): String {
    val hours = date.getHours().toString().padStart(2, '0')
    val minutes = date.getMinutes().toString().padStart(2, '0')
    return "$hours:$minutes"
}

private fun formatPatientName(surname: String?, name: String?, patronymic: String?): String? {
    val parts = listOfNotNull(surname, name, patronymic)
    return if (parts.isNotEmpty()) parts.joinToString(" ") else null
}

fun Container.doctorActiveAppointmentBanner(state: DoctorState = DoctorState) {
    val modalController = activeAppointmentModal(state)

    val banner = div(className = "active-appointment-banner") {
        visible = false
    }

    fun renderBanner() {
        banner.removeAll()
        val active = state.activeAppointment
        if (active == null) {
            banner.visible = false
            return
        }

        val patientName = formatPatientName(
            active.patient?.surname,
            active.patient?.name,
            active.patient?.patronymic
        ) ?: "Пациент #${active.appointment.clientId}"

        banner.visible = true
        banner.addCssClass("visible")

        banner.div(className = "active-appointment-content") {
            div(className = "active-appointment-text") {
                span("Сейчас у вас на приёме ")
                button(patientName, className = "link-button") {
                    onClick {
                        val userId = active.patient?.userId
                        val recordId = active.patient?.clientId
                        if (userId != null && recordId != null) {
                            Navigator.showDoctorPatient(userId, recordId)
                        } else {
                            Toast.info("Профиль пациента недоступен")
                        }
                    }
                }
            }

            div(className = "active-appointment-actions") {
                button("Подробнее", className = "btn-secondary-sm") {
                    onClick { modalController.open() }
                }

                if (active.status == ActiveAppointmentStatus.ACTIVE) {
                    button("Закончить приём досрочно", className = "btn-danger-sm") {
                        onClick {
                            val doctorUserId = Session.userId
                            if (doctorUserId == null) {
                                Toast.danger("Не удалось определить пользователя")
                                return@onClick
                            }

                            state.completeAppointment(active.appointment.id, doctorUserId) { success, error ->
                                if (success) {
                                    Toast.success("Приём завершён")
                                    modalController.close()
                                } else if (error != null) {
                                    Toast.danger(error)
                                }
                            }
                        }
                    }
                } else {
                    span("Приём завершён", className = "active-appointment-status")
                }
            }
        }
    }

    state.subscribe { renderBanner() }

    val doctorUserId = Session.userId
    if (doctorUserId != null && state.dashboardData == null && !state.isLoading) {
        state.loadDoctorDashboard(doctorUserId)
    } else {
        renderBanner()
    }

    // Keep banner up to date with any schedule or slot updates
    renderBanner()
}

private class ActiveAppointmentModalController(
    private val renderModal: () -> Unit,
    val open: () -> Unit,
    val close: () -> Unit
)

private fun Container.activeAppointmentModal(state: DoctorState): ActiveAppointmentModalController {
    var isOpen = false
    val modalContainer = div()

    fun renderModal() {
        modalContainer.removeAll()
        if (!isOpen) return

        val active = state.activeAppointment ?: return
        val patientName = formatPatientName(
            active.patient?.surname,
            active.patient?.name,
            active.patient?.patronymic
        ) ?: "Пациент #${active.appointment.clientId}"
        val interval = "${formatTime(active.start)} - ${formatTime(active.end)}"
        val complaint = active.appointment.comments?.takeIf { it.isNotBlank() } ?: "Жалоба не указана"
        val price = state.dashboardData?.doctor?.price?.let { "${it.toInt()} ₽" } ?: "—"

        modalContainer.div(className = "active-appointment-backdrop") {
            onClick { isOpen = false; renderModal() }
        }

        modalContainer.div(className = "active-appointment-modal") {
            div(className = "active-appointment-header") {
                span("Текущий приём", className = "active-appointment-title")
                button("×", className = "modal-close") {
                    onClick { isOpen = false; renderModal() }
                }
            }

            div(className = "active-appointment-body") {
                div(className = "active-appointment-row") {
                    span("Пациент", className = "active-appointment-label")
                    span(patientName, className = "active-appointment-value")
                }
                div(className = "active-appointment-row") {
                    span("Время", className = "active-appointment-label")
                    span(interval, className = "active-appointment-value")
                }
                div(className = "active-appointment-row") {
                    span("Жалоба", className = "active-appointment-label")
                    span(complaint, className = "active-appointment-value")
                }
                div(className = "active-appointment-row") {
                    span("Стоимость", className = "active-appointment-label")
                    span(price, className = "active-appointment-value")
                }
            }

            div(className = "active-appointment-footer") {
                if (active.status == ActiveAppointmentStatus.ACTIVE) {
                    button("Завершить приём", className = "btn-primary") {
                        onClick {
                            val doctorUserId = Session.userId
                            if (doctorUserId == null) {
                                Toast.danger("Не удалось определить пользователя")
                                return@onClick
                            }
                            state.completeAppointment(active.appointment.id, doctorUserId) { success, error ->
                                if (success) {
                                    Toast.success("Приём завершён")
                                    isOpen = false
                                    renderModal()
                                } else if (error != null) {
                                    Toast.danger(error)
                                }
                            }
                        }
                    }
                } else {
                    span("Слот завершён", className = "active-appointment-status")
                }
                button("Закрыть", className = "btn-ghost-sm") {
                    onClick { isOpen = false; renderModal() }
                }
            }
        }
    }

    fun openModal() {
        isOpen = true
        renderModal()
    }

    fun closeModal() {
        isOpen = false
        renderModal()
    }

    renderModal()

    return ActiveAppointmentModalController(
        renderModal = ::renderModal,
        open = ::openModal,
        close = ::closeModal
    )
}