package ui

import api.ApiConfig
import api.ProfileApiClient
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.core.onClickLaunch
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
import kotlinx.coroutines.launch
import org.interns.project.dto.ProfileUpdateDto

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
                        val firstNameField = text(label = "Имя") {
                            value = Session.firstName ?: ""
                        }
                        val lastNameField = text(label = "Фамилия") {
                            value = Session.lastName ?: ""
                        }
                        val patronymicField = text(label = "Отчество") { }

                        val birthDateField = dateTime(
                            format = "DD.MM.YYYY",
                            label = "Дата рождения"
                        ) {
                            placeholder = "Выберите дату рождения"
                            showClear = true
                        }

                        val phoneField = text(label = "Номер телефона") {
                            placeholder = "+7 (XXX) XXX-XX-XX"
                        }

                        val avatarField = text(label = "Ссылка на аватар") {
                            placeholder = "https://example.com/avatar.jpg"
                        }

                        val genderField = select(
                            options = listOf(
                                "M" to "Мужской",
                                "F" to "Женский"
                            ),
                            label = "Пол"
                        ) {
                            placeholder = "Выберите пол"
                        }
                        fun convertDate(value: String?): String? {
                            if (value.isNullOrBlank()) return null
                            val parts = value.split(".")
                            if (parts.size != 3) return null
                            val (day, month, year) = parts
                            return "$year-$month-$day"
                        }

                        val statusField = select(
                            options = listOf(
                                "ACTIVE" to "Активный",
                                "INACTIVE" to "Неактивный"
                            ),
                            label = "Статус"
                        ) {
                            placeholder = "Выберите статус"
                        }

                        div(className = "side button")

                        button("Сохранить", className = "btn-primary-lg").onClickLaunch {

                            val userId = Session.userId ?: return@onClickLaunch

                            val dto = ProfileUpdateDto(
                                firstName = firstNameField.value,
                                lastName = lastNameField.value,
                                patronymic = patronymicField.value,
                                phoneNumber = phoneField.value,
                                avatar = avatarField.value,
                                gender = genderField.value,
                                dateOfBirth = convertDate(birthDateField.getValueAsString()),
                            )

                            val result = profileApi.updateProfile(userId, dto)

                            result.onSuccess { updated ->
                                Session.updateFrom(updated)
                                onBack()
                            }

                            result.onFailure {
                                console.error("Ошибка обновления профиля: ${it.message}")
                                // можно вывести сообщение на экран
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