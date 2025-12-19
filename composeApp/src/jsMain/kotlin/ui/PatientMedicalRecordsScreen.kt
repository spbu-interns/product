package ui

import api.ApiConfig
import api.PatientApiClient
import io.kvision.core.Container
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.h4
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.panel.vPanel
import io.kvision.toast.Toast
import io.kvision.utils.perc
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.interns.project.dto.DoctorNoteResponse
import org.interns.project.dto.NoteVisibilityDto

private enum class PatientRecordStatus(val label: String, val cssClass: String) {
    NORMAL("Нормальный", "status-normal"),
    REVIEWED("Просмотрено", "status-reviewed"),
    ATTENTION("Требует внимания", "status-attention"),
}

private data class PatientRecordContent(
    val title: String,
    val category: String,
    val status: PatientRecordStatus = PatientRecordStatus.NORMAL,
    val notes: String,
    val doctorName: String? = null,
    val doctorSpecialty: String? = null,
)

private data class PatientRecordEntry(
    val id: Long,
    val title: String,
    val doctorName: String,
    val doctorSpecialty: String?,
    val createdAt: String,
    val displayDate: String,
    val category: String,
    val status: PatientRecordStatus,
    val notes: String,
)

private val recordJson = Json { ignoreUnknownKeys = true }

private fun mapVisibilityToStatus(visibility: NoteVisibilityDto): PatientRecordStatus = when (visibility) {
    NoteVisibilityDto.INTERNAL -> PatientRecordStatus.ATTENTION
    NoteVisibilityDto.PATIENT -> PatientRecordStatus.NORMAL
}

private fun formatRecordDate(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "—"
    return runCatching {
        val timePartStart = isoString.indexOf('T')
        val hasOffset = timePartStart != -1 &&
                isoString.indexOfAny(charArrayOf('+', '-'), startIndex = timePartStart) != -1

        val normalized = when {
            isoString.endsWith("Z") || isoString.endsWith("z") -> isoString
            hasOffset -> isoString
            else -> isoString + "Z"
        }

        val date = kotlin.js.Date(normalized)
        val time = date.getTime()
        if (time.isNaN()) return isoString
        val day = date.getDate().toString().padStart(2, '0')
        val month = (date.getMonth() + 1).toString().padStart(2, '0')
        val year = date.getFullYear()
        val hours = date.getHours().toString().padStart(2, '0')
        val minutes = date.getMinutes().toString().padStart(2, '0')
        "$day.$month.$year, $hours:$minutes"
    }.getOrDefault(isoString)
}

private fun DoctorNoteResponse.toPatientRecordEntry(): PatientRecordEntry {
    val parsed = runCatching { recordJson.decodeFromString<PatientRecordContent>(note) }.getOrNull()
    val status = parsed?.status ?: mapVisibilityToStatus(visibility)
    val title = parsed?.title?.takeIf { it.isNotBlank() }
        ?: note.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }
        ?: "Медицинская запись #$id"
    val notesText = parsed?.notes?.takeIf { it.isNotBlank() } ?: note
    val category = parsed?.category?.takeIf { it.isNotBlank() } ?: "Общее"
    val doctorName = parsed?.doctorName?.takeIf { it.isNotBlank() } ?: "Врач #$doctorId"
    val specialty = parsed?.doctorSpecialty?.takeIf { it.isNotBlank() }

    return PatientRecordEntry(
        id = id,
        title = title,
        doctorName = doctorName,
        doctorSpecialty = specialty,
        createdAt = createdAt,
        displayDate = formatRecordDate(updatedAt ?: createdAt),
        category = category,
        status = status,
        notes = notesText,
    )
}

fun Container.patientMedicalRecordsScreen(onLogout: () -> Unit = { Navigator.showHome() }) = vPanel(spacing = 12) {
    val uiScope = MainScope()
    val apiClient = PatientApiClient()

    var isLoading = false
    var errorMessage: String? = null
    var records: List<PatientRecordEntry> = emptyList()
    var cachedClientId: Long? = null

    var loadRecords: ((Boolean) -> Unit)? = null

    headerBar(
        mode = HeaderMode.PATIENT,
        active = NavTab.PROFILE,
        onLogout = {
            ApiConfig.clearToken()
            Session.clear()
            Navigator.showHome()
            uiScope.cancel()
        }
    )

    patientAccountLayout(active = PatientSection.MEDICAL_RECORDS, onLogout = onLogout) {
        h1("Медицинские записи", className = "account title")

        val recordsContainer = vPanel(spacing = 12, className = "doctor-records-list").apply {
            width = 100.perc
        }

        fun renderRecords() {
            recordsContainer.removeAll()
            when {
                isLoading -> {
                    recordsContainer.div(className = "doctor-record-card card") {
                        p("Загрузка медицинских записей...", className = "doctor-record-notes")
                    }
                }

                errorMessage != null -> {
                    recordsContainer.div(className = "doctor-record-card card") {
                        p(errorMessage ?: "Ошибка", className = "doctor-record-notes")
                        button("Повторить", className = "btn-ghost-sm").onClick {
                            errorMessage = null
                            loadRecords?.invoke(true)
                        }
                    }
                }

                records.isEmpty() -> {
                    recordsContainer.div(className = "doctor-record-card card") {
                        p("Нет медицинских записей", className = "doctor-record-notes")
                    }
                }

                else -> {
                    records.forEach { record ->
                        recordsContainer.div(className = "doctor-record-card card") {
                            div(className = "doctor-record-body") {
                                h4(record.title, className = "doctor-record-title")
                                val doctorSubtitle = listOfNotNull(
                                    listOfNotNull(record.doctorName, record.doctorSpecialty)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" • ")
                                        .takeIf { it.isNotBlank() },
                                    record.displayDate
                                ).joinToString(" • ")

                                span(doctorSubtitle, className = "doctor-record-subtitle")
                                p(record.notes, className = "doctor-record-notes")
                            }

                            div(className = "doctor-record-meta") {
                                span(record.category, className = "doctor-record-tag")
                                span(
                                    record.status.label,
                                    className = "doctor-record-status ${record.status.cssClass}"
                                )
                            }
                        }
                    }
                }
            }
        }

        // 👇 тут меняем лямбду на анонимную функцию
        loadRecords = fun(force: Boolean) {
            if (isLoading && !force) return

            val userId = Session.userId
            if (userId == null) {
                errorMessage = "Необходима авторизация"
                renderRecords()
                return
            }

            isLoading = true
            errorMessage = null
            renderRecords()

            uiScope.launch {
                val clientId = cachedClientId ?: apiClient.getClientId(userId).getOrElse { error ->
                    errorMessage = error.message ?: "Не удалось определить профиль пациента"
                    Toast.danger(errorMessage ?: "Ошибка загрузки")
                    null
                }

                if (clientId == null) {
                    isLoading = false
                    renderRecords()
                    return@launch
                }

                cachedClientId = clientId
                val result = apiClient.listMedicalRecords(clientId, includeInternal = true)
                result.fold(
                    onSuccess = { records },
                    onFailure = { error ->
                        errorMessage = error.message ?: "Не удалось загрузить медицинские записи"
                        Toast.danger(errorMessage ?: "Ошибка загрузки")
                    }
                )

                isLoading = false
                renderRecords()
            }
        }

        div(className = "card block") {
            add(recordsContainer)
        }

        loadRecords.invoke(false)
    }
}