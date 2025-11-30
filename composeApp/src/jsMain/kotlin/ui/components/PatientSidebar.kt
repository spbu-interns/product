package ui.components

import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h3
import io.kvision.html.li
import io.kvision.html.nav
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.html.ul
import io.kvision.toast.Toast
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ui.PatientSection
import ui.Session

private fun initialsFrom(displayName: String): String = displayName
    .split(' ', '-', '_')
    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
    .take(2)
    .joinToString("")
    .ifBlank { "П" }

fun Container.patientSidebar(
    patientId: Long?,
    displayNameState: StateFlow<String> = MutableStateFlow(Session.fullName() ?: Session.email ?: "Пользователь"),
    initialsState: StateFlow<String> = MutableStateFlow(initialsFrom(displayNameState.value)),
    coroutineScope: CoroutineScope? = null,
    active: PatientSection,
    onOverview: () -> Unit,
    onAppointments: () -> Unit,
    onMedicalRecords: () -> Unit,
    onMyRecords: () -> Unit,
    onFindDoctor: () -> Unit,
    onProfile: (() -> Unit),
    onProfileAlreadyOpen: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null
) {
    val displayName = displayNameState.value

    div(className = "sidebar card") {
        val avatar = div(className = "avatar circle") { +(initialsState.value) }
        val nameHeader = h3(displayName, className = "account name")
        patientId?.let { id ->
            p("ID: #$id", className = "account id")
        }

        coroutineScope?.launch {
            displayNameState.collect { updated -> nameHeader.content = updated }
        }
        coroutineScope?.launch {
            initialsState.collect { updated -> avatar.content = updated }
        }

        nav {
            ul(className = "side menu") {
                li(className = "side_item" + if (active == PatientSection.OVERVIEW) " is-active" else "") {
                    span("Обзор"); span("\uD83D\uDC64", className = "side icon")
                    onClick {
                        window.asDynamic().scrollTo(js("({ top: 0, behavior: 'smooth' })"))
                        onOverview()
                    }
                }
                li(className = "side_item" + if (active == PatientSection.APPOINTMENTS) " is-active" else "") {
                    span("Приёмы"); span("\uD83D\uDCC5", className = "side icon")
                    onClick {
                        window.asDynamic().scrollTo(js("({ top: 0, behavior: 'smooth' })"))
                        onAppointments()
                    }
                }
                li(className = "side_item" + if (active == PatientSection.MEDICAL_RECORDS) " is-active" else "") {
                    span("Медкарта"); span("\uD83D\uDCC4", className = "side icon")
                    onClick {
                        window.asDynamic().scrollTo(js("({ top: 0, behavior: 'smooth' })"))
                        onMedicalRecords()
                    }
                }
                li(className = "side_item" + if (active == PatientSection.MY_RECORDS) " is-active" else "") {
                    span("Мои записи"); span("\uD83D\uDCDD", className = "side icon")
                    onClick {
                        window.asDynamic().scrollTo(js("({ top: 0, behavior: 'smooth' })"))
                        onMyRecords()
                    }
                }
                li(className = "side_item" + if (active == PatientSection.EDIT_PROFILE) " is-active" else "") {
                    span("Мой профиль"); span("\uD83D\uDC64", className = "side icon")
                    onClick {
                        window.asDynamic().scrollTo(js("({ top: 0, behavior: 'smooth' })"))
                        if (active == PatientSection.EDIT_PROFILE) {
                            onProfileAlreadyOpen?.invoke()
                                ?: Toast.info("Профиль уже открыт")
                        } else {
                            onProfile()
                        }
                    }
                }
            }
        }

        div(className = "side button")
        button("Найти врача", className = "btn-primary-lg").onClick { onFindDoctor() }
        onLogout?.let {
            button("Выйти", className = "btn-logout-sm").onClick { it() }
        }
    }
}