package ui

import api.ApiConfig
import api.PatientApiClient
import io.kvision.core.Container
import io.kvision.core.JustifyContent
import io.kvision.core.onClick
import io.kvision.form.select.Select
import io.kvision.form.text.Text
import io.kvision.form.text.text
import io.kvision.form.text.textArea
import io.kvision.html.Div
import io.kvision.html.H4
import io.kvision.html.Li
import io.kvision.html.Span
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.h4
import io.kvision.html.li
import io.kvision.html.nav
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.html.ul
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel
import io.kvision.toast.Toast
import io.kvision.utils.perc
import io.kvision.utils.px
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.js.Date
import kotlin.math.roundToInt
import org.interns.project.dto.DoctorNoteCreateRequest
import org.interns.project.dto.DoctorNotePatchRequest
import org.interns.project.dto.DoctorNoteResponse
import org.interns.project.dto.NoteVisibilityDto
import org.interns.project.dto.ClientProfileDto
import org.interns.project.dto.UserResponseDto

private enum class DoctorPatientTab { OVERVIEW, RECORDS }

@Serializable
private enum class DoctorRecordStatus(val label: String, val cssClass: String) {
    NORMAL("Normal", "status-normal"),
    REVIEWED("Reviewed", "status-reviewed"),
    ATTENTION("Needs Attention", "status-attention")
}

@Serializable
private data class DoctorRecordContent(
    val title: String,
    val category: String,
    val status: DoctorRecordStatus = DoctorRecordStatus.NORMAL,
    val notes: String,
    val doctorName: String? = null
)

private data class DoctorRecordEntry(
    val id: Long,
    val doctorUserId: Long?,
    val title: String,
    val doctorName: String,
    val createdAt: String,
    val category: String,
    val status: DoctorRecordStatus,
    val notes: String,
    val visibility: NoteVisibilityDto,
    val content: DoctorRecordContent
)

private data class DoctorPatientProfile(
    val userId: Long,
    val patientRecordId: Long?,
    val fullName: String,
    val initials: String,
    val age: Int?,
    val dateOfBirth: String?,
    val phone: String?,
    val email: String,
    val height: Double?,
    val weight: Double?,
    val bloodType: String?,
    val address: String?,
    val emergencyContactName: String?,
    val emergencyContactNumber: String?,
    val snils: String?,
    val passport: String?,
    val dmsOms: String?,
    val summary: String
) {
    val ageLabel: String
        get() = age?.let { "$it лет" } ?: "Возраст не указан"

    val heightLabel: String
        get() = height?.let { "${it.roundToInt()} см" } ?: "—"

    val weightLabel: String
        get() = weight?.let { "${it.roundToInt()} кг" } ?: "—"

    val phoneLabel: String
        get() = phone?.takeIf { it.isNotBlank() } ?: "—"

    val emailLabel: String
        get() = email

    val birthLabel: String
        get() = dateOfBirth ?: "—"
}

private fun buildDoctorPatientProfile(
    user: UserResponseDto,
    client: ClientProfileDto?,
    fallbackRecordId: Long?
): DoctorPatientProfile {
    val nameParts = listOfNotNull(user.firstName ?: user.name, user.lastName ?: user.surname)
    val fullName = nameParts.joinToString(" ").ifBlank { user.login }
    val initials = fullName
        .split(' ', '-', '_')
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifBlank { user.login.firstOrNull()?.uppercaseChar()?.toString() ?: "PT" }

    val summaryParts = mutableListOf<String>()
    user.dateOfBirth?.takeIf { it.isNotBlank() }?.let { summaryParts += "Дата рождения: $it" }
    client?.address?.takeIf { it.isNotBlank() }?.let { summaryParts += "Адрес: $it" }
    client?.emergencyContactName?.takeIf { it.isNotBlank() }?.let { name ->
        val contact = listOfNotNull(
            name,
            client.emergencyContactNumber?.takeIf { it.isNotBlank() }
        ).joinToString(", ")
        summaryParts += "Контактное лицо: $contact"
    }
    val summary = if (summaryParts.isEmpty()) {
        "Дополнительная информация отсутствует."
    } else {
        summaryParts.joinToString(separator = ". ", postfix = ".")
    }

    return DoctorPatientProfile(
        userId = user.id,
        patientRecordId = client?.id ?: fallbackRecordId,
        fullName = fullName,
        initials = initials,
        age = calculateAge(user.dateOfBirth),
        dateOfBirth = user.dateOfBirth,
        phone = user.phoneNumber,
        email = user.email,
        height = client?.height,
        weight = client?.weight,
        bloodType = client?.bloodType,
        address = client?.address,
        emergencyContactName = client?.emergencyContactName,
        emergencyContactNumber = client?.emergencyContactNumber,
        snils = client?.snils,
        passport = client?.passport,
        dmsOms = client?.dmsOms,
        summary = summary,
    )
}

