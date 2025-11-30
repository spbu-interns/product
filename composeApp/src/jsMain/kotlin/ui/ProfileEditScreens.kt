package ui

import api.ApiConfig
import api.ProfileApiClient
import api.PatientApiClient
import io.kvision.core.Container
import io.kvision.core.Display
import io.kvision.core.Position
import io.kvision.core.onClick
import io.kvision.core.onClickLaunch
import io.kvision.core.onEvent
import io.kvision.form.check.checkBox
import io.kvision.form.select.select
import io.kvision.form.text.Text
import io.kvision.form.text.text
import io.kvision.form.text.textArea
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.interns.project.dto.ProfileUpdateDto
import org.w3c.dom.HTMLInputElement
import state.DoctorState
import state.PatientState
import ui.components.DateCalendar
import ui.components.dateCalendar
import ui.components.patientSidebar
import io.kvision.utils.px
import kotlin.js.Date
import kotlin.math.roundToInt
import kotlin.text.Regex
import utils.normalizeGender
import io.kvision.utils.perc

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

internal fun parseBirthDate(value: String?): Date? {
    val iso = toIsoDate(value) ?: return null
    val parts = iso.split("-")
    if (parts.size != 3) return null

    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
    val day = parts[2].toIntOrNull()?.takeIf { it in 1..31 } ?: return null

    return Date(year, month - 1, day)
}

internal fun formatBirthDate(date: Date?): String = date?.let {
    listOf(
        it.getDate().toString().padStart(2, '0'),
        (it.getMonth() + 1).toString().padStart(2, '0'),
        it.getFullYear().toString()
    ).joinToString(".")
} ?: ""

