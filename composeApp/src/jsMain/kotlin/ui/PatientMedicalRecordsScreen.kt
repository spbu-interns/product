package ui

import api.ApiConfig
import api.PatientApiClient
import io.kvision.core.Container
import io.kvision.core.JustifyContent
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.h3
import io.kvision.html.h4
import io.kvision.html.p
import io.kvision.panel.vPanel
import io.kvision.html.span
import io.kvision.modal.Modal
import io.kvision.modal.ModalSize
import io.kvision.toast.Toast
import io.kvision.utils.perc
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.interns.project.dto.MedicalRecordOutDto
import utils.downloadPdf

private enum class MedicalRecordStatus(val label: String, val cssClass: String) {
    NORMAL("Доступно", "status-normal"),
    DIAGNOSIS("Диагноз", "status-diagnosis"),
    TREATMENT("Лечение", "status-treatment"),
    ATTENTION("Внимание", "status-attention"),
}

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

private fun determineMedicalRecordStatus(dto: MedicalRecordOutDto): MedicalRecordStatus {
    return when {
        dto.diagnosis.isNullOrBlank() && dto.symptoms.isNullOrBlank() -> MedicalRecordStatus.ATTENTION
        dto.treatment.isNullOrBlank() && !dto.diagnosis.isNullOrBlank() -> MedicalRecordStatus.DIAGNOSIS
        !dto.treatment.isNullOrBlank() -> MedicalRecordStatus.TREATMENT
        else -> MedicalRecordStatus.NORMAL
    }
}

private fun generateMedicalRecordTitle(dto: MedicalRecordOutDto): String {
    val formattedDate = extractDate(dto.updatedAt ?: dto.createdAt)
    return "Запись от $formattedDate"
}

fun extractDate(date: String?): String {
    val parts = date?.substringBefore("T")?.split("-")
    return if (parts != null && parts.size >= 3) {
        "${parts[2]}.${parts[1]}.${parts[0]}"
    } else {
        "неизвестная дата"
    }
}

private fun MedicalRecordOutDto.toMedicalRecordEntry(): MedicalRecordEntry {
    val status = determineMedicalRecordStatus(this)
    val title = generateMedicalRecordTitle(this)
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
        category = "Медицинская запись"
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
        div(className = "patient-records-container") {
            h1("Медицинские записи", className = "account title")
            val recordsContainer = vPanel(spacing = 12, className = "patient-records-list").apply {
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
                            button("Скачать PDF", className = "btn-primary") {
                                onClick {
                                    uiScope.launch {
                                        apiClient.downloadMedicalRecordPdf(record.clientId, record.id)
                                            .onSuccess { bytes ->
                                                downloadPdf(bytes, "medical_record_${record.createdAt}.pdf")
                                                Toast.success("PDF успешно скачан")
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
                        recordsContainer.div(className = "patient-record-card card") {
                            p("Загрузка медицинских записей...", className = "patient-record-notes")
                        }
                    }

                    errorMessage != null -> {
                        recordsContainer.div(className = "patient-record-card card") {
                            p(errorMessage ?: "Ошибка", className = "patient-record-notes")
                            button("Повторить", className = "btn-ghost-sm").onClick {
                                errorMessage = null
                                loadRecords?.invoke(true)
                            }
                        }
                    }

                    medicalRecords.isEmpty() -> {
                        recordsContainer.div(className = "patient-record-card card") {
                            p("Нет медицинских записей", className = "patient-record-notes")
                        }
                    }

                    else -> {
                        medicalRecords.forEach { record ->
                            recordsContainer.div(className = "patient-record-card card") {
                                // HEADER с заголовком и датой
                                div(className = "patient-record-header") {
                                    h4(record.title, className = "patient-record-title")
                                    span(record.displayDate, className = "patient-record-date")
                                }

                                // BODY с основным содержимым
                                div(className = "patient-record-body") {
                                    // Диагноз (если есть)
                                    record.diagnosis?.takeIf { it.isNotBlank() }?.let { diagnosis ->
                                        p("Диагноз: $diagnosis", className = "patient-record-notes")
                                    }

                                    // Симптомы (если есть)
                                    record.symptoms?.takeIf { it.isNotBlank() }?.let { symptoms ->
                                        p("Симптомы: $symptoms", className = "patient-record-notes")
                                    }

                                    // Лечение (если есть)
                                    record.treatment?.takeIf { it.isNotBlank() }?.let { treatment ->
                                        p("Лечение: $treatment", className = "patient-record-notes")
                                    }

                                    // Рекомендации (если есть)
                                    record.recommendations?.takeIf { it.isNotBlank() }?.let { recommendations ->
                                        p("    Рекомендации: $recommendations", className = "patient-record-notes")
                                    }
                                }

                                // ACTIONS с кнопками
                                div(className = "patient-record-actions") {
                                    justifyContent = JustifyContent.SPACEBETWEEN

                                    // Справа - кнопки действий
                                    div(className = "patient-record-buttons") {
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
            }

            div(className = "patient-records-content") {
                add(recordsContainer)
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

            loadRecords.invoke(false)
        }
    }
}