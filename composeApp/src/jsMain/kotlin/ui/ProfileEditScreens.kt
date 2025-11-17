package ui

import api.ApiConfig
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.form.select.select
import io.kvision.form.text.text
import io.kvision.form.time.dateTime
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.h3
import io.kvision.html.p
import io.kvision.panel.vPanel
import io.kvision.toast.Toast

fun Container.patientProfileEditScreen(onBack: () -> Unit = { Navigator.showPatient() }) {
    profileEditScreenCommon(
        mode = HeaderMode.PATIENT,
        title = "Edit patient profile",
        onBack = onBack
    )
}

fun Container.doctorProfileEditScreen(onBack: () -> Unit = { Navigator.showDoctor() }) {
    profileEditScreenCommon(
        mode = HeaderMode.DOCTOR,
        title = "Edit doctor profile",
        onBack = onBack
    )
}

private fun Container.profileEditScreenCommon(
    mode: HeaderMode,
    title: String,
    onBack: () -> Unit
) {
    headerBar(
        mode = mode,
        active = NavTab.NONE,
        onLogout = {
            ApiConfig.clearToken()
            Session.clear()
            Navigator.showHome()
        }
    )

    val displayName = Session.fullName ?: Session.email ?: "User"
    val userIdText = Session.userId?.let { "ID: $it" } ?: ""
    val initials = displayName
        .split(' ', '-', '_')
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifBlank { "US" }

    div(className = "account container") {
        div(className = "account grid") {
            // Sidebar
            div(className = "sidebar card") {
                div(className = "avatar circle") { +initials }
                h3(displayName, className = "account name")
                if (userIdText.isNotBlank()) {
                    p(userIdText, className = "account id")
                }
                // Кнопки "Back" нет — только Cancel в форме
            }

            // Main column – edit form
            div(className = "main column") {
                h1(title, className = "account title")

                div(className = "card block") {
                    vPanel(spacing = 16) {
                        val firstNameField = text(label = "First name") {
                            value = Session.firstName ?: ""
                        }
                        val lastNameField = text(label = "Last name") {
                            value = Session.lastName ?: ""
                        }
                        val patronymicField = text(label = "Middle name") { }

                        val birthDateField = dateTime(
                            format = "DD.MM.YYYY",
                            label = "Birth date"
                        ) {
                            placeholder = "Select birth date"
                            // showTodayButton убрали, чтобы не было Unresolved reference
                            showClear = true
                        }

                        val phoneField = text(label = "Phone number") { }

                        val avatarField = text(label = "Avatar URL") {
                            placeholder = "https://example.com/avatar.jpg"
                        }

                        val genderField = select(
                            options = listOf(
                                "M" to "M",
                                "F" to "F"
                            ),
                            label = "Gender (M/F)"
                        ) {
                            placeholder = "Select gender"
                        }

                        val statusField = select(
                            options = listOf(
                                "ACTIVE" to "Active",
                                "INACTIVE" to "Inactive"
                            ),
                            label = "Active status"
                        ) {
                            placeholder = "Select status"
                        }

                        div(className = "side button")

                        button("Save", className = "btn-primary-lg").onClick {
                            // TODO: сюда подключить реальный вызов API
                            console.log(
                                "Profile edit: " +
                                        "firstName=${firstNameField.value}, " +
                                        "lastName=${lastNameField.value}, " +
                                        "patronymic=${patronymicField.value}, " +
                                        "birthDate=${birthDateField.getValueAsString()}, " +
                                        "phone=${phoneField.value}, " +
                                        "avatar=${avatarField.value}, " +
                                        "gender=${genderField.value}, " +
                                        "status=${statusField.value}"
                            )
                            Toast.success("Profile saved (frontend only for now)")
                            onBack()
                        }

                        button("Cancel", className = "btn-ghost-sm").onClick {
                            onBack()
                        }
                    }
                }
            }
        }
    }
}