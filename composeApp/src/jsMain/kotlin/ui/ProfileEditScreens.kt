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
import io.kvision.html.InputType
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
import state.DoctorState
import state.PatientState
import ui.components.patientSidebar
import kotlin.js.Date
import kotlin.text.Regex
import kotlin.math.roundToInt
import utils.normalizeGender

val profileApi = ProfileApiClient()

internal fun normalizeBirthInput(raw: String?): String {
    if (raw.isNullOrBlank()) return ""

    val digits = raw.filter { it.isDigit() }.take(8)
    var index = 0

    val dayRaw = digits.drop(index).take(2)
    val day = when {
        dayRaw.length < 2 -> dayRaw
        dayRaw.toIntOrNull() in 1..31 -> dayRaw
        else -> dayRaw.first().toString()
    }
    index += day.length

    val monthRaw = digits.drop(index).take(2)
    val month = when {
        monthRaw.length < 2 -> monthRaw
        monthRaw.toIntOrNull() in 1..12 -> monthRaw
        else -> monthRaw.first().toString()
    }
    index += month.length

    val year = digits.drop(index).take(4)

    return listOf(day, month, year).filter { it.isNotEmpty() }.joinToString(".")
}

internal fun toIsoDate(value: String?): String? {
    val digits = value?.filter { it.isDigit() } ?: return null
    if (digits.length < 6) return null

    var index = 0

    val dayRaw = digits.drop(index).take(2)
    if (dayRaw.length < 2) return null
    val day = dayRaw.toIntOrNull()?.takeIf { it in 1..31 } ?: return null
    index += 2

    val monthRaw = digits.drop(index).take(2)
    if (monthRaw.length < 2) return null
    val month = monthRaw.toIntOrNull()?.takeIf { it in 1..12 } ?: return null
    index += 2

    val yearCandidate = digits.drop(index)
    val year = when (yearCandidate.length) {
        2 -> {
            val suffix = yearCandidate.toIntOrNull() ?: return null
            val century = if (suffix <= 25) "20" else "19"
            "$century${suffix.toString().padStart(2, '0')}"
        }
        4 -> yearCandidate.takeIf { it.toIntOrNull() != null }
        else -> null
    } ?: return null

    val birth = runCatching { Date(year.toInt(), month - 1, day) }.getOrNull() ?: return null
    if (birth.getFullYear() != year.toInt() || birth.getMonth() != month - 1 || birth.getDate() != day) {
        return null
    }

    val age = runCatching {
        val today = Date()
        val millisInYear = 365.25 * 24 * 60 * 60 * 1000
        ((today.getTime() - birth.getTime()) / millisInYear).roundToInt()
    }.getOrDefault(0)

    return if (age in 16..150) {
        "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    } else {
        null
    }
}
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
    val isDoctorMode = mode == HeaderMode.DOCTOR
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
                        val lastNameField = text(label = "Фамилия") {
                            value = Session.lastName ?: ""
                        }
                        val firstNameField = text(label = "Имя") {
                            value = Session.firstName ?: ""
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

                        fun humanizeIso(value: String?): String {
                            if (value.isNullOrBlank()) return ""
                            val parts = value.split("-")
                            return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else ""
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

                        val heightField = text(label = "Рост (см)") {
                            addCssClass("kv-height")
                            type = InputType.NUMBER
                            placeholder = "1 – 260"

                            onEvent {
                                input = {
                                    val rawValue = value
                                    val number = rawValue?.toDoubleOrNull()
                                    val corrected = when {
                                        number == null || rawValue.isBlank() -> 1.0
                                        number < 1 -> 1.0
                                        number > 260 -> 260.0
                                        else -> number
                                    }
                                    if (corrected != number) {
                                        value = corrected.toInt().toString()
                                    }
                                }
                            }
                        }

                        val weightField = text(label = "Вес (кг)") {
                            addCssClass("kv-input")
                            type = InputType.NUMBER
                            placeholder = "1 – 636"

                            onEvent {
                                input = {
                                    val rawValue = value
                                    val number = rawValue?.toDoubleOrNull()
                                    val corrected = when {
                                        number == null || rawValue.isBlank() -> 1.0
                                        number < 1 -> 1.0
                                        number > 636 -> 636.0
                                        else -> number
                                    }
                                    if (corrected != number) {
                                        value = corrected.toInt().toString()
                                    }
                                }
                            }
                        }

                        val bloodTypeField = select(
                            listOf(
                                "" to "Выберите группу крови",
                                "I (O)-" to "I (O)-",
                                "I (O)+" to "I (O)+",
                                "II (A)-" to "II (A)-",
                                "II (A)+" to "II (A)+",
                                "III (B)-" to "III (B)-",
                                "III (B)+" to "III (B)+",
                                "IV (AB)-" to "IV (AB)-",
                                "IV (AB)+" to "IV (AB)+"
                            ),
                            label = "Группа крови"
                        ) {
                            addCssClass("kv-input")
                        }

                        val addressField = text(label = "Адрес проживания") {
                            addCssClass("kv-input")
                        }

                        val insuranceField = text(label = "Страховка / полис (ОМС/ДМС)") {
                            addCssClass("kv-input")
                        }

                        val snilsField = text(label = "СНИЛС") {
                            addCssClass("kv-input")
                        }

                        val passportField = text(label = "Паспорт") {
                            addCssClass("kv-input")
                        }

                        val emergencyNameField = text(label = "Экстренный контакт (имя)") {
                            addCssClass("kv-input")
                        }

                        val emergencyPhoneField = text(label = "Экстренный контакт (телефон)") {
                            placeholder = "+7 (XXX) XXX-XX-XX"
                            addCssClass("kv-input")
                            onEvent {
                                input = {
                                    val formatted = formatPhone(value)
                                    if (formatted != value) value = formatted
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

                        val doctorProfessionField = if (isDoctorMode) {
                            select(
                                options = listOf(
                                    "Кардиолог" to "Кардиолог",
                                    "Педиатр" to "Педиатр",
                                    "Невролог" to "Невролог",
                                    "Ортопед" to "Ортопед",
                                    "Офтальмолог" to "Офтальмолог",
                                    "Терапевт" to "Терапевт",
                                ),
                                label = "Специальность"
                            ) {
                                placeholder = "Выберите специальность"
                                value = "" // по умолчанию ничего не выбрано
                            }
                        } else null

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
                                heightField.value = profile?.client?.height?.takeIf { it > 0 }?.roundToInt()?.toString() ?: ""
                                weightField.value = profile?.client?.weight?.takeIf { it > 0 }?.roundToInt()?.toString() ?: ""
                                bloodTypeField.value = profile?.client?.bloodType ?: ""
                                addressField.value = profile?.client?.address ?: ""
                                insuranceField.value = profile?.client?.dmsOms ?: ""
                                snilsField.value = profile?.client?.snils ?: ""
                                passportField.value = profile?.client?.passport ?: ""
                                emergencyNameField.value = profile?.client?.emergencyContactName ?: ""
                                emergencyPhoneField.value = formatPhone(profile?.client?.emergencyContactNumber)
                                doctorProfessionField?.value = profile?.doctor?.profession ?: doctorProfessionField.value
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
                            val heightValue = heightField.value?.trim().orEmpty()
                            val weightValue = weightField.value?.trim().orEmpty()
                            val bloodTypeValue = bloodTypeField.value?.trim().orEmpty()
                            val addressValue = addressField.value?.trim().orEmpty()
                            val insuranceValue = insuranceField.value?.trim().orEmpty()
                            val snilsValue = snilsField.value?.trim().orEmpty()
                            val passportValue = passportField.value?.trim().orEmpty()
                            val emergencyNameValue = emergencyNameField.value?.trim().orEmpty()
                            val emergencyPhoneValue = formatPhone(emergencyPhoneField.value)

                            when {
                                last.isBlank() -> {
                                    errorText.content = "Укажите фамилию"
                                    return@onClickLaunch
                                }

                                first.isBlank() -> {
                                    errorText.content = "Укажите имя"
                                    return@onClickLaunch
                                }

                                isoBirthDate.isNullOrBlank() -> {
                                    errorText.content = "Укажите корректную дату рождения в формате ДД.ММ.ГГГГ"
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

                                isDoctorMode && doctorProfessionField?.value?.trim().isNullOrBlank() == true -> {
                                    errorText.content = "Укажите специальность"
                                    return@onClickLaunch
                                }

                                heightValue.isNotBlank() && heightValue.replace(',', '.').toDoubleOrNull() == null -> {
                                    errorText.content = "Рост должен быть числом"
                                    return@onClickLaunch
                                }

                                weightValue.isNotBlank() && weightValue.replace(',', '.').toDoubleOrNull() == null -> {
                                    errorText.content = "Вес должен быть числом"
                                    return@onClickLaunch
                                }

                                heightValue.isNotBlank() && heightValue.replace(',', '.').toDoubleOrNull()?.let { it < 40 || it > 260 } == true -> {
                                    errorText.content = "Рост должен быть в диапазоне 40-260 см"
                                    return@onClickLaunch
                                }

                                weightValue.isNotBlank() && weightValue.replace(',', '.').toDoubleOrNull()?.let { it < 20 || it > 400 } == true -> {
                                    errorText.content = "Вес должен быть в диапазоне 20-400 кг"
                                    return@onClickLaunch
                                }

                                bloodTypeValue.isNotBlank() && !Regex("""^(?i)(i{1,3}|iv|1|2|3|4|a|b|ab|o|0)[+-]?$""").matches(bloodTypeValue) -> {
                                    errorText.content = "Укажите группу крови, например A+, B- или III+"
                                    return@onClickLaunch
                                }

                                emergencyPhoneValue.isNotBlank() && !phoneRegex.matches(emergencyPhoneValue) -> {
                                    errorText.content = "Телефон экстренного контакта в формате +7 (XXX) XXX-XX-XX"
                                    emergencyPhoneField.value = emergencyPhoneValue
                                    return@onClickLaunch
                                }
                            }

                            errorText.content = ""
                            val userId = Session.userId ?: run {
                                errorText.content = "Не удалось определить пользователя"
                                return@onClickLaunch
                            }
                            val professionValue = doctorProfessionField?.value?.trim()

                            val dto = ProfileUpdateDto(
                                firstName = first,
                                lastName = last,
                                patronymic = patronymic.takeIf { it.isNotBlank() && !noPatronymic },
                                phoneNumber = formattedPhone.takeIf { it.isNotBlank() },
                                avatar = avatarField.value,
                                gender = normalizeGender(genderField.value),
                                dateOfBirth = isoBirthDate,
                                bloodType = bloodTypeValue.takeIf { it.isNotBlank() },
                                height = heightValue.replace(',', '.').toDoubleOrNull(),
                                weight = weightValue.replace(',', '.').toDoubleOrNull(),
                                emergencyContactName = emergencyNameValue.takeIf { it.isNotBlank() },
                                emergencyContactNumber = emergencyPhoneValue.takeIf { it.isNotBlank() },
                                address = addressValue.takeIf { it.isNotBlank() },
                                snils = snilsValue.takeIf { it.isNotBlank() },
                                passport = passportValue.takeIf { it.isNotBlank() },
                                dmsOms = insuranceValue.takeIf { it.isNotBlank() },
                                profession = if (isDoctorMode) professionValue else null,
                                info = null,
                                experience = null,
                                price = null
                            )

                            val result = profileApi.updateProfile(userId, dto)

                            result.onSuccess { updated ->
                                Session.hasNoPatronymic = noPatronymic
                                Session.updateFrom(updated)
                                Session.userId?.let { id ->
                                    if (isDoctorMode) {
                                        DoctorState.refresh(id)
                                    } else {
                                        PatientState.loadPatientDashboard(id)
                                    }
                                }
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