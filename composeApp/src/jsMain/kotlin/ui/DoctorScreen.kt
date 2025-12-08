package ui

import api.ApiConfig
import api.BookingApiClient
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
import io.kvision.utils.px
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import state.DoctorState
import state.DoctorState.dashboardData
import ui.components.timetableModal
import ui.components.updateAvatar
import utils.normalizeGender
import org.interns.project.dto.SlotResponse
import kotlin.js.Date

private data class DoctorPatientListItem(
    val userId: Long,
    val patientRecordId: Long,
    val name: String,
    val subtitle: String,
    val status: String,
    val initials: String,
)

private data class DoctorAppointmentCard(
    val appointmentId: Long,
    val patientUserId: Long?,
    val patientRecordId: Long?,
    val initials: String,
    val name: String,
    val notes: String,
    val datetime: String,
    val status: String,
    val slotStart: Date,
)

fun Container.doctorScreen(onLogout: () -> Unit = { Navigator.showHome() }) = vPanel(spacing = 12) {
    val uiScope = MainScope()
    val state = DoctorState
    val bookingApiClient = BookingApiClient()
    val timetableController = timetableModal()

    var unsubscribe: (() -> Unit)? = null

    fun cleanup() {
        uiScope.cancel()
        unsubscribe?.invoke()
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
    lateinit var scheduleGridContainer: Container
    lateinit var avatarContainer: Div
    lateinit var statisticsContainer: Container
    lateinit var doctorNameHeader: H4
    lateinit var todayAppointmentsContainer: Container
    lateinit var weekRangeLabel: Span
    var isScheduleUiReady = false

    var weeklySlots: List<SlotResponse> = emptyList()
    var slotsError: String? = null
    var isLoadingSlots = false
    var slotsLoaded = false
    var currentWeekOffset = 0
    var loadedDoctorId: Long? = null

    var renderPatients: () -> Unit = {}
    var renderStatistics: () -> Unit = {}
    var renderTodayAppointments: () -> Unit = {}
    var renderSchedule: () -> Unit = {}

    val millisPerDay = 24 * 60 * 60 * 1000

    fun formatDateTime(date: Date): String {
        val day = date.getDate().toString().padStart(2, '0')
        val month = (date.getMonth() + 1).toString().padStart(2, '0')
        val hours = date.getHours().toString().padStart(2, '0')
        val minutes = date.getMinutes().toString().padStart(2, '0')
        return "$day.$month.${date.getFullYear()} • $hours:$minutes"
    }

    fun Date.isSameDay(other: Date): Boolean =
        getFullYear() == other.getFullYear() &&
                getMonth() == other.getMonth() &&
                getDate() == other.getDate()

    fun Date.toIsoDate(): String {
        val year = getFullYear()
        val month = (getMonth() + 1).toString().padStart(2, '0')
        val day = getDate().toString().padStart(2, '0')
        return "$year-$month-$day"
    }

    fun startOfWeek(date: Date): Date {
        val normalizedDow = if (date.getDay() == 0) 7 else date.getDay()
        val mondayMillis = date.getTime() - (normalizedDow - 1) * millisPerDay
        return Date(mondayMillis)
    }

    fun formatWeekRangeLabel(anchor: Date): String {
        val end = Date(anchor.getTime() + 6 * millisPerDay)
        fun format(date: Date): String {
            val day = date.getDate().toString().padStart(2, '0')
            val month = (date.getMonth() + 1).toString().padStart(2, '0')
            return "$day.$month"
        }

        return "${format(anchor)} — ${format(end)}"
    }

    fun buildTodayAppointments(): List<DoctorAppointmentCard> {
        val dashboard = state.dashboardData ?: return emptyList()
        val slotsById = state.todaysSlots.associateBy { it.id }
        val patients = dashboard.patients.associateBy { it.clientId }
        val today = Date()

        return dashboard.appointments.mapNotNull { appointment ->
            val slot = slotsById[appointment.slotId] ?: return@mapNotNull null
            val slotStart = Date(slot.startTime)
            if (!slotStart.isSameDay(today)) return@mapNotNull null
            if (!appointment.status.equals("BOOKED", ignoreCase = true)) return@mapNotNull null

            val patient = patients[appointment.clientId]
            val name = listOfNotNull(patient?.surname, patient?.name, patient?.patronymic)
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" ")
                ?: "Пациент #${appointment.clientId}"

            val initials = name.split(' ', '-', '_')
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
                .ifBlank { patient?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "ПЦ" }

            DoctorAppointmentCard(
                appointmentId = appointment.id,
                patientUserId = patient?.userId,
                patientRecordId = patient?.clientId,
                initials = initials,
                name = name,
                notes = appointment.comments?.ifBlank { null } ?: "Комментариев нет",
                datetime = formatDateTime(slotStart),
                status = appointment.status,
                slotStart = slotStart,
            )
        }.sortedBy { it.slotStart.getTime() }
    }

    fun loadWeeklySlots(force: Boolean = false) {
        val doctorId = state.dashboardData?.doctor?.id ?: return
        if (loadedDoctorId != doctorId) {
            slotsLoaded = false
            weeklySlots = emptyList()
        }
        if (isLoadingSlots || (!force && slotsLoaded)) return

        isLoadingSlots = true
        slotsError = null
        renderSchedule()

        uiScope.launch {
            val response = bookingApiClient.listDoctorSlots(doctorId)
            response.onSuccess {
                weeklySlots = it.sortedBy { slot -> Date(slot.startTime).getTime() }
                slotsLoaded = true
                loadedDoctorId = doctorId
            }.onFailure {
                slotsError = it.message ?: "Не удалось загрузить слоты"
                weeklySlots = emptyList()
                slotsLoaded = false
            }

            isLoadingSlots = false
            renderSchedule()
        }
    }

    fun formatSlotLabel(slot: SlotResponse): String {
        val start = Date(slot.startTime)
        val end = Date(slot.endTime)
        val startTime = "${start.getHours().toString().padStart(2, '0')}:${start.getMinutes().toString().padStart(2, '0')}"
        val endTime = "${end.getHours().toString().padStart(2, '0')}:${end.getMinutes().toString().padStart(2, '0')}"
        return "$startTime — $endTime"
    }

    fun DoctorPatientListItem.render() {
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

    fun Container.renderAppointmentCard(card: DoctorAppointmentCard) {
        div(className = "appointment card full") {
            marginBottom = 12.px

            div(className = "appointment row") {
                div(className = "appointment avatar colored") { +card.initials }

                div(className = "appointment info") {
                    span(card.name, className = "appointment doctor")
                    span(card.notes, className = "appointment appointment-specialty")

                    div(className = "appointment meta") {
                        span("📅 ${card.datetime}")
                        span("• ${card.status}", className = "appointment status")
                    }
                }
            }

            onClick {
                val patientId = card.patientUserId
                val recordId = card.patientRecordId
                if (patientId != null && recordId != null) {
                    cleanup()
                    Navigator.showDoctorPatient(patientId, recordId)
                } else {
                    Toast.info("Данные пациента недоступны")
                }
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

    renderStatistics = fun() {
        statisticsContainer.removeAll()

        val todayCount = buildTodayAppointments().size
        val totalPatients = state.dashboardData?.patients?.size ?: 0
        val doctorRating = state.dashboardData?.doctor?.rating ?: 0.0

        statisticsContainer.apply {
            doctorStatisticsCard(todayCount.toString(), "Сегодня", "\uD83D\uDCC5")
            doctorStatisticsCard(totalPatients.toString(), "Пациенты", "\uD83D\uDC65")
            doctorStatisticsCard(doctorRating.toString(), "Рейтинг", "⭐")
        }
    }

    renderTodayAppointments = fun() {
        todayAppointmentsContainer.removeAll()

        val todayAppointments = buildTodayAppointments()

        when {
            state.isLoading -> {
                todayAppointmentsContainer.div(className = "doctor-empty-state") {
                    span("Загрузка расписания...", className = "doctor-patient-condition")
                }
            }

            state.error != null -> {
                todayAppointmentsContainer.div(className = "record item") {
                    span(state.error ?: "Не удалось загрузить расписание", className = "record subtitle")
                    button("Повторить", className = "btn-ghost-sm").onClick {
                        userId?.let { state.loadDoctorDashboard(it) }
                    }
                }
            }

            todayAppointments.isEmpty() -> {
                todayAppointmentsContainer.div(className = "empty-state") {
                    span("Нет приемов на сегодня")
                }
            }

            else -> {
                todayAppointments.forEach { appointment ->
                    todayAppointmentsContainer.renderAppointmentCard(appointment)
                }
            }
        }
    }

    renderSchedule = fun() {
        if (!isScheduleUiReady) return
        scheduleGridContainer.removeAll()

        if (state.dashboardData == null && state.isLoading) {
            scheduleGridContainer.div(className = "doctor-empty-state") {
                span("Загрузка расписания...", className = "doctor-patient-condition")
            }
            return
        }

        val doctorId = state.dashboardData?.doctor?.id
        if (doctorId == null) {
            scheduleGridContainer.div(className = "doctor-empty-state") {
                span("Не удалось определить врача", className = "record subtitle")
            }
            return
        }

        state.error?.let { errorMessage ->
            scheduleGridContainer.div(className = "record item") {
                span(errorMessage, className = "record subtitle")
                button("Обновить", className = "btn-ghost-sm").onClick {
                    userId?.let { id -> state.loadDoctorDashboard(id) }
                }
            }
            return
        }

        if (!slotsLoaded && !isLoadingSlots) {
            loadWeeklySlots()
        }

        val anchorMonday = startOfWeek(Date(Date().getTime() + currentWeekOffset * 7 * millisPerDay))
        weekRangeLabel.content = formatWeekRangeLabel(anchorMonday)

        when {
            state.isLoading || isLoadingSlots -> {
                scheduleGridContainer.div(className = "doctor-empty-state") {
                    span("Загрузка расписания...", className = "doctor-patient-condition")
                }
                return
            }

            slotsError != null -> {
                scheduleGridContainer.div(className = "record item") {
                    span(slotsError ?: "Не удалось загрузить слоты", className = "record subtitle")
                    button("Обновить", className = "btn-ghost-sm").onClick {
                        loadWeeklySlots(force = true)
                    }
                }
                return
            }
        }

        val appointmentsBySlot = state.dashboardData?.appointments?.associateBy { it.slotId } ?: emptyMap()
        val patientsById = state.dashboardData?.patients?.associateBy { it.clientId } ?: emptyMap()

        val weekdays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        val weekDays = (0 until 7).map { shift ->
            Date(anchorMonday.getTime() + shift * millisPerDay)
        }

        val today = Date()
        val weekGrid = scheduleGridContainer.div(className = "schedule-week-grid") {}

        weekDays.forEach { day ->
            val iso = day.toIsoDate()

            val dowIndex = (if (day.getDay() == 0) 7 else day.getDay()) - 1
            val dowLabel = weekdays[dowIndex] // "Пн", "Вт", ...
            val dateLabel = "${day.getDate().toString().padStart(2, '0')}." +
                    (day.getMonth() + 1).toString().padStart(2, '0')

            val daySlots = weeklySlots
                .filter { it.startTime.startsWith(iso) }
                .sortedBy { Date(it.startTime).getTime() }

            val dayCardClasses = buildString {
                append("schedule-day-card card")
                if (day.isSameDay(today)) append(" is-today")
            }

            weekGrid.div(className = dayCardClasses) {
                // шапка дня
                div(className = "schedule-day-header") {
                    span(dowLabel, className = "schedule-day-dow")
                    span(dateLabel, className = "schedule-day-date")
                }

                // слоты
                div(className = "schedule-day-body") {
                    if (daySlots.isEmpty()) {
                        span("Нет слотов", className = "schedule-day-empty")
                    } else {
                        daySlots.forEach { slot ->
                            val appointment = appointmentsBySlot[slot.id]
                            val patient = appointment?.let { patientsById[it.clientId] }
                            val booked = appointment != null || slot.isBooked
                            val slotClass = "schedule-slot" + if (booked) " is-booked" else " is-free"
                            val statusLabel = if (booked) {
                                val patientName = listOfNotNull(patient?.surname, patient?.name, patient?.patronymic)
                                    .takeIf { it.isNotEmpty() }
                                    ?.joinToString(" ")
                                    ?: "Бронь подтверждена"
                                "Занято — $patientName"
                            } else {
                                "Свободно для брони"
                            }

                            div(className = slotClass) {
                                span(formatSlotLabel(slot), className = "schedule-slot-time")
                                span(statusLabel, className = "schedule-slot-status")
                            }.onClick {
                                when {
                                    appointment != null && patient != null -> {
                                        cleanup()
                                        Navigator.showDoctorPatient(patient.userId, patient.clientId)
                                    }

                                    appointment != null -> Toast.info("Нет данных пациента по этой брони")
                                    else -> timetableController.open(doctorNameHeader.content.toString(), doctorId)
                                }
                            }
                        }
                    }
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
                    statisticsContainer = div(className = "statistics grid doctor-grid")
                    renderStatistics()

                    // Приемы на сегодня
                    div(className = "card block appointment-block") {
                        h4("Приёмы на сегодня", className = "block title")
                        todayAppointmentsContainer = div()
                        renderTodayAppointments()
                    }

                    // Пациенты
                    h4("Пациенты", className = "block title")
                    div(className = "card block") {
                        patientsContainer = div(className = "records list")
                    }
                }

                scheduleContainer = div {
                    visible = false
                    h1("Расписание", className = "account title")

                    div(className = "card block appointment-block") {
                        div(className = "schedule-toolbar") {
                            button("◀ Предыдущая неделя", className = "btn-ghost-sm").onClick {
                                currentWeekOffset -= 1
                                renderSchedule()
                            }

                            weekRangeLabel = span("", className = "schedule-week-range")

                            button("Следующая неделя ▶", className = "btn-ghost-sm").onClick {
                                currentWeekOffset += 1
                                renderSchedule()
                            }

                            button("Редактировать расписание", className = "btn-secondary-lg timetable-trigger").onClick {
                                timetableController.open(doctorNameHeader.content.toString(), state.dashboardData?.doctor?.id)
                            }
                        }

                        scheduleGridContainer = div(className = "records list schedule-grid-wrapper")
                        isScheduleUiReady = true
                    }
                }
            }
        }
    }

    unsubscribe = state.subscribe {
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
        renderStatistics()
        renderTodayAppointments()
        renderSchedule()
    }

    renderPatients()
    renderStatistics()
    renderTodayAppointments()
    renderSchedule()
}

private fun Container.doctorStatisticsCard(value: String, label: String, icon: String) {
    div(className = "statistics card") {
        span(icon, className = "statistics icon")
        h4(value, className = "statistics value")
        span(label, className = "statistics label")
    }
}