private fun calculateAge(dateIso: String?): Int? {
    if (dateIso.isNullOrBlank()) return null
    return runCatching {
        val birth = Date(dateIso)
        val time = birth.getTime()
        if (time.isNaN()) return null
        val now = Date()
        var age = now.getFullYear() - birth.getFullYear()
        val monthDiff = now.getMonth() - birth.getMonth()
        if (monthDiff < 0 || (monthDiff == 0 && now.getDate() < birth.getDate())) {
            age -= 1
        }
        if (age >= 0) age else null
    }.getOrNull()
}

private val recordJson = Json { ignoreUnknownKeys = true }

private fun mapVisibilityToStatus(visibility: NoteVisibilityDto): DoctorRecordStatus = when (visibility) {
    NoteVisibilityDto.INTERNAL -> DoctorRecordStatus.ATTENTION
    NoteVisibilityDto.PATIENT -> DoctorRecordStatus.NORMAL
}

private fun DoctorRecordStatus.toVisibility(): NoteVisibilityDto = when (this) {
    DoctorRecordStatus.ATTENTION -> NoteVisibilityDto.INTERNAL
    DoctorRecordStatus.NORMAL,
    DoctorRecordStatus.REVIEWED -> NoteVisibilityDto.PATIENT
}

private fun DoctorNoteResponse.toRecordEntry(
    selfDoctorLabel: String,
    overrideDoctorUserId: Long? = null,
): DoctorRecordEntry {
    val parsed = runCatching { recordJson.decodeFromString<DoctorRecordContent>(note) }.getOrNull()
    val status = parsed?.status ?: mapVisibilityToStatus(visibility)
    val title = parsed?.title?.takeIf { it.isNotBlank() }
        ?: note.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }
        ?: "Medical note #$id"
    val notesText = parsed?.notes?.takeIf { it.isNotBlank() } ?: note
    val category = parsed?.category?.takeIf { it.isNotBlank() } ?: "General"
    val doctorUserId = overrideDoctorUserId ?: doctorId
    val doctorName = parsed?.doctorName?.takeIf { it.isNotBlank() }
        ?: if (Session.userId != null && doctorUserId == Session.userId) selfDoctorLabel else "Doctor #$doctorUserId"

    val safeContent = parsed?.copy(
        title = title,
        category = category,
        status = status,
        notes = notesText,
        doctorName = doctorName,
    ) ?: DoctorRecordContent(
        title = title,
        category = category,
        status = status,
        notes = notesText,
        doctorName = doctorName,
    )

    return DoctorRecordEntry(
        id = id,
        doctorUserId = doctorUserId,
        title = title,
        doctorName = doctorName,
        createdAt = createdAt,
        category = category,
        status = status,
        notes = notesText,
        visibility = visibility,
        content = safeContent,
    )
}

