package ui

import api.ApiConfig
import api.PatientApiClient
import io.kvision.core.Container
import io.kvision.core.FlexDirection
import io.kvision.core.Overflow
import io.kvision.form.text.Text
import io.kvision.form.text.text
import io.kvision.form.text.textArea
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h4
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.panel.vPanel
import io.kvision.toast.Toast
import io.kvision.utils.perc
import io.kvision.utils.px
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.interns.project.dto.ComplaintCreateRequest
import org.interns.project.dto.ComplaintResponse
import ui.components.complaintCard
import ui.PatientSection
import ui.patientAccountLayout

fun Container.myRecordsScreen(
    onLogout: () -> Unit = { Navigator.showHome() }
) = vPanel(spacing = 12) {
    val uiScope = MainScope()
    headerBar(
        mode = HeaderMode.PATIENT,
        active = NavTab.NONE,
        onLogout = {
            ApiConfig.clearToken()
            Session.clear()
            onLogout()
            uiScope.cancel()
        }
    )
    val patientId = Session.userId
    if (patientId == null) {
        div(className = "account container") {
            div(className = "card block") {
                h4("Пожалуйста, войдите", className = "block title")
                p("Для просмотра записей необходимо выполнить вход в систему.")
            }
        }
        return@vPanel
    }

    val apiClient = PatientApiClient()
    var currentComplaints: List<ComplaintResponse> = emptyList()

    fun renderComplaints(container: Container, items: List<ComplaintResponse>) {
        container.removeAll()
        if (items.isEmpty()) {
            container.div(className = "record item card") {
                div(className = "record-row") {
                    span("Пока нет записей", className = "record title")
                }
            }
        } else {
            items.forEach {
                container.complaintCard(it) { comp ->
                    Navigator.showRecordEditor(comp.id.toString())
                }
            }
        }
    }

    val complaintsContainer = vPanel(spacing = 8, className = "records list").apply {
        width = 100.perc
        flexDirection = FlexDirection.COLUMNREV
    }

    fun loadComplaints() {
        complaintsContainer.removeAll()
        complaintsContainer.div(className = "record item card") {
            div(className = "record-row") { span("Загрузка...") }
        }
        uiScope.launch {
            val result = apiClient.listComplaints(patientId)
            result.fold(
                onSuccess = { list ->
                    currentComplaints = list.sortedByDescending { it.createdAt }
                    complaintsContainer.removeAll()
                    renderComplaints(complaintsContainer, currentComplaints)
                },
                onFailure = { error ->
                    complaintsContainer.removeAll()
                    complaintsContainer.div(className = "record item card") {
                        div(className = "record-row") {
                            span("Не удалось загрузить записи", className = "record title")
                            span(error.message ?: "Ошибка", className = "record date")
                        }
                    }
                    Toast.danger("Не удалось загрузить записи: ${error.message}")
                }
            )
        }
    }

    patientAccountLayout(active = PatientSection.MY_RECORDS, onLogout = onLogout) {
        val titleInput: Text = text(label = "Заголовок") {
            placeholder = "Например, боль в спине"
            addCssClass("kv-input")
        }

        val bodyInput = textArea(label = "Описание жалобы") {
            placeholder = "Опишите проблему"
            addCssClass("kv-input")
            addCssClass("complaint-body")
            height = 120.px
            setAttribute("style", "resize: vertical;")
        }

        val addButton = button("Добавить", className = "btn-primary")
        val formError = span("").apply { addCssClass("text-danger") }

        div(className = "card block") {
            h4("Добавить жалобу", className = "block title")
            div(className = "form column") {
                div(className = "form field") { add(titleInput) }
                div(className = "form field") { add(bodyInput) }
                div(className = "form actions") {
                    add(formError)
                    add(addButton)
                }
            }
        }

        addButton.onClick {
            val title = titleInput.value?.trim().orEmpty()
            val body = bodyInput.value?.trim().orEmpty()
            if (title.isBlank()) {
                Toast.warning("Введите заголовок")
                return@onClick
            }
            if (body.isBlank()) {
                Toast.warning("Введите жалобу")
                return@onClick
            }
            addButton.disabled = true
            uiScope.launch {
                val result = apiClient.createComplaint(patientId, ComplaintCreateRequest(title, body))
                result.fold(
                    onSuccess = { complaint ->
                        titleInput.value = ""
                        bodyInput.value = ""
                        addButton.disabled = false
                        currentComplaints = listOf(complaint) + currentComplaints
                        complaintsContainer.removeAll()
                        renderComplaints(complaintsContainer, currentComplaints)
                        Toast.success("Жалоба добавлена")
                    },
                    onFailure = { error ->
                        addButton.disabled = false
                        Toast.danger("Не удалось добавить жалобу")
                    }
                )
            }
        }

        h4("Мои жалобы", className = "block title")
        div(className = "card block") {
            div(className = "records scrollbox") {
                maxHeight = 320.px
                overflowY = Overflow.AUTO
                overflowX = Overflow.HIDDEN
                width = 100.perc
                paddingRight = 6.px
                add(complaintsContainer)
            }
        }
    }

    loadComplaints()
}
