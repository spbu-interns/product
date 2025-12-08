package ui

import api.ApiConfig
import api.PatientApiClient
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.core.onEvent
import io.kvision.core.onInput
import io.kvision.form.text.TextArea
import io.kvision.form.text.textArea
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.modal.Modal
import io.kvision.modal.ModalSize
import io.kvision.panel.SimplePanel
import io.kvision.panel.simplePanel
import io.kvision.panel.vPanel
import io.kvision.toast.Toast
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.interns.project.dto.AppointmentReviewRequest
import org.interns.project.dto.AppointmentWithReviewDto
import kotlin.js.Date

fun Container.patientAppointmentsScreen(onLogout: () -> Unit = { Navigator.showHome() }) = vPanel(spacing = 12) {
    headerBar(
        mode = HeaderMode.PATIENT,
        active = NavTab.PROFILE,
        onLogout = {
            ApiConfig.clearToken()
            Session.clear()
            Navigator.showHome()
        }
    )

    val scope = MainScope()
    val apiClient = PatientApiClient()
    var clientId: Long? = null

    var appointments: List<AppointmentWithReviewDto> = emptyList()
    var pendingInvites: List<AppointmentWithReviewDto> = emptyList()
    var loading = true
    var selectedAppointment: AppointmentWithReviewDto? = null
    var rating = 0
    var hoverRating = 0
    var commentText = ""

    val reviewModal = Modal("Отзыв о визите", closeButton = true, animation = true, size = io.kvision.modal.ModalSize.LARGE)

    lateinit var pendingBanner: SimplePanel
    lateinit var pastList: Container
    lateinit var starContainer: Container
    var commentField: TextArea? = null

    fun formatDateTime(iso: String): String {
        val d = Date(iso)
        val day = d.getDate().toString().padStart(2, '0')
        val month = (d.getMonth() + 1).toString().padStart(2, '0')
        val hours = d.getHours().toString().padStart(2, '0')
        val minutes = d.getMinutes().toString().padStart(2, '0')
        return "$day.$month.${d.getFullYear()} • $hours:$minutes"
    }

    fun renderStars(container: Container) {
        container.removeAll()
        (1..5).forEach { index ->
            container.span(
                "★",
                className = "review-star " +
                        if (index <= (hoverRating.takeIf { it > 0 } ?: rating)) "is-active" else ""
            ) {
                onClick {
                    rating = index
                    renderStars(container)
                }
                onEvent {
                    mouseover = { _ ->
                        hoverRating = index
                        renderStars(container)
                    }
                    mouseout = { _ ->
                        hoverRating = 0
                        renderStars(container)
                    }
                }
            }
        }
    }

    fun openReviewModal(appointment: AppointmentWithReviewDto) {
        selectedAppointment = appointment
        rating = appointment.review?.rating ?: 0
        hoverRating = 0
        commentText = appointment.review?.comment ?: ""
        renderStars(starContainer)
        commentField?.value = commentText
        reviewModal.show()
    }

    suspend fun loadClientData(userId: Long) {
        clientId = apiClient.getClientId(userId).getOrNull()
        clientId?.let { id ->
            appointments = apiClient.getAppointmentHistoryWithReviews(id).getOrDefault(emptyList())

            val explicitInvites = apiClient.getPendingReviewAppointments(id).getOrDefault(emptyList())
            val localPending = appointments.filter { it.status == "COMPLETED" && it.review == null }

            pendingInvites = (explicitInvites + localPending)
                .distinctBy { it.appointmentId }
                .sortedByDescending { Date(it.slotStart).getTime() }
        }
        loading = false
    }

    fun refreshData() {
        loading = true
        Session.userId?.let { uid ->
            scope.launch {
                loadClientData(uid)
                pastList.refreshPastAppointments(appointments, loading, ::openReviewModal, ::formatDateTime)
                pendingBanner.refreshBanner(pendingInvites, ::openReviewModal, ::formatDateTime)
            }
        }
    }

    patientAccountLayout(active = PatientSection.APPOINTMENTS, onLogout = onLogout) {
        h1("Мои приёмы", className = "account title appointments-title")

        pendingBanner = simplePanel {}
        add(pendingBanner)

        div(className = "appointments tabs") {
            val upcomingTab = button("Предстоящие", className = "tab-button is-active")
            val pastTab = button("Прошедшие", className = "tab-button")

            val upcomingList = div(className = "appointments list") {
                p("Нет предстоящих приёмов", className = "empty-state")
            }

            pastList = div(className = "appointments list") {
                p("Загрузка...", className = "empty-state")
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

        reviewModal.apply {
            div(className = "review-modal-body") {
                div(className = "review-summary") {
                    span("Оцените визит", className = "review-label")
                    starContainer = simplePanel(className = "review-stars") { renderStars(this) }
                }

                div(className = "review-comment") {
                    span("Комментарий (необязательно)", className = "review-label")
                    commentField = textArea(
                        value = commentText,
                        rows = 4
                    ) {
                        addCssClass("input")

                        placeholder = "Расскажите, как прошёл приём"
                        onEvent {
                            input = { _ ->
                                commentText = value ?: ""
                            }
                        }
                    }
                }

                div(className = "review-footer") {
                    button("Отменить", className = "btn ghost") {
                        onClick { reviewModal.hide() }
                    }
                    button("Сохранить", className = "btn primary") {
                        onClick {
                            val current = selectedAppointment ?: return@onClick
                            if (rating == 0) {
                                Toast.success("Пожалуйста, оцените визит")
                                return@onClick
                            }

                            clientId?.let { cid ->
                                scope.launch {
                                    apiClient
                                        .saveAppointmentReview(
                                            current.appointmentId,
                                            AppointmentReviewRequest(rating = rating, comment = commentText.ifBlank { null })
                                        )
                                    reviewModal.hide()
                                    loadClientData(Session.userId ?: return@launch)
                                    pastList.refreshPastAppointments(
                                        appointments,
                                        loading,
                                        ::openReviewModal,
                                        ::formatDateTime
                                    )
                                    pendingBanner.refreshBanner(
                                        pendingInvites,
                                        ::openReviewModal,
                                        ::formatDateTime
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        refreshData()
    }
}

private fun AppointmentWithReviewDto.statusLabel(): String = when (status) {
    "COMPLETED" -> "Завершено"
    "CANCELED" -> "Отменено"
    "NO_SHOW" -> "Не состоялось"
    else -> status
}

private fun Container.appointmentCard(
    appointment: AppointmentWithReviewDto,
    onReview: (AppointmentWithReviewDto) -> Unit,
    formatDateTime: (String) -> String
) {
    div(className = "appointment card full") {
        div(className = "appointment row") {
            div(className = "appointment avatar colored") { +"👤" }

            div(className = "appointment info") {
                span(appointment.doctorName ?: "Врач #${appointment.doctorId}", className = "appointment doctor")
                appointment.doctorProfession?.let {
                    span(it, className = "appointment appointment-specialty")
                }

                div(className = "appointment meta") {
                    span("📅 ${formatDateTime(appointment.slotStart)}")
                }
            }

            div(className = "appointment actions") {
                span(appointment.statusLabel(), className = "status completed")

                if (appointment.status == "COMPLETED") {
                    div(className = "appointment buttons") {
                        val label = if (appointment.review == null) "Оставить отзыв" else "Изменить отзыв"
                        button(label, className = "btn primary small") {
                            onClick { onReview(appointment) }
                        }
                    }
                }
            }
        }
        appointment.review?.let { review ->
            div(className = "appointment note") {
                span("Ваша оценка: ${review.rating}★", className = "review-label")
                review.comment?.let { span(it) }
            }
        }
    }
}

private fun SimplePanel.refreshBanner(
    pending: List<AppointmentWithReviewDto>,
    onReview: (AppointmentWithReviewDto) -> Unit,
    formatDateTime: (String) -> String
) {
    removeAll()
    if (pending.isEmpty()) {
        visible = false
        return
    }

    visible = true
    val next = pending.first()
    div(className = "notification banner info") {
        span("Приём завершён — поделитесь впечатлением", className = "banner-title")
        p(
            "${next.doctorName ?: "Врач #${next.doctorId}"} • ${formatDateTime(next.slotStart)}",
            className = "banner-meta"
        )
        button("Оставить отзыв", className = "btn primary") {
            onClick { onReview(next) }
        }
    }
}

private fun Container.refreshPastAppointments(
    data: List<AppointmentWithReviewDto>,
    loading: Boolean,
    onReview: (AppointmentWithReviewDto) -> Unit,
    formatDateTime: (String) -> String
) {
    removeAll()
    if (loading) {
        p("Загрузка...", className = "empty-state")
        return
    }

    if (data.isEmpty()) {
        p("Нет завершённых приёмов", className = "empty-state")
        return
    }

    data.forEach { appointment ->
        appointmentCard(appointment, onReview, formatDateTime)
    }
}