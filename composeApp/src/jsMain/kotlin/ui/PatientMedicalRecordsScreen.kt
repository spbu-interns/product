package ui

import api.ApiConfig
import api.PatientApiClient
import io.kvision.core.Container
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.h3
import io.kvision.html.h4
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.modal.Modal
import io.kvision.modal.ModalSize
import io.kvision.panel.vPanel
import io.kvision.toast.Toast
import io.kvision.utils.perc
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.interns.project.dto.DoctorNoteResponse
import org.interns.project.dto.MedicalRecordOutDto
import org.interns.project.dto.NoteVisibilityDto
import utils.downloadPdf

private enum class PatientRecordStatus(val label: String, val cssClass: String) {
    NORMAL("Нормальный", "status-normal"),
    REVIEWED("Просмотрено", "status-reviewed"),
    ATTENTION("Требует внимания", "status-attention"),
}

private enum class MedicalRecordStatus(val label: String, val cssClass: String) {
    NORMAL("Нормальный", "status-normal"),
    REVIEWED("Просмотрено", "status-reviewed"),
    ATTENTION("Требует внимания", "status-attention"),
    DIAGNOSIS("Диагностика", "status-diagnosis"),
    TREATMENT("Лечение", "status-treatment"),
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

private data class MedicalRecordEntry(
    val id: Long,
    val clientId: Long,
    val doctorId: Long?,
    val appointmentId: Long?,
    val diagnosis: String?,
    val symptoms: String?,
    val treatment: String?,
    val recommendations: String?,
    val createdAt: String,
    val updatedAt: String?,
    val displayDate: String,
    val status: MedicalRecordStatus,
    val title: String,
    val doctorName: String,
    val doctorSpecialty: String?,
    val category: String
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

private fun determineMedicalRecordStatus(dto: MedicalRecordOutDto): MedicalRecordStatus {
    return when {
        dto.diagnosis.isNullOrBlank() && dto.symptoms.isNullOrBlank() -> MedicalRecordStatus.ATTENTION
        dto.treatment.isNullOrBlank() && !dto.diagnosis.isNullOrBlank() -> MedicalRecordStatus.DIAGNOSIS
        !dto.treatment.isNullOrBlank() -> MedicalRecordStatus.TREATMENT
        else -> MedicalRecordStatus.NORMAL
    }
}

private fun generateMedicalRecordTitle(dto: MedicalRecordOutDto): String {
    return when {
        !dto.diagnosis.isNullOrBlank() -> "Диагноз: ${dto.diagnosis!!.take(50)}${if (dto.diagnosis!!.length > 50) "..." else ""}"
        !dto.symptoms.isNullOrBlank() -> "Симптомы: ${dto.symptoms!!.take(50)}${if (dto.symptoms!!.length > 50) "..." else ""}"
        !dto.treatment.isNullOrBlank() -> "Лечение: ${dto.treatment!!.take(50)}${if (dto.treatment!!.length > 50) "..." else ""}"
        else -> "Медицинская запись #${dto.id}"
    }
}

private fun determineMedicalRecordCategory(dto: MedicalRecordOutDto): String {
    return when {
        !dto.diagnosis.isNullOrBlank() && !dto.treatment.isNullOrBlank() -> "Диагноз и лечение"
        !dto.diagnosis.isNullOrBlank() -> "Диагноз"
        !dto.symptoms.isNullOrBlank() -> "Симптомы"
        !dto.treatment.isNullOrBlank() -> "Лечение"
        !dto.recommendations.isNullOrBlank() -> "Рекомендации"
        else -> "Общее"
    }
}

private fun MedicalRecordOutDto.toMedicalRecordEntry(): MedicalRecordEntry {
    val status = determineMedicalRecordStatus(this)
    val title = generateMedicalRecordTitle(this)
    val category = determineMedicalRecordCategory(this)
    val doctorName = doctorId?.let { "Врач #$it" } ?: "Не указан"

    return MedicalRecordEntry(
        id = id,
        clientId = clientId,
        doctorId = doctorId,
        appointmentId = appointmentId,
        diagnosis = diagnosis,
        symptoms = symptoms,
        treatment = treatment,
        recommendations = recommendations,
        createdAt = createdAt,
        updatedAt = updatedAt,
        displayDate = formatRecordDate(updatedAt ?: createdAt),
        status = status,
        title = title,
        doctorName = doctorName,
        doctorSpecialty = null,
        category = category
    )
}

fun Container.patientMedicalRecordsScreen(onLogout: () -> Unit = { Navigator.showHome() }) = vPanel(spacing = 12) {
    val uiScope = MainScope()
    val apiClient = PatientApiClient()

    var isLoading = false
    var errorMessage: String? = null
    var medicalRecords: List<MedicalRecordEntry> = emptyList()
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

        val recordsContainer = vPanel(spacing = 12, className = "medical-records-container").apply {
            width = 100.perc
        }

        fun showMedicalRecordDetails(record: MedicalRecordEntry) {
            val modal = Modal(
                caption = "Детали медицинской записи",
                closeButton = true,
                size = ModalSize.LARGE
            ) {
                div(className = "medical-record-details") {
                    div(className = "details-header") {
                        h3(record.title, className = "details-title")
                        span(record.displayDate, className = "details-date")
                        span(record.status.label, className = "details-status ${record.status.cssClass}")
                    }

                    div(className = "details-sections") {
                        record.diagnosis?.takeIf { it.isNotBlank() }?.let { diagnosis ->
                            div(className = "details-section") {
                                h4("Диагноз", className = "section-title")
                                p(diagnosis, className = "section-content")
                            }
                        }

                        record.symptoms?.takeIf { it.isNotBlank() }?.let { symptoms ->
                            div(className = "details-section") {
                                h4("Симптомы", className = "section-title")
                                p(symptoms, className = "section-content")
                            }
                        }

                        record.treatment?.takeIf { it.isNotBlank() }?.let { treatment ->
                            div(className = "details-section") {
                                h4("Лечение", className = "section-title")
                                p(treatment, className = "section-content")
                            }
                        }

                        record.recommendations?.takeIf { it.isNotBlank() }?.let { recommendations ->
                            div(className = "details-section") {
                                h4("Рекомендации", className = "section-title")
                                p(recommendations, className = "section-content")
                            }
                        }
                    }

                    div(className = "details-meta") {
                        div(className = "meta-grid") {
                            div(className = "meta-item") {
                                span("ID записи:", className = "meta-label")
                                span(record.id.toString(), className = "meta-value")
                            }

                            div(className = "meta-item") {
                                span("Категория:", className = "meta-label")
                                span(record.category, className = "meta-value")
                            }

                            div(className = "meta-item") {
                                span("Врач:", className = "meta-label")
                                span(record.doctorName, className = "meta-value")
                            }

                            record.doctorId?.let { doctorId ->
                                div(className = "meta-item") {
                                    span("ID врача:", className = "meta-label")
                                    span(doctorId.toString(), className = "meta-value")
                                }
                            }

                            record.appointmentId?.let { appointmentId ->
                                div(className = "meta-item") {
                                    span("ID приема:", className = "meta-label")
                                    span(appointmentId.toString(), className = "meta-value")
                                }
                            }

                            div(className = "meta-item") {
                                span("Создано:", className = "meta-label")
                                span(formatRecordDate(record.createdAt), className = "meta-value")
                            }

                            record.updatedAt?.let { updatedAt ->
                                div(className = "meta-item") {
                                    span("Обновлено:", className = "meta-label")
                                    span(formatRecordDate(updatedAt), className = "meta-value")
                                }
                            }
                        }
                    }

                    div(className = "details-actions") {
                        button("Закрыть", className = "btn-secondary") {
                            onClick { this.hide() }
                        }
                        button("Скачать PDF", className = "btn-primary") {
                            onClick {
                                uiScope.launch {
                                    apiClient.downloadMedicalRecordPdf(record.clientId, record.id)
                                        .onSuccess { bytes ->
                                            downloadPdf(
                                                bytes = bytes,
                                                filename = "medical_record_${record.id}.pdf"
                                            )
                                        }
                                        .onFailure { error ->
                                            Toast.danger(error.message ?: "Не удалось скачать PDF")
                                        }
                                    hide()
                                }
                            }
                        }
                    }
                }
            }
            modal.show()
        }

        fun downloadMedicalRecordPdf(clientId: Long, recordId: Long) {
            uiScope.launch {
                Toast.info("Формирование PDF...")
                apiClient.downloadMedicalRecordPdf(clientId, recordId)
                    .onSuccess { bytes ->
                        downloadPdf(
                            bytes = bytes,
                            filename = "medical_record_${recordId}.pdf"
                        )
                    }
                    .onFailure { error ->
                        Toast.danger(error.message ?: "Не удалось скачать PDF")
                    }
            }
        }

        fun renderMedicalRecords() {
            recordsContainer.removeAll()
            when {
                isLoading -> {
                    recordsContainer.div(className = "medical-record-card card") {
                        p("Загрузка медицинских записей...", className = "medical-record-content")
                    }
                }

                errorMessage != null -> {
                    recordsContainer.div(className = "medical-record-card card") {
                        p(errorMessage ?: "Ошибка", className = "medical-record-content")
                        button("Повторить", className = "btn-ghost-sm").onClick {
                            errorMessage = null
                            loadRecords?.invoke(true)
                        }
                    }
                }

                medicalRecords.isEmpty() -> {
                    recordsContainer.div(className = "medical-record-card card") {
                        p("Нет медицинских записей", className = "medical-record-content")
                    }
                }

                else -> {
                    medicalRecords.forEach { record ->
                        recordsContainer.div(className = "medical-record-card card") {
                            div(className = "medical-record-header") {
                                h4(record.title, className = "medical-record-title")
                                span(record.displayDate, className = "medical-record-date")
                                span(record.status.label, className = "medical-record-status ${record.status.cssClass}")
                            }

                            div(className = "medical-record-body") {
                                record.diagnosis?.takeIf { it.isNotBlank() }?.let { diagnosis ->
                                    div(className = "record-field") {
                                        span("Диагноз: ", className = "field-label")
                                        span(diagnosis, className = "field-value")
                                    }
                                }

                                record.symptoms?.takeIf { it.isNotBlank() }?.let { symptoms ->
                                    div(className = "record-field") {
                                        span("Симптомы: ", className = "field-label")
                                        span(symptoms, className = "field-value")
                                    }
                                }

                                record.treatment?.takeIf { it.isNotBlank() }?.let { treatment ->
                                    div(className = "record-field") {
                                        span("Лечение: ", className = "field-label")
                                        span(treatment, className = "field-value")
                                    }
                                }
                            }

                            div(className = "medical-record-meta") {
                                span(record.category, className = "medical-record-tag")
                                span(record.doctorName, className = "medical-record-doctor")

                                button("Подробнее", className = "btn-ghost-sm").onClick {
                                    showMedicalRecordDetails(record)
                                }

                                button("Скачать PDF", className = "btn-ghost-sm").onClick {
                                    downloadMedicalRecordPdf(record.clientId, record.id)
                                }
                            }
                        }
                    }
                }
            }
        }

        loadRecords = fun(force: Boolean) {
            if (isLoading && !force) return

            val userId = Session.userId
            if (userId == null) {
                errorMessage = "Необходима авторизация"
                renderMedicalRecords()
                return
            }

            isLoading = true
            errorMessage = null
            renderMedicalRecords()

            uiScope.launch {
                val clientId = cachedClientId ?: apiClient.getClientId(userId).getOrElse { error ->
                    errorMessage = error.message ?: "Не удалось определить профиль пациента"
                    Toast.danger(errorMessage ?: "Ошибка загрузки")
                    null
                }

                if (clientId == null) {
                    isLoading = false
                    renderMedicalRecords()
                    return@launch
                }

                cachedClientId = clientId
                val result = apiClient.getMedicalRecords(clientId)
                result.fold(
                    onSuccess = { recordsList ->
                        medicalRecords = recordsList
                            .map { it.toMedicalRecordEntry() }
                            .sortedByDescending { it.createdAt }
                    },
                    onFailure = { error ->
                        errorMessage = error.message ?: "Не удалось загрузить медицинские записи"
                        Toast.danger(errorMessage ?: "Ошибка загрузки")
                    }
                )

                isLoading = false
                renderMedicalRecords()
            }
        }

        div(className = "card block") {
            add(recordsContainer)
        }

        loadRecords.invoke(false)
    }
}