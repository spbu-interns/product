package ui

import api.ApiConfig
import api.PatientApiClient
import io.kvision.core.*
import io.kvision.form.text.text
import io.kvision.html.Button
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.span
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel
import io.kvision.toast.Toast
import io.kvision.utils.perc
import io.kvision.utils.px
import io.kvision.html.Div
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ui.PatientSection
import ui.components.patientSidebar
import org.interns.project.dto.ComplaintPatchRequest
import org.interns.project.dto.ComplaintResponse


fun Container.recordEditorScreen(recordId: String, onBack: () -> Unit) = vPanel(spacing = 12) {
    val uiScope = MainScope()
    headerBar(mode = HeaderMode.PATIENT, active = NavTab.PROFILE, onLogout = {
        ApiConfig.clearToken()
        Session.clear()
        Navigator.showHome()
        uiScope.cancel()
    })

    val patientId = Session.userId
    val complaintId = recordId.toLongOrNull()
    if (patientId == null || complaintId == null) {
        div(className = "card block") { +"Запись не найдена" }
        return@vPanel
    }
    val apiClient = PatientApiClient()
    div(className = "account container") {
        div(className = "account grid") {
            patientSidebar(
                patientId = patientId,
                active = PatientSection.MY_RECORDS,
                onOverview = { Navigator.showPatient() },
                onAppointments = { Navigator.showAppointments() },
                onMedicalRecords = { Navigator.showPatientMedicalRecords() },
                onMyRecords = { Navigator.showMyRecords() },
                onFindDoctor = { Navigator.showFind() },
                onProfile = { Navigator.showPatientProfileEdit() }
            )

            div(className = "main column") {
                h1("Редактирование жалобы", className = "account title")

                val titleInput = text(label = "Заголовок")

                val editor = div(className = "custom-editor", rich = true).apply {
                    width = 100.perc
                    height = 420.px
                    border = Border(1.px, BorderStyle.SOLID, Color.name(Col.LIGHTGRAY))
                    borderRadius = 8.px
                    padding = 12.px
                    background = Background(Color.hex(0xFFFFFF))
                    overflowY = Overflow.AUTO
                    setAttribute("contenteditable", "true")
                    content = "Загрузка..."
                }

                var btnBold: Button? = null
                var btnItalic: Button? = null
                var btnUnderline: Button? = null

                fun Button?.setActive(active: Boolean) {
                    this?.let {
                        if (active) {
                            it.addCssClass("btn-toolbar-active")
                        } else {
                            it.removeCssClass("btn-toolbar-active")
                        }
                    }
                }

                fun syncToolbar() {
                    val b = document.queryCommandState("bold")
                    val i = document.queryCommandState("italic")
                    val u = document.queryCommandState("underline")
                    btnBold.setActive(b == true)
                    btnItalic.setActive(i == true)
                    btnUnderline.setActive(u == true)
                }

                fun applyCmd(cmd: String) {
                    editor.getElement()?.unsafeCast<org.w3c.dom.HTMLElement>()?.focus()
                    document.execCommand(cmd, false, "")
                    window.setTimeout({ syncToolbar() }, 0)
                }

                div(className = "card block") {
                    add(titleInput)

                    hPanel(className = "toolbar", spacing = 8) {
                        justifyContent = JustifyContent.SPACEBETWEEN
                        alignItems = AlignItems.CENTER

                        span("Описание", className = "kv-form-label").apply {
                            fontWeight = FontWeight.NORMAL
                        }
                        hPanel(spacing = 8) {
                            btnBold = button("B", className = "btn-toolbar").apply {
                                fontWeight = FontWeight.BOLD
                                onClick { applyCmd("bold") }
                            }
                            btnItalic = button("I", className = "btn-toolbar").apply {
                                fontStyle = FontStyle.ITALIC
                                onClick { applyCmd("italic") }
                            }
                            btnUnderline = button("U", className = "btn-toolbar text-underline").apply {
                                onClick { applyCmd("underline") }
                            }
                        }
                    }

                    add(editor)
                }

                val errorLabel = span("").apply {
                    addCssClass("text-danger")
                    display = Display.NONE
                }
                add(errorLabel)

                editor.onEvent {
                    keyup = { _ -> syncToolbar() }
                    mouseup = { _ -> syncToolbar() }
                    input = { syncToolbar() }
                }

                val selectionHandler: (dynamic) -> Unit = { syncToolbar() }

                document.addEventListener("selectionchange", selectionHandler)
                syncToolbar()

                val saveButton = button("Сохранить", className = "btn-primary")
                val deleteButton = button("Удалить", className = "btn-danger")
                val backButton = button("Назад", className = "btn")

                hPanel(spacing = 8) {
                    add(saveButton)
                    add(deleteButton)
                    add(backButton)
                }

                fun populateComplaint(complaint: ComplaintResponse) {
                    titleInput.value = complaint.title
                    setEditorHtml(editor, complaint.body)
                }

                fun loadComplaint() {
                    uiScope.launch {
                        val result = apiClient.listComplaints(patientId)
                        result.fold(
                            onSuccess = { list ->
                                val found = list.firstOrNull { it.id == complaintId }
                                if (found != null) {
                                    populateComplaint(found)
                                } else {
                                    errorLabel.content = "Запись не найдена"
                                }
                            },
                            onFailure = { error ->
                                errorLabel.content = error.message ?: "Ошибка загрузки"
                                Toast.danger(errorLabel.content ?: "Ошибка")
                            }
                        )
                    }
                }

                saveButton.onClick {
                    val title = titleInput.value?.trim().orEmpty()
                    val body = getEditorHtml(editor)

                    if (title.isBlank()) {
                        Toast.danger("Введите заголовок")
                        return@onClick
                    }
                    if (body.isBlank()) {
                        Toast.danger("Введите описание")
                        return@onClick
                    }
                    errorLabel.content = ""
                    saveButton.disabled = true

                    uiScope.launch {
                        val result = apiClient.updateComplaint(
                            complaintId,
                            ComplaintPatchRequest(title = title, body = body)
                        )
                        result.fold(
                            onSuccess = { updated ->
                                saveButton.disabled = false
                                populateComplaint(updated)
                                Toast.success("Сохранено")
                                document.removeEventListener("selectionchange", selectionHandler)
                                uiScope.cancel()
                                Navigator.showMyRecords()
                            },
                            onFailure = { error ->
                                saveButton.disabled = false
                                errorLabel.content = error.message ?: "Не удалось сохранить"
                                Toast.danger(errorLabel.content ?: "Ошибка")
                            }
                        )
                    }
                }

                deleteButton.onClick {
                    deleteButton.disabled = true
                    uiScope.launch {
                        val result = apiClient.deleteComplaint(complaintId)
                        result.fold(
                            onSuccess = { deleted ->
                                deleteButton.disabled = false
                                if (deleted) {
                                    Toast.success("Жалоба удалена")
                                    document.removeEventListener("selectionchange", selectionHandler)
                                    uiScope.cancel()
                                    Navigator.showMyRecords()
                                } else {
                                    errorLabel.content = "Жалоба не найдена"
                                }
                            },
                            onFailure = { error ->
                                deleteButton.disabled = false
                                errorLabel.content = error.message ?: "Не удалось удалить"
                                Toast.danger(errorLabel.content ?: "Ошибка")
                            }
                        )
                    }
                }

                backButton.onClick {
                    onBack()
                    document.removeEventListener("selectionchange", selectionHandler)
                    uiScope.cancel()
                }

                loadComplaint()
            }
        }
    }
}

private fun getEditorHtml(div: Div): String =
    div.getElement()?.unsafeCast<org.w3c.dom.HTMLElement>()?.innerHTML?.trim().orEmpty()

private fun setEditorHtml(div: Div, html: String) {
    div.getElement()?.unsafeCast<org.w3c.dom.HTMLElement>()?.innerHTML = html
}