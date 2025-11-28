package ui

import api.ApiConfig
import api.ProfileApiClient
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.core.onClickLaunch
import io.kvision.form.check.checkBox
import io.kvision.form.select.select
import io.kvision.form.text.text
import io.kvision.form.time.dateTime
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.h3
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.panel.vPanel
import io.kvision.toast.Toast
import kotlinx.coroutines.launch
import org.interns.project.dto.ProfileUpdateDto
import ui.components.SidebarTab
import ui.components.patientSidebar
import kotlin.js.Date

val profileApi = ProfileApiClient()
fun Container.patientProfileEditScreen(onBack: () -> Unit = { Navigator.showPatient() }) {
    profileEditScreenCommon(
        mode = HeaderMode.PATIENT,
        title = "Редактирование профиля пациента",
        onBack = onBack
    )
}

fun Container.doctorProfileEditScreen(onBack: () -> Unit = { Navigator.showDoctor() }) {
    profileEditScreenCommon(
        mode = HeaderMode.DOCTOR,
        title = "Редактирование профиля врача",
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

    val displayName = Session.fullName() ?: Session.email ?: "Пользователь"
    val userIdText = Session.userId?.let { "ID: $it" } ?: ""
    val initials = displayName
        .split(' ', '-', '_')
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifBlank { "ПС" }

    div(className = "account container") {
        div(className = "account grid") {
            // Sidebar
            when (mode) {
                HeaderMode.PATIENT -> {
                    val patientId = Session.userId
                    if (patientId != null) {
                        patientSidebar(
                            patientId = patientId,
                            active = SidebarTab.PROFILE,
                            onOverview = { Navigator.showPatient() },
                            onAppointments = { Navigator.showAppointments() },
                            onMedicalRecords = { Navigator.showStub("Раздел медицинской карты находится в разработке") },
                            onMyRecords = { Navigator.showMyRecords() },
                            onFindDoctor = { Navigator.showFind() },
                            onProfile = { Navigator.showPatientProfileEdit() }
                        )
                    } else {
                        div(className = "sidebar card") {
                            div(className = "avatar circle") { +initials }
                            h3(displayName, className = "account name")
                        }
                    }
                }

                HeaderMode.DOCTOR -> {
                    div(className = "sidebar card") {
                        div(className = "avatar circle") { +initials }
                        h3(displayName, className = "account name")
                        if (userIdText.isNotBlank()) {
                            p(userIdText, className = "account id")
                        }
                    }
                }

                HeaderMode.PUBLIC -> {
                    div(className = "sidebar card") {
                        div(className = "avatar circle") { +initials }
                        h3(displayName, className = "account name")
                    }
                }
            }

            // Main column – edit form
            div(className = "main column") {
                h1(title, className = "account title")

                div(className = "card block") {
                    vPanel(spacing = 16) {
                        val firstNameField = text(label = "Имя") {
                            value = Session.firstName ?: ""
                        }
                        val lastNameField = text(label = "Фамилия") {
                            value = Session.lastName ?: ""
                        }
                        val patronymicField = text(label = "Отчество") {
                            value = Session.patronymic ?: ""
                        }
                        val noPatronymicCheck = checkBox(label = "Нет отчества") {
                            value = Session.patronymic.isNullOrBlank() && Session.hasNoPatronymic
                        }

                        fun toJsDate(raw: String?): Date? {
                            if (raw.isNullOrBlank()) return null
                            return runCatching { Date("${raw}T00:00:00") }.getOrNull()
                        }

                        fun toIsoDate(value: String?): String? {
                            if (value.isNullOrBlank()) return null
                            val cleaned = value.substringBefore("T")
                            return when {
                                cleaned.contains(".") -> cleaned.split(".").takeIf { it.size == 3 }
                                    ?.let { (day, month, year) -> "$year-$month-$day" }
                                cleaned.contains("-") -> cleaned
                                else -> null
                            }
                        }

                        val birthDateField = dateTime(
                            format = "DD.MM.YYYY",
                            label = "Дата рождения"
                        ) {
                            placeholder = "Выберите дату рождения"
                            showClear = false
                            value = toJsDate(Session.dateOfBirth)
                        }

                        val phoneField = text(label = "Номер телефона") {
                            placeholder = "+7 (XXX) XXX-XX-XX"
                            value = Session.phoneNumber ?: ""
                        }

                        val avatarField = text(label = "Ссылка на ваше фото") {
                            placeholder = "https://example.com/avatar.jpg"
                            value = Session.avatar ?: ""
                        }

                        val genderField = select(
                            options = listOf(
                                "M" to "Мужской",
                                "F" to "Женский"
                            ),
                            label = "Пол"
                        ) {
                            placeholder = "Выберите пол"
                            value = Session.gender
                        }

                        val errorText = span("").apply { addCssClass("text-danger") }

                        div(className = "side button")

                        button("Сохранить", className = "btn-primary-lg").onClickLaunch {

                            val first = firstNameField.value?.trim().orEmpty()
                            val last = lastNameField.value?.trim().orEmpty()
                            val patronymic = patronymicField.value?.trim().orEmpty()
                            val noPatronymic = noPatronymicCheck.value
                            val isoBirthDate = toIsoDate(birthDateField.getValueAsString())

                            when {
                                first.isBlank() -> {
                                    errorText.content = "Укажите имя"
                                    return@onClickLaunch
                                }

                                last.isBlank() -> {
                                    errorText.content = "Укажите фамилию"
                                    return@onClickLaunch
                                }

                                isoBirthDate.isNullOrBlank() -> {
                                    errorText.content = "Выберите дату рождения"
                                    return@onClickLaunch
                                }

                                patronymic.isBlank() && !noPatronymic -> {
                                    errorText.content = "Заполните отчество или отметьте, что его нет"
                                    return@onClickLaunch
                                }
                            }

                            errorText.content = ""
                            val userId = Session.userId ?: run {
                                errorText.content = "Не удалось определить пользователя"
                                return@onClickLaunch
                            }

                            val dto = ProfileUpdateDto(
                                firstName = first,
                                lastName = last,
                                patronymic = patronymic.takeIf { it.isNotBlank() && !noPatronymic },
                                phoneNumber = phoneField.value,
                                avatar = avatarField.value,
                                gender = genderField.value,
                                dateOfBirth = isoBirthDate,
                                bloodType = null,
                                height = null,
                                weight = null,
                                emergencyContactName = null,
                                emergencyContactNumber = null,
                                address = null,
                                snils = null,
                                passport = null,
                                dmsOms = null,
                                profession = null,
                                info = null,
                                experience = null,
                                price = null
                            )

                            val result = profileApi.updateProfile(userId, dto)

                            result.onSuccess { updated ->
                                Session.hasNoPatronymic = noPatronymic
                                Session.updateFrom(updated)
                                onBack()
                            }

                            result.onFailure {
                                val message = it.message ?: "Ошибка обновления профиля"
                                errorText.content = message
                                Toast.danger("Не удалось сохранить профиль: $message")
                            }
                        }


                        button("Отмена", className = "btn-ghost-sm").onClick {
                            onBack()
                        }
                    }
                }
            }
        }
    }
}