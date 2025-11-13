package ui

import api.ApiConfig
import api.PatientApiClient
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.html.Span
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
import io.kvision.toast.Toast
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private data class DoctorPatientListItem(
    val userId: Long,
    val patientRecordId: Long,
    val name: String,
    val subtitle: String,
    val status: String,
    val initials: String,
)

fun Container.doctorScreen(onLogout: () -> Unit = { Navigator.showHome() }) = vPanel(spacing = 12) {
    val uiScope = MainScope()
    val apiClient = PatientApiClient()

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
    var patientCountPill: Span? = null
    var patientMetricValue: Span? = null

    var renderPatients: () -> Unit = {}
    var loadPatients: (Boolean) -> Unit = {}

    fun DoctorPatientListItem.render() {
        val userId = this@render.userId
        val recordId = this@render.patientRecordId
        patientsContainer.div(className = "doctor-patient-item") {
            div(className = "doctor-patient-avatar") { +initials }
            div(className = "doctor-patient-info") {
                span(name, className = "doctor-patient-name")
                span(subtitle, className = "doctor-patient-condition")
            }
            span(status, className = "status info")
            onClick {
                cleanup()
                Navigator.showDoctorPatient(userId, recordId)
            }
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
                patientsContainer.div(className = "doctor-empty-state") {
                    span(patientsError ?: "Ошибка", className = "doctor-patient-condition")
                    button("Повторить", className = "btn-ghost-sm").onClick {
                        patientsError = null
                        loadPatients(true)
                    }
                }
            }
            patients.isEmpty() -> {
                patientsContainer.div(className = "doctor-empty-state") {
                    span("Пациенты не найдены", className = "doctor-patient-condition")
                    button("Обновить", className = "btn-ghost-sm").onClick {
                        loadPatients(true)
                    }
                }
            }
            else -> patients.forEach { it.render() }
        }

        patientCountPill?.content = "Пациенты в базе: ${patients.size}"
        patientMetricValue?.content = patients.size.toString()
    }

    loadPatients = fun(force: Boolean) {
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
                    val enriched = mutableListOf<DoctorPatientListItem>()
                    list.forEach { user ->
                        val client = apiClient.getClientProfile(user.id).getOrElse { error ->
                            println("Failed to load client profile for user ${user.id}: ${error.message}")
                            null
                        }
                        val recordId = client?.id ?: return@forEach
                        val displayName = listOfNotNull(user.firstName ?: user.name, user.lastName ?: user.surname)
                            .joinToString(" ")
                            .ifBlank { user.login }
                        val initials = displayName
                            .split(' ', '-', '_')
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .take(2)
                            .joinToString("")
                            .ifBlank { user.login.firstOrNull()?.uppercaseChar()?.toString() ?: "PT" }
                        val subtitle = user.email
                        val status = if (user.isActive) "Active" else "Inactive"
                        enriched += DoctorPatientListItem(
                            userId = user.id,
                            patientRecordId = recordId,
                            name = displayName,
                            subtitle = subtitle,
                            status = status,
                            initials = initials,
                        )
                    }
                    patients.clear()
                    patients.addAll(enriched.sortedBy { it.name.lowercase() })
                    patientsError = if (enriched.isEmpty() && list.isNotEmpty()) {
                        "Пациенты с заполненным профилем не найдены"
                    } else {
                        null
                    }
                },
                onFailure = { error ->
                    patientsError = error.message ?: "Не удалось загрузить пациентов"
                    Toast.danger(patientsError ?: "Ошибка")
                }
            )
            isLoadingPatients = false
            renderPatients()
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

    div(className = "doctor container") {
        div(className = "doctor grid") {
            div(className = "sidebar card doctor-sidebar") {
                div(className = "avatar circle doctor-avatar") { +doctorInitials }
                h3(doctorName, className = "account name")
                if (doctorSubtitle.isNotBlank()) {
                    span(doctorSubtitle, className = "doctor-specialty")
                }

                div(className = "doctor-tags") {
                    Session.userId?.let { span("ID: #$it", className = "doctor-tag") }
                    span("Role: ${Session.accountType ?: "DOCTOR"}", className = "doctor-tag")
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
                            onClick {
                                if (patients.isNotEmpty()) {
                                    cleanup()
                                    val first = patients.first()
                                    Navigator.showDoctorPatient(first.userId, first.patientRecordId)
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
            }

            div(className = "doctor-main column") {
                div(className = "doctor-header") {
                    div {
                        h1("Dashboard Overview", className = "doctor-title")
                        span("Сегодня: ${js("new Date()").unsafeCast<dynamic>().toLocaleDateString()}", className = "doctor-date")
                    }
                    div(className = "doctor-status") {
                        patientCountPill = span("Пациенты в базе: ${patients.size}", className = "doctor-status-pill")
                    }
                }

                div(className = "doctor-metrics grid") {
                    doctorMetric("4", "Today", "Appointments")
                    patientMetricValue = doctorMetric(patients.size.toString(), "Patients", "Active in Care")
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
                            h4("Patients from database", className = "block title")
                            patientsContainer = div(className = "doctor-patient-list")
                        }

                        div(className = "card block doctor-week-summary") {
                            h4("This Week", className = "block title")
                            div(className = "doctor-week-list") {
                                doctorWeekStat("Total Appointments", "18")
                                doctorWeekStat("New Patients", patients.size.toString())
                                doctorWeekStat("Follow-ups", "7")
                                doctorWeekStat("Consultations", "8")
                            }
                        }
                    }
                }
            }
        }
    }

    loadPatients(false)
    renderPatients()
}

private fun Container.doctorMetric(value: String, label: String, subtitle: String): Span {
    lateinit var valueSpan: Span
    div(className = "doctor-metric card") {
        hPanel(className = "doctor-metric-header") {
            valueSpan = span(value, className = "doctor-metric-value")
            span(label, className = "doctor-metric-label")
        }
        span(subtitle, className = "doctor-metric-subtitle")
    }
    return valueSpan
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

private fun Container.doctorWeekStat(label: String, value: String) {
    div(className = "doctor-week-item") {
        span(label, className = "doctor-week-label")
        span(value, className = "doctor-week-value")
    }
}