fun Container.doctorPatientScreen(
    patientUserId: Long,
    patientRecordId: Long?,
    onLogout: () -> Unit = { Navigator.showHome() },
    onBack: () -> Unit = { Navigator.showDoctor() }
) = vPanel(spacing = 12) {
    val uiScope = MainScope()
    val apiClient = PatientApiClient()
    var currentPatientRecordId: Long? = patientRecordId
    val doctorLabel = listOfNotNull(Session.fullName, Session.email, Session.userId?.let { "Doctor #$it" })
        .firstOrNull() ?: "Doctor"

    fun cleanup() {
        uiScope.cancel()
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

    var profile: DoctorPatientProfile? = null
    var isLoadingProfile = true
    var profileError: String? = null
    var activeTab = DoctorPatientTab.OVERVIEW

    val records = mutableListOf<DoctorRecordEntry>()
    var recordsLoaded = false
    var isLoadingRecords = false
    var recordsError: String? = null
    var editingRecordId: Long? = null

    val recordsContainer = vPanel(spacing = 12, className = "doctor-records-list").apply {
        width = 100.perc
    }
    var renderRecordItems: () -> Unit = {}

    var sidebarAvatar: Div? = null
    var sidebarName: H4? = null
    var sidebarId: Span? = null

    var rerender: () -> Unit = {}

    fun applyProfileUi(data: DoctorPatientProfile?) {
        sidebarAvatar?.content = data?.initials ?: "PT"
        sidebarName?.content = data?.fullName ?: "Пациент"
        val recordLabel = data?.patientRecordId ?: currentPatientRecordId
        val userLabel = data?.userId ?: patientUserId
        val idText = buildString {
            append("Patient record ID: ")
            append(recordLabel?.let { "#$it" } ?: "—")
            append(" (user #")
            append(userLabel)
            append(")")
        }
        sidebarId?.content = idText
    }

    fun loadRecords(force: Boolean = false) {
        if (isLoadingRecords) return
        if (recordsLoaded && !force) return
        if (Session.userId == null) {
            recordsError = "Необходима авторизация"
            renderRecordItems()
            return
        }
        if (profile == null) {
            if (!isLoadingProfile) {
                recordsError = profileError ?: "Профиль пациента не найден"
                renderRecordItems()
            }
            return
        }
        val recordId = currentPatientRecordId
        if (recordId == null) {
            recordsError = "Пациентская запись не найдена"
            renderRecordItems()
            return
        }

        isLoadingRecords = true
        recordsError = null
        editingRecordId = null
        renderRecordItems()

        uiScope.launch {
            val result = apiClient.listNotes(recordId, includeInternal = true)
            result.fold(
                onSuccess = { notes ->
                    recordsLoaded = true
                    records.clear()
                    records.addAll(notes.map { it.toRecordEntry(doctorLabel) })
                },
                onFailure = { error ->
                    recordsError = error.message ?: "Не удалось загрузить записи"
                    Toast.danger(recordsError ?: "Ошибка загрузки")
                }
            )
            isLoadingRecords = false
            renderRecordItems()
        }
    }

    renderRecordItems = {
        recordsContainer.removeAll()
        when {
            isLoadingRecords -> {
                recordsContainer.div(className = "doctor-record-card card") {
                    p("Загрузка записей...", className = "doctor-record-notes")
                }
            }
            recordsError != null -> {
                recordsContainer.div(className = "doctor-record-card card") {
                    p(recordsError ?: "Ошибка", className = "doctor-record-notes")
                    button("Повторить", className = "btn-ghost-sm").onClick {
                        recordsError = null
                        loadRecords(force = true)
                    }
                }
            }
            records.isEmpty() -> {
                recordsContainer.div(className = "doctor-record-empty card") {
                    p(
                        "No medical records yet. Add your first note to keep track of patient history.",
                        className = "doctor-record-empty-text"
                    )
                }
            }
            else -> {
                records.forEach { record ->
                    val isEditing = record.id == editingRecordId
                    recordsContainer.div(className = "doctor-record-card card") {
                        if (!isEditing) {
                            div(className = "doctor-record-body") {
                                h4(record.title, className = "doctor-record-title")
                                span(
                                    "${record.doctorName} • ${record.createdAt}",
                                    className = "doctor-record-subtitle"
                                )
                                p(record.notes, className = "doctor-record-notes")
                            }

                            div(className = "doctor-record-meta") {
                                span(record.category, className = "doctor-record-tag")
                                span(
                                    record.status.label,
                                    className = "doctor-record-status ${record.status.cssClass}"
                                )
                            }

                            hPanel(spacing = 8, className = "doctor-record-actions") {
                                justifyContent = JustifyContent.END
                                button("Редактировать", className = "btn-ghost-sm").onClick {
                                    editingRecordId = record.id
                                    renderRecordItems()
                                }
                                button("Download", className = "btn-ghost-sm").onClick {
                                    Toast.info("Скачивание отчета будет доступно позже")
                                }
                            }
                        } else {
                            div(className = "doctor-record-body") {
                                h4("Редактирование записи", className = "doctor-record-title")
                                span(
                                    "${record.doctorName} • ${record.createdAt}",
                                    className = "doctor-record-subtitle"
                                )
                            }

                            div(className = "doctor-record-editor") {
                                val titleInput = text(label = "Название записи").apply {
                                    addCssClass("kv-input")
                                    value = record.content.title
                                }
                                val categoryInput = text(label = "Категория").apply {
                                    addCssClass("kv-input")
                                    value = record.content.category
                                }
                                val statusSelect = Select(
                                    options = listOf(
                                        DoctorRecordStatus.NORMAL.name to DoctorRecordStatus.NORMAL.label,
                                        DoctorRecordStatus.REVIEWED.name to DoctorRecordStatus.REVIEWED.label,
                                        DoctorRecordStatus.ATTENTION.name to DoctorRecordStatus.ATTENTION.label,
                                    ),
                                    label = "Статус",
                                ).apply {
                                    addCssClass("kv-input")
                                    value = record.status.name
                                }
                                val notesInput = textArea(label = "Описание записи").apply {
                                    addCssClass("kv-input")
                                    height = 120.px
                                    value = record.content.notes
                                }
                                val errorLabel = span("").apply { addCssClass("text-danger") }

                                add(titleInput)
                                add(categoryInput)
                                add(statusSelect)
                                add(notesInput)
                                add(errorLabel)

                                hPanel(spacing = 8, className = "doctor-record-actions") {
                                    justifyContent = JustifyContent.END
                                    val saveButton = button("Сохранить изменения", className = "btn-primary")
                                    val cancelButton = button("Отмена", className = "btn-ghost-sm")

                                    cancelButton.onClick {
                                        editingRecordId = null
                                        renderRecordItems()
                                    }

                                    saveButton.onClick {
                                        val title = titleInput.value?.trim().orEmpty()
                                        val category = categoryInput.value?.trim().orEmpty()
                                        val statusValue = statusSelect.value ?: record.status.name
                                        val notes = notesInput.value?.trim().orEmpty()

                                        when {
                                            title.isBlank() -> {
                                                errorLabel.content = "Введите название записи"
                                                return@onClick
                                            }

                                            notes.isBlank() -> {
                                                errorLabel.content = "Добавьте описание записи"
                                                return@onClick
                                            }
                                        }

                                        errorLabel.content = ""
                                        saveButton.disabled = true
                                        cancelButton.disabled = true

                                        val status = runCatching { DoctorRecordStatus.valueOf(statusValue) }
                                            .getOrDefault(record.status)
                                        val content = DoctorRecordContent(
                                            title = title,
                                            category = if (category.isBlank()) "General" else category,
                                            status = status,
                                            notes = notes,
                                            doctorName = record.content.doctorName,
                                        )

                                        uiScope.launch {
                                            val result = apiClient.updateNote(
                                                record.id,
                                                DoctorNotePatchRequest(
                                                    note = recordJson.encodeToString(content),
                                                    visibility = status.toVisibility(),
                                                ),
                                            )

                                            result.fold(
                                                onSuccess = { response ->
                                                    val updatedEntry = response.toRecordEntry(
                                                        doctorLabel,
                                                        record.doctorUserId,
                                                    )
                                                    val index = records.indexOfFirst { it.id == record.id }
                                                    if (index >= 0) {
                                                        records[index] = updatedEntry
                                                    }
                                                    recordsError = null
                                                    editingRecordId = null
                                                    renderRecordItems()
                                                    Toast.success("Запись обновлена")
                                                },
                                                onFailure = { error ->
                                                    errorLabel.content = error.message ?: "Не удалось обновить запись"
                                                    saveButton.disabled = false
                                                    cancelButton.disabled = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun loadProfile() {
        isLoadingProfile = true
        profileError = null
        recordsLoaded = false
        records.clear()
        currentPatientRecordId = patientRecordId
        applyProfileUi(null)
        rerender()

        uiScope.launch {
            val userResult = apiClient.getPatientProfile(patientUserId)
            userResult.fold(
                onSuccess = { user ->
                    val clientResult = apiClient.getClientProfile(user.id)
                    val client = clientResult.getOrElse { error ->
                        Toast.danger(error.message ?: "Не удалось загрузить дополнительные данные пациента")
                        null
                    }
                    currentPatientRecordId = client?.id ?: currentPatientRecordId ?: patientRecordId
                    profile = buildDoctorPatientProfile(user, client, currentPatientRecordId ?: patientUserId)
                    profileError = null
                    isLoadingProfile = false
                    applyProfileUi(profile)
                    rerender()
                },
                onFailure = { error ->
                    profile = null
                    profileError = error.message ?: "Пациент не найден"
                    isLoadingProfile = false
                    currentPatientRecordId = patientRecordId
                    applyProfileUi(null)
                    rerender()
                    Toast.danger(profileError ?: "Ошибка загрузки профиля")
                }
            )
        }
    }

    fun createRecordForm(container: Container) {
        val titleInput: Text = text(label = "Название записи").apply { addCssClass("kv-input") }
        val categoryInput: Text = text(label = "Категория").apply {
            value = "General"
            addCssClass("kv-input")
        }
        val statusSelect = Select(
            options = listOf(
                DoctorRecordStatus.NORMAL.name to DoctorRecordStatus.NORMAL.label,
                DoctorRecordStatus.REVIEWED.name to DoctorRecordStatus.REVIEWED.label,
                DoctorRecordStatus.ATTENTION.name to DoctorRecordStatus.ATTENTION.label
            ),
            label = "Статус"
        ).apply {
            value = DoctorRecordStatus.NORMAL.name
            addCssClass("kv-input")
        }
        val notesInput = textArea(label = "Описание записи").apply {
            placeholder = "Добавьте заметки врача"
            addCssClass("kv-input")
            height = 120.px
        }
        val errorLabel = span("").apply { addCssClass("text-danger") }
        val addButton = button("Добавить запись", className = "btn-primary")

        container.div(className = "card block doctor-record-editor") {
            h4("Добавить новую запись", className = "block title")
            vPanel(spacing = 12) {
                add(titleInput)
                add(categoryInput)
                add(statusSelect)
                add(notesInput)
                add(errorLabel)
                add(addButton)
            }
        }

        addButton.onClick {
            val title = titleInput.value?.trim().orEmpty()
            val category = categoryInput.value?.trim().orEmpty()
            val statusValue = statusSelect.value ?: DoctorRecordStatus.NORMAL.name
            val notes = notesInput.value?.trim().orEmpty()

            when {
                title.isBlank() -> {
                    errorLabel.content = "Введите название записи"
                    return@onClick
                }
                notes.isBlank() -> {
                    errorLabel.content = "Добавьте описание записи"
                    return@onClick
                }
                Session.userId == null -> {
                    errorLabel.content = "Необходима авторизация"
                    return@onClick
                }
                profile == null -> {
                    errorLabel.content = "Профиль пациента не загружен"
                    return@onClick
                }
            }

            val recordId = currentPatientRecordId
            if (recordId == null) {
                errorLabel.content = "Пациентская запись не найдена"
                return@onClick
            }

            errorLabel.content = ""
            addButton.disabled = true

            val status = runCatching { DoctorRecordStatus.valueOf(statusValue) }
                .getOrDefault(DoctorRecordStatus.NORMAL)
            val content = DoctorRecordContent(
                title = title,
                category = if (category.isBlank()) "General" else category,
                status = status,
                notes = notes,
                doctorName = doctorLabel
            )

            uiScope.launch {
                val result = apiClient.createNote(
                    recordId,
                    DoctorNoteCreateRequest(
                        doctorId = Session.userId!!,
                        note = recordJson.encodeToString(content),
                        visibility = status.toVisibility()
                    )
                )

                result.fold(
                    onSuccess = { response ->
                        titleInput.value = ""
                        categoryInput.value = "General"
                        statusSelect.value = DoctorRecordStatus.NORMAL.name
                        notesInput.value = ""
                        recordsError = null
                        recordsLoaded = true
                        records.add(0, response.toRecordEntry(doctorLabel))
                        renderRecordItems()
                        Toast.success("Запись добавлена")
                    },
                    onFailure = { error ->
                        errorLabel.content = error.message ?: "Не удалось добавить запись"
                        Toast.danger(errorLabel.content ?: "Ошибка")
                    }
                )

                addButton.disabled = false
            }
        }
    }

    fun Container.renderOverview(profileData: DoctorPatientProfile?, isLoading: Boolean, error: String?) {
        when {
            isLoading -> {
                div(className = "doctor-record-card card") {
                    p("Загрузка данных пациента...", className = "doctor-record-notes")
                }
            }
            error != null -> {
                div(className = "doctor-record-card card") {
                    p(error, className = "doctor-record-notes")
                    button("Повторить", className = "btn-ghost-sm").onClick { loadProfile() }
                }
            }
            profileData == null -> {
                div(className = "doctor-record-card card") {
                    p("Пациент не найден", className = "doctor-record-notes")
                    button("Назад к пациентам", className = "btn-ghost-sm").onClick {
                        cleanup()
                        onBack()
                    }
                }
            }
            else -> {
                val data = profileData
                div(className = "doctor-patient-header card") {
                    div(className = "doctor-patient-header-left") {
                        span(data.fullName, className = "doctor-patient-name")
                        span(data.ageLabel, className = "doctor-patient-age")
                        span("Дата рождения: ${data.birthLabel}", className = "doctor-patient-age")
                    }
                    button("Назад к пациентам", className = "btn-ghost-sm").onClick {
                        cleanup()
                        onBack()
                    }
                }

                div(className = "doctor-patient-info card") {
                    h4("General Information", className = "doctor-patient-section-title")
                    div(className = "doctor-patient-stats") {
                        div(className = "doctor-patient-stat") {
                            span("Height", className = "stat-label")
                            span(data.heightLabel, className = "stat-value")
                        }
                        div(className = "doctor-patient-stat") {
                            span("Weight", className = "stat-label")
                            span(data.weightLabel, className = "stat-value")
                        }
                        div(className = "doctor-patient-stat") {
                            span("Phone", className = "stat-label")
                            span(data.phoneLabel, className = "stat-value")
                        }
                        div(className = "doctor-patient-stat") {
                            span("Email", className = "stat-label")
                            span(data.emailLabel, className = "stat-value")
                        }
                    }
                }

                div(className = "doctor-patient-overview card") {
                    h4("Patient Details", className = "doctor-patient-section-title")
                    p(data.summary, className = "doctor-patient-summary")

                    val documents = listOfNotNull(
                        data.snils?.takeIf { it.isNotBlank() }?.let { "СНИЛС: $it" },
                        data.passport?.takeIf { it.isNotBlank() }?.let { "Паспорт: $it" },
                    )
                    val emergency = listOfNotNull(
                        data.emergencyContactName?.takeIf { it.isNotBlank() }?.let { "Имя: $it" },
                        data.emergencyContactNumber?.takeIf { it.isNotBlank() }?.let { "Телефон: $it" },
                    )
                    val insurance = listOfNotNull(
                        data.dmsOms?.takeIf { it.isNotBlank() }?.let { "Полис: $it" },
                        data.bloodType?.takeIf { it.isNotBlank() }?.let { "Группа крови: $it" },
                        data.address?.takeIf { it.isNotBlank() }?.let { "Адрес: $it" },
                    )

                    div(className = "doctor-patient-columns") {
                        div(className = "doctor-patient-column") {
                            span("Documents", className = "doctor-patient-subtitle")
                            ul {
                                if (documents.isEmpty()) {
                                    li("Нет данных")
                                } else {
                                    documents.forEach { item -> li(item) }
                                }
                            }
                        }
                        div(className = "doctor-patient-column") {
                            span("Emergency Contacts", className = "doctor-patient-subtitle")
                            ul {
                                if (emergency.isEmpty()) {
                                    li("Нет данных")
                                } else {
                                    emergency.forEach { item -> li(item) }
                                }
                            }
                        }
                        div(className = "doctor-patient-column") {
                            span("Insurance & Info", className = "doctor-patient-subtitle")
                            ul {
                                if (insurance.isEmpty()) {
                                    li("Нет данных")
                                } else {
                                    insurance.forEach { item -> li(item) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun Container.renderRecords(profileData: DoctorPatientProfile?, isLoading: Boolean, error: String?) {
        div(className = "doctor-records-header") {
            h1("Medical Records", className = "doctor-records-title")
            profileData?.let { span(it.fullName, className = "doctor-records-subtitle") }
            button("Download All", className = "btn-ghost-sm").onClick {
                Toast.info("Скачивание всех записей скоро будет доступно")
            }
        }

        when {
            isLoading -> {
                div(className = "doctor-record-card card") {
                    p("Загрузка данных пациента...", className = "doctor-record-notes")
                }
            }
            error != null -> {
                div(className = "doctor-record-card card") {
                    p(error, className = "doctor-record-notes")
                    button("Повторить", className = "btn-ghost-sm").onClick { loadProfile() }
                }
            }
            profileData == null -> {
                div(className = "doctor-record-card card") {
                    p("Профиль пациента не найден. Добавление записей недоступно.", className = "doctor-record-notes")
                }
            }
            else -> {
                renderRecordItems()
                add(recordsContainer)
                createRecordForm(this)
            }
        }
    }

    var mainColumn: Container? = null
    var overviewItem: Li? = null
    var recordsItem: Li? = null

    fun updateActive(tab: DoctorPatientTab) {
        activeTab = tab
        overviewItem?.removeCssClass("is-active")
        recordsItem?.removeCssClass("is-active")
        when (tab) {
            DoctorPatientTab.OVERVIEW -> overviewItem?.addCssClass("is-active")
            DoctorPatientTab.RECORDS -> recordsItem?.addCssClass("is-active")
        }
        mainColumn?.let { column ->
            column.removeAll()
            when (tab) {
                DoctorPatientTab.OVERVIEW -> column.renderOverview(profile, isLoadingProfile, profileError)
                DoctorPatientTab.RECORDS -> {
                    column.renderRecords(profile, isLoadingProfile, profileError)
                    if (!isLoadingProfile && profile != null) {
                        loadRecords()
                    }
                }
            }
        }
    }

    rerender = { updateActive(activeTab) }

    div(className = "doctor container") {
        div(className = "doctor-patient grid") {
            div(className = "sidebar card doctor-patient-sidebar") {
                sidebarAvatar = div(className = "avatar circle doctor-patient-avatar") { +"PT" }
                sidebarName = h4("Пациент", className = "doctor-patient-sidebar-name")
                val initialRecordLabel = currentPatientRecordId?.let { "#$it" } ?: "—"
                sidebarId = span(
                    "Patient record ID: $initialRecordLabel (user #$patientUserId)",
                    className = "doctor-patient-sidebar-id"
                )

                nav {
                    ul(className = "side menu") {
                        overviewItem = li(className = "side_item") {
                            span("Overview")
                            span("\uD83D\uDCC8", className = "side icon")
                            onClick { updateActive(DoctorPatientTab.OVERVIEW) }
                        }

                        recordsItem = li(className = "side_item") {
                            span("Medical Records")
                            span("\uD83D\uDCC4", className = "side icon")
                            onClick { updateActive(DoctorPatientTab.RECORDS) }
                        }
                    }
                }

                div(className = "side button")
                button("Back to dashboard", className = "btn-primary-lg").onClick {
                    cleanup()
                    onBack()
                }
            }

            mainColumn = div(className = "doctor-patient-main column")
        }
    }

    applyProfileUi(profile)
    updateActive(activeTab)
    loadProfile()
}