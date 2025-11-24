package ui

import api.ApiConfig
import api.PatientApiClient
import io.kvision.core.Container
import io.kvision.core.onClick
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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ui.components.timetableModal

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
    val apiClient = PatientApiClient()
    val timetableController = timetableModal()

    fun cleanup() {
        uiScope.cancel()
    }

    val doctorName = Session.fullName ?: Session.email ?: "Doctor"
    val doctorInitials = doctorName
        .split(' ', '-', '_')
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifBlank { Session.email?.firstOrNull()?.uppercaseChar()?.toString() ?: "DR" }
    val doctorSubtitle = Session.email ?: ""

    val patients = mutableListOf<DoctorPatientListItem>()
    var patientsLoaded = false
    var isLoadingPatients = false
    var patientsError: String? = null

    lateinit var patientsContainer: Container
    lateinit var overviewContainer: Container
    lateinit var scheduleContainer: Container
    lateinit var scheduleListContainer: Container

    var renderPatients: () -> Unit = {}
    var renderSchedule: () -> Unit = {}

    val appointments = listOf(
        DoctorAppointmentCard("JS", "John Smith", "09:00 AM · Consultation · 30 min", "confirmed"),
        DoctorAppointmentCard("SW", "Sarah Wilson", "10:30 AM · Routine Check-up · 15 min", "confirmed"),
        DoctorAppointmentCard("MJ", "Mike Johnson", "01:00 PM · Follow-up · 20 min", "pending"),
        DoctorAppointmentCard("ED", "Emma Davis", "03:30 PM · Consultation · 45 min", "confirmed"),
    )

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
                Navigator.showDoctorPatient(userId)
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

            span(item.status, className = statusClass)

            onClick {
                cleanup()
                Navigator.showDoctorPatient(item.userId)
            }
        }
    }

    fun addPatientItem(userId: Long, recordId: Long?, name: String?, note: String?) {
        val initials = name
            ?.split(' ', '-', '_')
            ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
            ?.take(2)
            ?.joinToString("")
            ?.ifBlank { "PT" }
            ?: "PT"

        patients.add(
            DoctorPatientListItem(
                userId = userId,
                patientRecordId = recordId ?: 0,
                name = name ?: "Patient #$userId",
                subtitle = note ?: "Подробнее...",
                status = "active",
                initials = initials,
            )
        )
    }

    fun loadPatients(force: Boolean) {
        if (isLoadingPatients) return
        if (patientsLoaded && !force) return

        isLoadingPatients = true
        patientsError = null
        renderPatients()

        uiScope.launch {
            val result = apiClient.listPatients()
            result.fold(
                onSuccess = { list ->
                    patientsLoaded = true
                    patients.clear()
                    list.forEach { patient ->
                        val p = patient.asDynamic()
                        val user = p.user

                        val userId = (user.id as Number).toLong()
                        val recordId = (p.id as Number?)?.toLong()

                        val name = buildString {
                            val firstName = user.firstName as String?
                            val lastName = user.lastName as String?

                            val nameParts = listOfNotNull<String>(firstName, lastName)

                            if (nameParts.isNotEmpty()) {
                                append(nameParts.joinToString(" "))
                            } else {
                                append(user.login as String)
                            }
                        }

                        val email = user.email as String?

                        addPatientItem(
                            userId = userId,
                            recordId = recordId,
                            name = name,
                            note = email,
                        )
                    }
                },
                onFailure = { error ->
                    patientsError = error.message ?: "Не удалось загрузить пациентов"
                    Toast.danger(patientsError ?: "Ошибка загрузки")
                }
            )
            isLoadingPatients = false
            renderPatients()
            renderSchedule()
        }
    }

    renderPatients = fun() {
        patientsContainer.removeAll()
        when {
            isLoadingPatients -> {
                patientsContainer.div(className = "doctor-empty-state") {
                    span("Загрузка пациентов...", className = "doctor-patient-condition")
                }
            }
            patientsError != null -> {
                patientsContainer.div(className = "record item") {
                    span("Загрузка пациентов...", className = "record subtitle")
                    button("Повторить", className = "btn-ghost-sm").onClick {
                        patientsError = null
                        loadPatients(true)
                    }
                }
            }
            patients.isEmpty() -> {
                patientsContainer.div(className = "record item") {
                    span("Пациенты не найдены", className = "record subtitle")
                    button("Обновить", className = "btn-ghost-sm").onClick {
                        loadPatients(true)
                    }
                }
            }
            else -> patients.forEach { it.render() }
        }
    }

    renderSchedule = fun() {
        scheduleListContainer.removeAll()

        when {
            isLoadingPatients -> {
                scheduleListContainer.div(className = "doctor-empty-state") {
                    span("Загрузка пациентов...", className = "doctor-patient-condition")
                }
            }

            patientsError != null -> {
                scheduleListContainer.div(className = "record item") {
                    span(patientsError ?: "Не удалось загрузить пациентов", className = "record subtitle")
                    button("Повторить", className = "btn-ghost-sm").onClick {
                        patientsError = null
                        loadPatients(true)
                    }
                }
            }

            patients.isEmpty() -> {
                scheduleListContainer.div(className = "record item") {
                    span("Пока нет записанных пациентов", className = "record subtitle")
                }
            }

            else -> {
                patients.forEach { patient ->
                    scheduleListContainer.renderSchedulePatient(patient)
                }
            }
        }
    }

    headerBar(
        mode = HeaderMode.DOCTOR,
        active = NavTab.NONE,
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
                div(className = "avatar circle") { +doctorInitials }
                h4(doctorName, className = "account name")
                span("Role: DOCTOR", className = "account id")
                if (doctorSubtitle.isNotBlank()) {
                    span(doctorSubtitle, className = "account id")
                }

                nav {
                    ul(className = "side menu") {
                        li(className = "side_item is-active") {
                            span("Overview")
                            span("\uD83D\uDC64", className = "side icon")
                            onClick {
                                overviewContainer.visible = true
                                scheduleContainer.visible = false
                            }
                        }
                        li(className = "side_item") {
                            span("Schedule")
                            span("\uD83D\uDCC5", className = "side icon")
                            onClick {
                                overviewContainer.visible = false
                                scheduleContainer.visible = true
                                renderSchedule()
                            }
                        }
                        li(className = "side_item") {
                            span("Patients")
                            span("\uD83D\uDC65", className = "side icon")
                            onClick {
                                if (patients.isNotEmpty()) {
                                    cleanup()
                                    val first = patients.first()
                                    Navigator.showDoctorPatient(first.userId)
                                } else {
                                    loadPatients(true)
                                }
                            }
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
                button("Timetable", className = "btn-secondary-lg timetable-trigger").onClick {
                    timetableController.open(doctorName)
                }
            }

            div(className = "main column") {

                overviewContainer = div {
                    h1("Dashboard Overview", className = "account title")

                    div(className = "statistics grid") {
                        doctorStatisticsCard("4", "Today", "\uD83D\uDCC5")
                        doctorStatisticsCard(patients.size.toString(), "Patients", "\uD83D\uDC65")
                        doctorStatisticsCard("4.9", "Rating", "⭐")
                    }

                    div(className = "card block appointment-block") {
                        h4("Today's Appointments", className = "block title")
                        appointments.forEach { appointment ->
                            doctorAppointmentCard(appointment)
                        }
                    }

                    h4("Patients from database", className = "block title")
                    div(className = "card block") {
                        patientsContainer = div(className = "records list")
                    }
                }

                scheduleContainer = div {
                    visible = false

                    h1("Schedule", className = "account title")

                    div(className = "card block appointment-block") {
                        h4("Scheduled Patients", className = "block title")
                        scheduleListContainer = div(className = "records list")
                    }
                }
            }
        }
    }

    loadPatients(false)
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

private fun Container.doctorAppointmentCard(appointment: DoctorAppointmentCard) {
    val statusClass = when (appointment.status.lowercase()) {
        "confirmed" -> "status success"
        "pending" -> "status info"
        "cancelled" -> "status neutral"
        else -> "status neutral"
    }

    div(className = "appointment card") {
        div(className = "appointment row") {
            div(className = "appointment avatar") { +appointment.initials }
            div(className = "appointment info") {
                span(appointment.name, className = "appointment doctor")
                span(appointment.notes, className = "appointment meta")
            }
            div(className = "appointment actions") {
                span(appointment.status, className = statusClass)
            }
        }
    }
}
