package ui

import api.ApiConfig
import api.PatientApiClient
import io.kvision.core.Container
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.panel.vPanel
import io.kvision.toast.Toast
import io.kvision.utils.perc
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.interns.project.dto.MedicalRecordDto

fun Container.patientMedicalRecordsScreen(onLogout: () -> Unit = { Navigator.showHome() }) = vPanel(spacing = 12) {
    val uiScope = MainScope()
    val apiClient = PatientApiClient()

    var isLoading = false
    var errorMessage: String? = null
    var records: List<MedicalRecordDto> = emptyList()

    var loadRecords: ((Boolean) -> Unit)? = null

    headerBar(
        mode = HeaderMode.PATIENT,
        active = NavTab.NONE,
        onLogout = {
            ApiConfig.clearToken()
            Session.clear()
            Navigator.showHome()
            uiScope.cancel()
        }
    )

    patientAccountLayout(active = PatientSection.MEDICAL_RECORDS, onLogout = onLogout) {
        h1("Медицинские записи", className = "account title")

        val recordsContainer = vPanel(spacing = 8, className = "records list").apply {
            width = 100.perc
        }

        fun renderRecords() {
            recordsContainer.removeAll()
            when {
                isLoading -> {
                    recordsContainer.div(className = "record item card") {
                        p("Загрузка медицинских записей...", className = "record title")
                    }
                }

                errorMessage != null -> {
                    recordsContainer.div(className = "record item card") {
                        p(errorMessage ?: "Ошибка", className = "record title")
                        button("Повторить", className = "btn-ghost-sm").onClick {
                            errorMessage = null
                            loadRecords?.invoke(true)
                        }
                    }
                }

                records.isEmpty() -> {
                    recordsContainer.div(className = "record item card") {
                        p("Нет медицинских записей", className = "record title")
                    }
                }

                else -> {
                    records.forEach { record ->
                        recordsContainer.div(className = "record item card") {
                            div(className = "record-row") {
                                span("Запись #${record.id}", className = "record title")
                                span(record.createdAt, className = "record date")
                            }
                            record.diagnosis?.takeIf { it.isNotBlank() }?.let { diagnosis ->
                                p("Диагноз: $diagnosis")
                            }
                            record.symptoms?.takeIf { it.isNotBlank() }?.let { symptoms ->
                                p("Симптомы: $symptoms")
                            }
                            record.treatment?.takeIf { it.isNotBlank() }?.let { treatment ->
                                p("Лечение: $treatment")
                            }
                            record.recommendations?.takeIf { it.isNotBlank() }?.let { recommendations ->
                                p("Рекомендации: $recommendations")
                            }
                        }
                    }
                }
            }
        }

        // 👇 тут меняем лямбду на анонимную функцию
        loadRecords = fun(force: Boolean) {
            if (isLoading && !force) return

            val patientId = Session.userId
            if (patientId == null) {
                errorMessage = "Необходима авторизация"
                renderRecords()
                return
            }

            isLoading = true
            errorMessage = null
            renderRecords()

            uiScope.launch {
                val result = apiClient.getMedicalRecords(patientId)
                result.fold(
                    onSuccess = { list ->
                        records = list
                    },
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
