package ui

import api.ApiConfig
import api.ProfileApiClient
import api.PatientApiClient
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.core.onClickLaunch
import io.kvision.core.onEvent
import io.kvision.form.check.checkBox
import io.kvision.form.select.select
import io.kvision.form.text.text
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.h3
import io.kvision.html.li
import io.kvision.html.nav
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.html.ul
import io.kvision.panel.vPanel
import io.kvision.toast.Toast
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.interns.project.dto.ProfileUpdateDto
import org.w3c.dom.HTMLInputElement
import ui.components.patientSidebar
import kotlin.js.Date
import kotlin.text.Regex
import utils.normalizeGender

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
    val uiScope = MainScope()
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
    val patientApi = PatientApiClient()
    val phoneRegex = Regex("""^\+7 \(\d{3}\) \d{3}-\d{2}-\d{2}$""")

    fun formatPhone(raw: String?): String {
        val digits = raw.orEmpty().filter { it.isDigit() }
        if (digits.isEmpty()) return ""

        val national = when {
            digits.startsWith("7") || digits.startsWith("8") -> digits.drop(1)
            else -> digits
        }.take(10)

        val part1 = national.take(3)
        val part2 = national.drop(3).take(3)
        val part3 = national.drop(6).take(2)
        val part4 = national.drop(8).take(2)

        return buildString {
            append("+7")
            if (national.isEmpty()) return@buildString
            append(" (")
            append(part1)
            if (part1.length == 3) append(") ") else return@buildString
            append(part2)
            if (part2.length == 3) append("-") else return@buildString
            append(part3)
            if (part3.length == 2) append("-") else return@buildString
            append(part4)
        }.trimEnd()
    }



    div(className = "account container") {
        div(className = "account grid") {
            // Sidebar
            when (mode) {
                HeaderMode.PATIENT -> {
                    val patientId = Session.userId
                    if (patientId != null) {
                        patientSidebar(
                            patientId = patientId,
                            active = PatientSection.EDIT_PROFILE,
                            onOverview = { Navigator.showPatient() },
                            onAppointments = { Navigator.showAppointments() },
                            onMedicalRecords = { Navigator.showStub("Раздел медицинской карты находится в разработке") },
                            onMyRecords = { Navigator.showMyRecords() },
                            onFindDoctor = { Navigator.showFind() },
                            onProfile = { Navigator.showPatientProfileEdit() },
                            onLogout = {
                                ApiConfig.clearToken(); Session.clear(); Navigator.showHome()
                            }
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

                        nav {
                            ul(className = "side menu") {
                                li(className = "side_item") {
                                    span("Обзор"); span("\uD83D\uDC64", className = "side icon")
                                    onClick {
                                        window.asDynamic().scrollTo(js("({ top: 0, behavior: 'smooth' })"))
                                        Navigator.showDoctor()
                                    }
                                }
                                li(className = "side_item") {
                                    span("Расписание"); span("\uD83D\uDCC5", className = "side icon")
                                    onClick {
                                        window.asDynamic().scrollTo(js("({ top: 0, behavior: 'smooth' })"))
                                        Navigator.showDoctor()
                                    }
                                }
                                li(className = "side_item") {
                                    span("Пациенты"); span("\uD83D\uDC65", className = "side icon")
                                    onClick {
                                        window.asDynamic().scrollTo(js("({ top: 0, behavior: 'smooth' })"))
                                        Toast.info("История посещений пациентов скоро будет доступна")
                                    }
                                }
                                li(className = "side_item is-active") {
                                    span("Мой профиль"); span("\uD83D\uDC64", className = "side icon")
                                }
                            }
                        }

                        div(className = "side button")
                        button("Расписание", className = "btn-secondary-lg timetable-trigger").onClick {
                            Navigator.showDoctor()
                        }
                        button("Выйти", className = "btn-logout-sm").onClick {
                            ApiConfig.clearToken(); Session.clear(); Navigator.showHome()
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
                        var patronymicDirty = false
                        val patronymicField = text(label = "Отчество") {
                            value = Session.patronymic ?: ""
                            onEvent {
                                input = { patronymicDirty = true }
                            }
                        }
                        val noPatronymicCheck = checkBox(label = "Нет отчества") {
                            value = Session.patronymic.isNullOrBlank() && Session.hasNoPatronymic
                        }.apply {
                            fun syncState(checked: Boolean) {
                                patronymicField.disabled = checked
                                if (checked) patronymicField.value = ""
                            }

                            syncState(value)

                            onClick {
                                patronymicDirty = true
                                val checked = value
                                syncState(checked)
                            }
                        }

                        fun normalizeBirthInput(raw: String?): String {
                            if (raw.isNullOrBlank()) return ""
                            val digits = raw.filter { it.isDigit() }.take(8)
                            val hasSeparator = raw.contains('.')

                            val dayRaw = digits.take(2)
                            val monthRaw = digits.drop(2).take(2)
                            val yearRaw = digits.drop(4)

                            val day = if (dayRaw.length == 1 && hasSeparator) "0$dayRaw" else dayRaw
                            val month = if (monthRaw.length == 1 && (hasSeparator || digits.length > 3)) "0$monthRaw" else monthRaw

                            return buildString {
                                if (day.isNotEmpty()) {
                                    append(day.take(2))
                                    if (day.length >= 2) append('.')
                                }
                                if (month.isNotEmpty()) {
                                    append(month.take(2))
                                    if (month.length >= 2) append('.')
                                }
                                if (yearRaw.isNotEmpty()) append(yearRaw.take(4))
                            }
                        }

                        fun humanizeIso(value: String?): String {
                            if (value.isNullOrBlank()) return ""
                            val parts = value.split("-")
                            return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else ""
                        }

                        fun toIsoDate(value: String?): String? {
                            val digits = value?.filter { it.isDigit() } ?: return null
                            if (digits.length < 6) return null

                            val day = digits.take(2).padStart(2, '0')
                            val month = digits.drop(2).take(2).padStart(2, '0')
                            val yearCandidate = digits.drop(4)
                            val year = when (yearCandidate.length) {
                                2 -> {
                                    val suffix = yearCandidate.padStart(2, '0').toInt()
                                    val century = if (suffix <= 25) "20" else "19"
                                    "$century${suffix.toString().padStart(2, '0')}"
                                }
                                4 -> yearCandidate
                                else -> return null
                            }

                            val iso = "$year-$month-$day"

                            val age = runCatching {
                                val today = Date()
                                val birth = Date(year.toInt(), month.toInt() - 1, day.toInt())
                                val millisInYear = 365.25 * 24 * 60 * 60 * 1000
                                ((today.getTime() - birth.getTime()) / millisInYear).toInt()
                            }.getOrDefault(0)

                            return if (age in 16..150) iso else null
                        }

                        val birthDateField = text(label = "Дата рождения") {
                            placeholder = "ДД.ММ.ГГГГ"
                            value = humanizeIso(Session.dateOfBirth)
                            addCssClass("kv-input")
                            onEvent {
                                input = {
                                    val next = normalizeBirthInput(value)
                                    if (value != next) value = next
                                }
                            }
                        }

                        val phoneField = text(label = "Номер телефона") {
                            val self = this
                            placeholder = "+7 (XXX) XXX-XX-XX"
                            value = formatPhone(Session.phoneNumber)
                            addCssClass("kv-input")
                            onEvent {
                                input = {
                                    val formatted = formatPhone(value)
                                    if (formatted != value) value = formatted
                                }
                                keydown = keydown@{ event ->
                                    val key = event.asDynamic().key?.toString()
                                    val allowedControl = event.asDynamic().ctrlKey == true || event.asDynamic().metaKey == true
                                    val isEditingKey = key in listOf("Backspace", "Delete", "ArrowLeft", "ArrowRight", "Tab")
                                    val isDigit = key?.singleOrNull()?.isDigit() == true
                                    val allowedSign = key in listOf("+", " ", "(", ")", "-")

                                    if (key == "Backspace" || key == "Delete") {
                                        val inputEl = self.getElement()?.unsafeCast<HTMLInputElement?>()
                                        val current = self.value ?: ""
                                        val cursor = (inputEl?.selectionStart ?: current.length)
                                            .coerceIn(0, current.length)

                                        val digitPositions = current.mapIndexedNotNull { index, ch ->
                                            ch.takeIf { it.isDigit() }?.let { index }
                                        }
                                        val digits = current.filter { it.isDigit() }

                                        val targetDigitIndex = when (key) {
                                            "Backspace" -> digitPositions.indexOfLast { it < cursor }
                                                .takeIf { it != -1 }
                                                ?: digitPositions.lastIndex.takeIf { digitPositions.isNotEmpty() }

                                            else -> digitPositions.indexOfFirst { it >= cursor }
                                                .takeIf { it != -1 }
                                                ?: digitPositions.firstOrNull()?.let { 0 }
                                        }

                                        if (targetDigitIndex != null) {
                                            val newDigits = buildString {
                                                digits.forEachIndexed { index, ch ->
                                                    if (index != targetDigitIndex) append(ch)
                                                }
                                            }
                                            val formatted = formatPhone(newDigits)
                                            val caretPosition = formatPhone(newDigits.take(targetDigitIndex)).length

                                            self.value = formatted
                                            inputEl?.let { el ->
                                                el.selectionStart = caretPosition
                                                el.selectionEnd = caretPosition
                                            }

                                            event.preventDefault()
                                            return@keydown
                                        }
                                    }
                                    if (key != null && !isDigit && !isEditingKey && !allowedSign && !allowedControl) {
                                        event.preventDefault()
                                    }
                                }
                            }
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
                            value = normalizeGender(Session.gender)
                        }

                        uiScope.launch {
                            val userId = Session.userId ?: return@launch
                            patientApi.getFullUserProfile(userId).onSuccess { profile ->
                                profile?.user?.let { Session.updateFrom(it) }
                                firstNameField.value = Session.firstName ?: profile?.user?.name ?: ""
                                lastNameField.value = Session.lastName ?: profile?.user?.surname ?: ""
                                if (!patronymicDirty) {
                                    patronymicField.value = Session.patronymic ?: profile?.user?.patronymic ?: ""
                                    noPatronymicCheck.value = Session.patronymic.isNullOrBlank() && Session.hasNoPatronymic
                                    patronymicField.disabled = noPatronymicCheck.value
                                }
                                phoneField.value = formatPhone(Session.phoneNumber ?: profile?.user?.phoneNumber)
                                genderField.value = normalizeGender(Session.gender ?: profile?.user?.gender)
                                birthDateField.value = humanizeIso(Session.dateOfBirth ?: profile?.user?.dateOfBirth)
                                avatarField.value = Session.avatar ?: profile?.user?.avatar ?: ""
                            }.onFailure {
                                Toast.warning(it.message ?: "Не удалось загрузить профиль")
                            }
                        }

                        val errorText = span("").apply { addCssClass("text-danger") }

                        div(className = "side button")

                        button("Сохранить", className = "btn-primary-lg").onClickLaunch {

                            val first = firstNameField.value?.trim().orEmpty()
                            val last = lastNameField.value?.trim().orEmpty()
                            val patronymic = patronymicField.value?.trim().orEmpty()
                            val noPatronymic = noPatronymicCheck.value
                            val isoBirthDate = toIsoDate(birthDateField.getValueAsString())
                            val formattedPhone = formatPhone(phoneField.value)

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
                                    errorText.content = "Уточните дату рождения в формате ДД.ММ.ГГГГ"
                                    return@onClickLaunch
                                }

                                formattedPhone.isNotBlank() && !phoneRegex.matches(formattedPhone) -> {
                                    errorText.content = "Введите номер в формате +7 (XXX) XXX-XX-XX"
                                    phoneField.value = formattedPhone
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
                                phoneNumber = formattedPhone.takeIf { it.isNotBlank() },
                                avatar = avatarField.value,
                                gender = normalizeGender(genderField.value),
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