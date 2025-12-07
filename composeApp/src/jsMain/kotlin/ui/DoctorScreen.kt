package ui

import api.ApiConfig
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.html.Div
import io.kvision.html.H4
import io.kvision.html.Span
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.h4
import io.kvision.html.li
import io.kvision.html.nav
import io.kvision.html.span
import io.kvision.html.ul
import io.kvision.panel.vPanel
import io.kvision.toast.Toast
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import state.DoctorState
import state.DoctorState.dashboardData
import ui.components.timetableModal
import ui.components.updateAvatar
import utils.normalizeGender

private data class DoctorPatientListItem(
    val userId: Long,
    val patientRecordId: Long,
    val name: String,
    val subtitle: String,
    val status: String,
    val initials: String,
)

private data class DoctorAppointmentCard(
    val initials: String,
    val name: String,
    val notes: String,
    val status: String,
)

fun Container.doctorScreen(onLogout: () -> Unit = { Navigator.showHome() }) = vPanel(spacing = 12) {
    val uiScope = MainScope()
    val state = DoctorState
    val timetableController = timetableModal()

    fun cleanup() {
        uiScope.cancel()
    }

    // Загружаем данные при создании экрана
    val userId = Session.userId
    if (userId != null) {
        state.loadDoctorDashboard(userId)
    }

    // Используем данные из состояния или сессии как fallback
    val initialDashboard = state.dashboardData
    val initialDoctorName = initialDashboard?.user?.let { user ->
        listOfNotNull(user.surname, user.name, user.patronymic)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" ")
    } ?: Session.fullName() ?: Session.email ?: "Врач"
    val initialDoctorAvatarUrl = initialDashboard?.user?.avatar ?: Session.avatar

    val initialDoctorInitials = initialDoctorName
        .split(' ', '-', '_')
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifBlank { Session.email?.firstOrNull()?.uppercaseChar()?.toString() ?: "ВР" }

    val defaultSpecialty = "Специальность не указана"
    val doctorSubtitleSpan = Span(
        initialDashboard?.doctor?.profession?.takeIf { it.isNotBlank() } ?: defaultSpecialty,
        className = "account id"
    )

    lateinit var patientsContainer: Container
    lateinit var overviewContainer: Container
    lateinit var scheduleContainer: Container
    lateinit var avatarContainer: Div
    lateinit var doctorNameHeader: H4
    var scheduleListContainer: Container? = null

    var renderPatients: () -> Unit = {}
    var renderSchedule: () -> Unit = {}

    fun DoctorPatientListItem.render() {
        val statusClass = when (status.lowercase()) {
            "confirmed", "active" -> "status success"
            "new" -> "status info"
            else -> "status neutral"
        }

        patientsContainer.div(className = "record item") {
            vPanel {
                span(name, className = "record title")
                span(subtitle, className = "record subtitle")
            }

            onClick {
                cleanup()
                Navigator.showDoctorPatient(userId, patientRecordId)
            }
        }
    }

    fun Container.renderSchedulePatient(item: DoctorPatientListItem) {
        val statusClass = when (item.status.lowercase()) {
            "confirmed", "active" -> "status success"
            "new" -> "status info"
            else -> "status neutral"
        }

        div(className = "record item") {
            vPanel {
                span(item.name, className = "record title")
                span(item.subtitle, className = "record subtitle")
            }

            span(content = item.status, className = statusClass)

            onClick {
                cleanup()
                Navigator.showDoctorPatient(item.userId, item.patientRecordId)
            }
        }
    }

    renderPatients = fun() {
        patientsContainer.removeAll()
        val dashboard = state.dashboardData

        when {
            state.isLoading -> {
                patientsContainer.div(className = "doctor-empty-state") {
                    span("Загрузка пациентов...", className = "doctor-patient-condition")
                }
            }

            state.error != null -> {
                patientsContainer.div(className = "record item") {
                    span("Ошибка загрузки", className = "record subtitle")
                    button("Повторить", className = "btn-ghost-sm").onClick {
                        userId?.let { state.loadDoctorDashboard(it) }
                    }
                }
            }

            dashboard?.patients.isNullOrEmpty() -> {
                patientsContainer.div(className = "record item") {
                    span("Пациенты не найдены", className = "record subtitle")
                    button("Обновить", className = "btn-ghost-sm").onClick {
                        userId?.let { state.loadDoctorDashboard(it) }
                    }
                }
            }

            else -> {
                dashboard.patients.forEach { patient ->
                    val initials = patient.name?.take(2)?.uppercase() ?: "ПЦ"
                    val name = listOfNotNull(patient.surname, patient.name, patient.patronymic)
                        .takeIf { it.isNotEmpty() }?.joinToString(" ") ?: "Пациент #${patient.userId}"
                    val subtitle = patient.phoneNumber ?: patient.dateOfBirth ?: "Подробнее..."

                    DoctorPatientListItem(
                        userId = patient.userId,
                        patientRecordId = patient.clientId,
                        name = name,
                        subtitle = subtitle,
                        status = "active",
                        initials = initials
                    ).render()
                }
            }
        }
    }

    renderSchedule = fun() {
        val listContainer = scheduleListContainer ?: return
        listContainer.removeAll()
        val dashboard = state.dashboardData

        when {
            state.isLoading -> {
                listContainer.div(className = "doctor-empty-state") {
                    span("Загрузка расписания...", className = "doctor-patient-condition")
                }
            }

            state.error != null -> {
                listContainer.div(className = "record item") {
                    span(state.error ?: "Не удалось загрузить расписание", className = "record subtitle")
                    button("Повторить", className = "btn-ghost-sm").onClick {
                        userId?.let { state.loadDoctorDashboard(it) }
                    }
                }
            }

            dashboard?.patients.isNullOrEmpty() -> {
                listContainer.div(className = "record item") {
                    span("Пока нет записанных пациентов", className = "record subtitle")
                }
            }

            else -> {
                dashboard.patients.forEach { patient ->
                    val initials = patient.name?.take(2)?.uppercase() ?: "ПЦ"
                    val name = listOfNotNull(patient.surname, patient.name, patient.patronymic)
                        .takeIf { it.isNotEmpty() }?.joinToString(" ") ?: "Пациент #${patient.userId}"
                    val subtitle = patient.phoneNumber ?: patient.dateOfBirth ?: "Подробнее..."

                    val patientItem = DoctorPatientListItem(
                        userId = patient.userId,
                        patientRecordId = patient.clientId,
                        name = name,
                        subtitle = subtitle,
                        status = "active",
                        initials = initials
                    )
                    listContainer.renderSchedulePatient(patientItem)
                }
            }
        }
    }

    headerBar(
        mode = HeaderMode.DOCTOR,
        active = NavTab.PROFILE,
        onLogout = {
            ApiConfig.clearToken()
            Session.clear()
            cleanup()
            onLogout()
        }
    )

    div(className = "account container") {
        div(className = "account grid") {
            div(className = "sidebar card") {
                avatarContainer = div(className = "avatar circle") {}
                avatarContainer.updateAvatar(initialDoctorAvatarUrl, initialDoctorInitials)
                doctorNameHeader = h4(initialDoctorName, className = "account name")
                add(doctorSubtitleSpan)

                nav {
                    ul(className = "side menu") {
                        li(className = "side_item is-active") {
                            span("Обзор")
                            span("\uD83D\uDC64", className = "side icon")
                            onClick {
                                window.asDynamic().scrollTo(js("({ top: 0, behavior: 'smooth' })"))
                                overviewContainer.visible = true
                                scheduleContainer.visible = false
                            }
                        }
                        li(className = "side_item") {
                            span("Расписание")
                            span("\uD83D\uDCC5", className = "side icon")
                            onClick {
                                window.asDynamic().scrollTo(js("({ top: 0, behavior: 'smooth' })"))
                                overviewContainer.visible = false
                                scheduleContainer.visible = true
                                renderSchedule()
                            }
                        }
                        li(className = "side_item") {
                            span("Пациенты")
                            span("\uD83D\uDC65", className = "side icon")
                            onClick {
                                window.asDynamic().scrollTo(js("({ top: 0, behavior: 'smooth' })"))
                                Toast.info("История посещений пациентов скоро будет доступна")
                            }
                        }
                        li(className = "side_item") {
                            span("Мой профиль")
                            span("\uD83D\uDC64", className = "side icon")
                            onClick {
                                window.asDynamic().scrollTo(js("({ top: 0, behavior: 'smooth' })"))
                                Navigator.showDoctorProfileEdit()
                            }
                        }
                    }
                }

                div(className = "side button")
                button("Создать приём", className = "btn-primary-lg").onClick {
                    Navigator.showStub("Создание приема скоро будет доступно")
                }
                button("Расписание", className = "btn-secondary-lg timetable-trigger").onClick {
                    val currentDoctorId = state.dashboardData?.doctor?.id
                    if (currentDoctorId == null) {
                        Toast.danger("Не удалось загрузить профиль врача")
                    } else {
                        timetableController.open(doctorNameHeader.content.toString(), currentDoctorId)
                    }
                }
                button("Выйти", className = "btn-logout-sm").onClick {
                    ApiConfig.clearToken()
                    Session.clear()
                    cleanup()
                    onLogout()
                }
            }

            div(className = "main column") {

                overviewContainer = div {
                    h1("Аккаунт", className = "account title")

                    // Статистика
                    val todayAppointments = dashboardData?.appointments?.filter {
                        // Фильтр для сегодняшних записей (нужно добавить логику дат)
                        it.status == "BOOKED"
                    } ?: emptyList()

                    val totalPatients = dashboardData?.patients?.size ?: 0
                    val doctorRating = dashboardData?.doctor?.rating ?: 0.0

                    div(className = "statistics grid doctor-grid") {
                        doctorStatisticsCard(todayAppointments.size.toString(), "Сегодня", "\uD83D\uDCC5")
                        doctorStatisticsCard(totalPatients.toString(), "Пациенты", "\uD83D\uDC65")
                        doctorStatisticsCard(doctorRating.toString(), "Рейтинг", "⭐")
                    }

                    // Приемы на сегодня
                    div(className = "card block appointment-block") {
                        h4("Приёмы на сегодня", className = "block title")

                        if (todayAppointments.isNotEmpty()) {
                            todayAppointments.forEach { appointment ->
                                div(className = "appointment card") {
                                    span("Приём #${appointment.id}", className = "record title")
                                    span("Статус: ${appointment.status}", className = "record subtitle")
                                }
                            }
                        } else {
                            div(className = "empty-state") {
                                span("Нет приемов на сегодня")
                            }
                        }
                    }

                    // Пациенты
                    h4("Пациенты", className = "block title")
                    div(className = "card block") {
                        patientsContainer = div(className = "records list")
                    }
                }

                scheduleContainer = div {
                    visible = false

                    if (state.isLoading) {
                        div(className = "loading-state") {
                            span("Загрузка расписания...", className = "doctor-patient-condition")
                        }
                        return@div
                    }

                    state.error?.let { errorMessage ->
                        div(className = "error-state") {
                            span("Ошибка: $errorMessage", className = "record subtitle")
                            button("Повторить", className = "btn-primary") {
                                onClick {
                                    userId?.let { state.loadDoctorDashboard(it) }
                                }
                            }
                        }
                        return@div
                    }

                    h1("Расписание", className = "account title")

                    div(className = "card block appointment-block") {
                        h4("Запланированные пациенты", className = "block title")
                        scheduleListContainer = div(className = "records list")
                    }
                }
            }
        }
    }

    state.onUpdate = {
        val dashboard = state.dashboardData

        val updatedDoctorName = dashboard?.user?.let { user ->
            listOfNotNull(user.surname, user.name, user.patronymic)
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" ")
        } ?: initialDoctorName

        val updatedInitials = updatedDoctorName
            .split(' ', '-', '_')
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifBlank { initialDoctorInitials }

        doctorSubtitleSpan.content = dashboard?.doctor?.profession?.takeIf { it.isNotBlank() }
            ?: defaultSpecialty
        doctorNameHeader.content = updatedDoctorName
        avatarContainer.updateAvatar(dashboard?.user?.avatar ?: Session.avatar, updatedInitials)

        renderPatients()
        renderSchedule()
    }

    renderPatients()
    renderSchedule()
}

private fun Container.doctorStatisticsCard(value: String, label: String, icon: String) {
    div(className = "statistics card") {
        span(icon, className = "statistics icon")
        h4(value, className = "statistics value")
        span(label, className = "statistics label")
    }
}