internal fun humanizeIso(value: String?): String {
    if (value.isNullOrBlank()) return ""
    val parts = value.split("-")
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else ""
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

    fun buildDisplayName(): String = Session.fullName() ?: Session.email ?: "Пользователь"
    fun buildInitials(displayName: String): String = displayName
        .split(' ', '-', '_')
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifBlank { "ПС" }

    val displayNameState = MutableStateFlow(buildDisplayName())
    val initialsState = MutableStateFlow(buildInitials(displayNameState.value))
    val defaultSpecialty = "Специальность не указана"
    val specializationState = MutableStateFlow(defaultSpecialty)
    val userIdText = Session.userId?.let { "ID: $it" } ?: ""
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
                            displayNameState = displayNameState,
                            initialsState = initialsState,
                            coroutineScope = uiScope,
                            active = PatientSection.EDIT_PROFILE,
                            onOverview = { Navigator.showPatient() },
                            onAppointments = { Navigator.showAppointments() },
                            onMedicalRecords = { Navigator.showPatientMedicalRecords() },
                            onMyRecords = { Navigator.showMyRecords() },
                            onFindDoctor = { Navigator.showFind() },
                            onProfile = { Navigator.showPatientProfileEdit() },
                            onProfileAlreadyOpen = { Toast.info("Профиль уже открыт") },
                            onLogout = {
                                ApiConfig.clearToken(); Session.clear(); Navigator.showHome()
                            }
                        )
                    } else {
                        div(className = "sidebar card") {
                            val avatar = div(className = "avatar circle") { +initialsState.value }
                            val nameHeader = h3(displayNameState.value, className = "account name")

                            uiScope.launch {
                                displayNameState.collect { nameHeader.content = it }
                            }
                            uiScope.launch {
                                initialsState.collect { avatar.content = it }
                            }
                        }
                    }
                }

                HeaderMode.DOCTOR -> {
                    div(className = "sidebar card") {
                        val avatar = div(className = "avatar circle") { +initialsState.value }
                        val nameHeader = h3(displayNameState.value, className = "account name")
                        val specializationText = p(specializationState.value, className = "account id")
                        if (userIdText.isNotBlank()) {
                            p(userIdText, className = "account id")
                        }

                        uiScope.launch {
                            displayNameState.collect { nameHeader.content = it }
                        }
                        uiScope.launch {
                            initialsState.collect { avatar.content = it }
                        }
                        uiScope.launch {
                            specializationState.collect { specializationText.content = it }
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
                        val avatar = div(className = "avatar circle") { +initialsState.value }
                        val nameHeader = h3(displayNameState.value, className = "account name")

                        uiScope.launch {
                            displayNameState.collect { nameHeader.content = it }
                        }
                        uiScope.launch {
                            initialsState.collect { avatar.content = it }
                        }
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

                        val today = Date()
                        val birthMaxDate = Date(today.getFullYear() - 16, today.getMonth(), today.getDate())
                        val birthMinDate = Date(today.getFullYear() - 150, today.getMonth(), today.getDate())
                        var birthCalendar: DateCalendar? = null

// поле на всю ширину
                        val birthDateField = Text(label = "Дата рождения").apply {
                            placeholder = "ДД.ММ.ГГГГ"
                            value = humanizeIso(Session.dateOfBirth)
                            addCssClass("kv-input")
                            width = 100.perc
                        }

// обёртка — relative, чтобы календарь встал попапом под полем
                        div(className = "birthdate-wrapper") {
                            display = Display.BLOCK
                            position = Position.RELATIVE
                            width = 100.perc

                            add(birthDateField)

                            birthCalendar = dateCalendar(
                                initialDate = parseBirthDate(birthDateField.value),
                                minDate = birthMinDate,
                                maxDate = birthMaxDate
                            ) { date ->
                                val formatted = formatBirthDate(date)
                                if (birthDateField.value != formatted) {
                                    birthDateField.value = formatted
                                }
                                birthCalendar?.visible = false
                            }.apply {
                                visible = false
                                position = Position.ABSOLUTE
                                top = 80.px
                                left = 0.px
                            }
                        }

                        birthDateField.onEvent {
                            input = {
                                val next = normalizeBirthInput(birthDateField.value)
                                if (birthDateField.value != next) birthDateField.value = next
                                birthCalendar?.setSelectedDate(parseBirthDate(next))
                            }
                            click = {
                                birthCalendar?.visible = !birthCalendar.visible
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

                        val heightField = if (!isDoctorMode) {
                            text(label = "Рост (см)") {
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
                        } else null

                        val weightField = if (!isDoctorMode) {
                            text(label = "Вес (кг)") {
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
                        } else null

                        val bloodTypeField = if (!isDoctorMode) {
                            select(
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
                        } else null

                        val addressField = if (!isDoctorMode) {
                            text(label = "Адрес проживания") {
                                addCssClass("kv-input")
                            }
                        } else null

                        val insuranceField = if (!isDoctorMode) {
                            text(label = "Страховка / полис (ОМС/ДМС)") {
                                addCssClass("kv-input")
                            }
                        } else null

                        val snilsField = if (!isDoctorMode) {
                            text(label = "СНИЛС") {
                                addCssClass("kv-input")
                            }
                        } else null

                        val passportField = if (!isDoctorMode) {
                            text(label = "Паспорт") {
                                addCssClass("kv-input")
                            }
                        } else null

                        val emergencyNameField = if (!isDoctorMode) {
                            text(label = "Экстренный контакт (имя)") {
                                addCssClass("kv-input")
                            }
                        } else null

                        val emergencyPhoneField = if (!isDoctorMode) {
                            text(label = "Экстренный контакт (телефон)") {
                                placeholder = "+7 (XXX) XXX-XX-XX"
                                addCssClass("kv-input")
                                onEvent {
                                    input = {
                                        val formatted = formatPhone(value)
                                        if (formatted != value) value = formatted
                                    }
                                }
                            }
                        } else null

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

                        doctorProfessionField?.onEvent {
                            change = {
                                specializationState.value = doctorProfessionField.value?.takeIf { it.isNotBlank() }
                                    ?: defaultSpecialty
                            }
                        }

                        val doctorInfoField = if (isDoctorMode) {
                            textArea(label = "Описание") {
                                placeholder = "Расскажите пациентам о себе"
                                rows = 3
                                addCssClass("kv-input")
                            }
                        } else null

                        val doctorExperienceField = if (isDoctorMode) {
                            text(label = "Стаж (лет)") {
                                addCssClass("kv-input")
                                type = InputType.NUMBER
                                placeholder = "0–80"

                                onEvent {
                                    input = {
                                        val raw = value
                                        val number = raw?.toIntOrNull()
                                        val corrected = when {
                                            raw.isNullOrBlank() -> null
                                            number == null -> null
                                            number < 0 -> 0
                                            number > 80 -> 80
                                            else -> number
                                        }
                                        if (corrected != null && corrected != number) {
                                            value = corrected.toString()
                                        }
                                    }
                                }
                            }
                        } else null

                        val doctorPriceField = if (isDoctorMode) {
                            text(label = "Стоимость приёма (₽)") {
                                addCssClass("kv-input")
                                type = InputType.NUMBER
                                placeholder = "Например, 1500"

                                onEvent {
                                    input = {
                                        val raw = value
                                        val number = raw?.replace(',', '.')?.toDoubleOrNull()
                                        val corrected = when {
                                            raw.isNullOrBlank() -> null
                                            number == null -> null
                                            number < 0 -> 0.0
                                            number > 50000 -> 50000.0
                                            else -> number
                                        }
                                        if (corrected != null && corrected != number) {
                                            value = if (corrected % 1.0 == 0.0) corrected.toInt().toString() else corrected.toString()
                                        }
                                    }
                                }
                            }
                        } else null

                        uiScope.launch {
                            val userId = Session.userId ?: return@launch
                            patientApi.getFullUserProfile(userId).onSuccess { profile ->
                                profile?.user?.let { Session.updateFrom(it) }
                                displayNameState.value = buildDisplayName()
                                initialsState.value = buildInitials(displayNameState.value)
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
                                birthCalendar?.setSelectedDate(parseBirthDate(birthDateField.value))
                                avatarField.value = Session.avatar ?: profile?.user?.avatar ?: ""
                                heightField?.value = profile?.client?.height?.takeIf { it > 0 }?.roundToInt()?.toString() ?: ""
                                weightField?.value = profile?.client?.weight?.takeIf { it > 0 }?.roundToInt()?.toString() ?: ""
                                if (!isDoctorMode) {
                                    bloodTypeField?.value = profile?.client?.bloodType ?: ""
                                    addressField?.value = profile?.client?.address ?: ""
                                    insuranceField?.value = profile?.client?.dmsOms ?: ""
                                    snilsField?.value = profile?.client?.snils ?: ""
                                    passportField?.value = profile?.client?.passport ?: ""
                                    emergencyNameField?.value = profile?.client?.emergencyContactName ?: ""
                                    emergencyPhoneField?.value = formatPhone(profile?.client?.emergencyContactNumber)
                                }
                                doctorProfessionField?.value = profile?.doctor?.profession ?: doctorProfessionField.value
                                doctorInfoField?.value = profile?.doctor?.info ?: doctorInfoField.value ?: ""
                                doctorExperienceField?.value = profile?.doctor?.experience?.takeIf { it > 0 }?.toString() ?: ""
                                doctorPriceField?.value = profile?.doctor?.price?.takeIf { it > 0 }
                                    ?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
                                    ?: ""
                                specializationState.value = profile?.doctor?.profession?.takeIf { it.isNotBlank() }
                                    ?: defaultSpecialty
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
                            val heightValue = heightField?.value?.trim().orEmpty()
                            val weightValue = weightField?.value?.trim().orEmpty()
                            val bloodTypeValue = bloodTypeField?.value?.trim().orEmpty()
                            val addressValue = addressField?.value?.trim().orEmpty()
                            val insuranceValue = insuranceField?.value?.trim().orEmpty()
                            val snilsValue = snilsField?.value?.trim().orEmpty()
                            val passportValue = passportField?.value?.trim().orEmpty()
                            val emergencyNameValue = emergencyNameField?.value?.trim().orEmpty()
                            val emergencyPhoneValue = formatPhone(emergencyPhoneField?.value)
                            val doctorInfoValue = doctorInfoField?.value?.trim().orEmpty()
                            val doctorExperienceValue = doctorExperienceField?.value?.trim().orEmpty()
                            val doctorPriceValue = doctorPriceField?.value?.trim().orEmpty()

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

                                isDoctorMode && doctorInfoValue.isBlank() -> {
                                    errorText.content = "Добавьте краткое описание"
                                    return@onClickLaunch
                                }

                                isDoctorMode && doctorExperienceValue.isBlank() -> {
                                    errorText.content = "Укажите стаж"
                                    return@onClickLaunch
                                }

                                isDoctorMode && doctorExperienceValue.toIntOrNull() == null -> {
                                    errorText.content = "Стаж должен быть целым числом"
                                    return@onClickLaunch
                                }

                                isDoctorMode && doctorExperienceValue.toIntOrNull()?.let { it < 0 || it > 80 } == true -> {
                                    errorText.content = "Стаж должен быть в диапазоне 0–80 лет"
                                    return@onClickLaunch
                                }

                                isDoctorMode && doctorPriceValue.isBlank() -> {
                                    errorText.content = "Укажите стоимость приёма"
                                    return@onClickLaunch
                                }

                                isDoctorMode && doctorPriceValue.replace(',', '.').toDoubleOrNull() == null -> {
                                    errorText.content = "Стоимость должна быть числом"
                                    return@onClickLaunch
                                }

                                isDoctorMode && doctorPriceValue.replace(',', '.').toDoubleOrNull()?.let { it < 0 || it > 50000 } == true -> {
                                    errorText.content = "Стоимость должна быть в диапазоне 0–50000 ₽"
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

                                !isDoctorMode && emergencyPhoneValue.isNotBlank() && !phoneRegex.matches(emergencyPhoneValue) -> {
                                    errorText.content = "Телефон экстренного контакта в формате +7 (XXX) XXX-XX-XX"
                                    emergencyPhoneField?.value = emergencyPhoneValue
                                    return@onClickLaunch
                                }
                            }

                            errorText.content = ""
                            val userId = Session.userId ?: run {
                                errorText.content = "Не удалось определить пользователя"
                                return@onClickLaunch
                            }
                            val professionValue = doctorProfessionField?.value?.trim()
                            val experienceValue = doctorExperienceValue.toIntOrNull()
                            val priceValue = doctorPriceValue.replace(',', '.').toDoubleOrNull()

                            val dto = ProfileUpdateDto(
                                firstName = first,
                                lastName = last,
                                patronymic = patronymic.takeIf { it.isNotBlank() && !noPatronymic },
                                phoneNumber = formattedPhone.takeIf { it.isNotBlank() },
                                avatar = avatarField.value,
                                gender = normalizeGender(genderField.value),
                                dateOfBirth = isoBirthDate,
                                bloodType = if (!isDoctorMode) bloodTypeValue.takeIf { it.isNotBlank() } else null,
                                height = if (!isDoctorMode) heightValue.replace(',', '.').toDoubleOrNull() else null,
                                weight = if (!isDoctorMode) weightValue.replace(',', '.').toDoubleOrNull() else null,
                                emergencyContactName = if (isDoctorMode) null else emergencyNameValue.takeIf { it.isNotBlank() },
                                emergencyContactNumber = if (isDoctorMode) null else emergencyPhoneValue.takeIf { it.isNotBlank() },
                                address = if (isDoctorMode) null else addressValue.takeIf { it.isNotBlank() },
                                snils = if (isDoctorMode) null else snilsValue.takeIf { it.isNotBlank() },
                                passport = if (isDoctorMode) null else passportValue.takeIf { it.isNotBlank() },
                                dmsOms = if (isDoctorMode) null else insuranceValue.takeIf { it.isNotBlank() },
                                profession = if (isDoctorMode) professionValue else null,
                                info = if (isDoctorMode) doctorInfoValue else null,
                                experience = if (isDoctorMode) experienceValue else null,
                                price = if (isDoctorMode) priceValue else null
                            )

                            val result = profileApi.updateProfile(userId, dto)

                            result.onSuccess { updated ->
                                Session.hasNoPatronymic = noPatronymic
                                Session.updateFrom(updated)
                                displayNameState.value = buildDisplayName()
                                initialsState.value = buildInitials(displayNameState.value)
                                Session.userId?.let { id ->
                                    if (isDoctorMode) {
                                        DoctorState.refresh(id)
                                    } else {
                                        PatientState.loadPatientDashboard(id)
                                    }
                                }
                                Toast.success("Профиль успешно сохранён")
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