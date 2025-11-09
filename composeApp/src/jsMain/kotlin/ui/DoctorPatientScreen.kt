package ui

import io.kvision.core.Container
import io.kvision.core.JustifyContent
import io.kvision.core.onClick
import io.kvision.form.select.Select
import io.kvision.form.text.Text
import io.kvision.form.text.text
import io.kvision.form.text.textArea
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.Li
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
import kotlin.js.Date

private enum class DoctorPatientTab { OVERVIEW, RECORDS }

private enum class DoctorRecordStatus(val label: String, val cssClass: String) {
    NORMAL("Normal", "status-normal"),
    REVIEWED("Reviewed", "status-reviewed"),
    ATTENTION("Needs Attention", "status-attention")
}

private data class DoctorRecordEntry(
    val id: String,
    val title: String,
    val doctorName: String,
    val createdAt: String,
    val category: String,
    val status: DoctorRecordStatus,
    val notes: String
)

private data class DoctorPatientProfile(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val age: Int,
    val heightCm: Int,
    val weightKg: Int,
    val phone: String,
    val email: String,
    val summary: String,
    val conditions: List<String>,
    val medications: List<String>,
    val allergies: List<String>,
    val records: List<DoctorRecordEntry>
) {
    val initials: String
        get() = listOf(firstName.firstOrNull(), lastName.firstOrNull())
            .filterNotNull()
            .joinToString("")
            .ifBlank { "PT" }

    val fullName: String
        get() = "$firstName $lastName"
}

private val sampleDoctorPatients = listOf(
    DoctorPatientProfile(
        id = 101,
        firstName = "John",
        lastName = "Smith",
        age = 45,
        heightCm = 178,
        weightKg = 82,
        phone = "+1 (555) 213-8890",
        email = "john.smith@email.com",
        summary = "Patient is undergoing regular cardiovascular monitoring with stable results over the last quarter.",
        conditions = listOf("Hypertension", "Cholesterol (borderline)"),
        medications = listOf("Lisinopril 10mg", "Atorvastatin 20mg"),
        allergies = listOf("Penicillin"),
        records = listOf(
            DoctorRecordEntry(
                id = "1",
                title = "Blood Test Report",
                doctorName = "Dr. Emily Rodriguez",
                createdAt = "August 15, 2025",
                category = "Lab Results",
                status = DoctorRecordStatus.NORMAL,
                notes = "Routine blood panel shows all values within normal range. Continue current medication plan."
            ),
            DoctorRecordEntry(
                id = "2",
                title = "Knee X-Ray",
                doctorName = "Dr. David Wilson",
                createdAt = "July 10, 2025",
                category = "X-Ray",
                status = DoctorRecordStatus.REVIEWED,
                notes = "No acute findings. Mild osteoarthritis changes consistent with prior imaging."
            ),
            DoctorRecordEntry(
                id = "3",
                title = "Cardiac Assessment",
                doctorName = "Dr. Sarah Johnson",
                createdAt = "June 5, 2025",
                category = "ECG",
                status = DoctorRecordStatus.NORMAL,
                notes = "Electrocardiogram remains stable. Recommend continued monitoring every 6 months."
            )
        )
    ),
    DoctorPatientProfile(
        id = 102,
        firstName = "Sarah",
        lastName = "Wilson",
        age = 37,
        heightCm = 165,
        weightKg = 68,
        phone = "+1 (555) 784-2210",
        email = "sarah.wilson@email.com",
        summary = "Type 2 diabetes well controlled with recent HbA1c at 6.4%. Focus on lifestyle adjustments.",
        conditions = listOf("Type 2 Diabetes"),
        medications = listOf("Metformin 500mg"),
        allergies = listOf("None reported"),
        records = emptyList()
    ),
    DoctorPatientProfile(
        id = 103,
        firstName = "Mike",
        lastName = "Johnson",
        age = 52,
        heightCm = 181,
        weightKg = 90,
        phone = "+1 (555) 990-4410",
        email = "mike.johnson@email.com",
        summary = "Patient in follow-up for anxiety management. Upcoming evaluation scheduled in two weeks.",
        conditions = listOf("Generalized Anxiety Disorder"),
        medications = listOf("Sertraline 50mg"),
        allergies = listOf("Latex"),
        records = emptyList()
    )
).associateBy { it.id }

fun Container.doctorPatientScreen(
    patientId: Long,
    onLogout: () -> Unit = { Navigator.showHome() },
    onBack: () -> Unit = { Navigator.showDoctor() }
) = vPanel(spacing = 12) {
    val profile = sampleDoctorPatients[patientId] ?: sampleDoctorPatients.values.first()
    var activeTab = DoctorPatientTab.OVERVIEW

    headerBar(
        mode = HeaderMode.DOCTOR,
        active = NavTab.NONE,
        onLogout = onLogout
    )

    val records = profile.records.toMutableList()
    var recordCounter = records.size
    val doctorName = "Dr. Sarah Johnson"

    val recordsContainer = vPanel(spacing = 12, className = "doctor-records-list").apply {
        width = 100.perc
    }

    fun renderRecordItems() {
        recordsContainer.removeAll()
        if (records.isEmpty()) {
            recordsContainer.div(className = "doctor-record-empty card") {
                p("No medical records yet. Add your first note to keep track of patient history.", className = "doctor-record-empty-text")
            }
        } else {
            records.forEach { record ->
                recordsContainer.div(className = "doctor-record-card card") {
                    div(className = "doctor-record-body") {
                        h4(record.title, className = "doctor-record-title")
                        span("${record.doctorName} • ${record.createdAt}", className = "doctor-record-subtitle")
                        p(record.notes, className = "doctor-record-notes")
                    }

                    div(className = "doctor-record-meta") {
                        span(record.category, className = "doctor-record-tag")
                        span(record.status.label, className = "doctor-record-status ${record.status.cssClass}")
                    }

                    hPanel(spacing = 8, className = "doctor-record-actions") {
                        justifyContent = JustifyContent.END
                        button("Download", className = "btn-ghost-sm").onClick {
                            Toast.info("Скачивание отчета будет доступно позже")
                        }
                    }
                }
            }
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

            if (title.isBlank()) {
                errorLabel.content = "Введите название записи"
                return@onClick
            }
            if (notes.isBlank()) {
                errorLabel.content = "Добавьте описание записи"
                return@onClick
            }

            errorLabel.content = ""
            recordCounter += 1
            val formattedDate = Date().toLocaleDateString("en-US", js("({ year: 'numeric', month: 'long', day: 'numeric' })"))
            records.add(0, DoctorRecordEntry(
                id = recordCounter.toString(),
                title = title,
                doctorName = doctorName,
                createdAt = formattedDate,
                category = if (category.isBlank()) "General" else category,
                status = DoctorRecordStatus.valueOf(statusValue),
                notes = notes
            ))
            renderRecordItems()
            titleInput.value = ""
            notesInput.value = ""
            Toast.success("Запись добавлена")
        }
    }

    fun Container.renderOverview() {
        div(className = "doctor-patient-header card") {
            div(className = "doctor-patient-header-left") {
                span(profile.fullName, className = "doctor-patient-name")
                span("${profile.age} years", className = "doctor-patient-age")
            }
            button("Назад к пациентам", className = "btn-ghost-sm").onClick { onBack() }
        }

        div(className = "doctor-patient-info card") {
            h4("General Information", className = "doctor-patient-section-title")
            div(className = "doctor-patient-stats") {
                div(className = "doctor-patient-stat") {
                    span("Height", className = "stat-label")
                    span("${profile.heightCm} cm", className = "stat-value")
                }
                div(className = "doctor-patient-stat") {
                    span("Weight", className = "stat-label")
                    span("${profile.weightKg} kg", className = "stat-value")
                }
                div(className = "doctor-patient-stat") {
                    span("Phone", className = "stat-label")
                    span(profile.phone, className = "stat-value")
                }
                div(className = "doctor-patient-stat") {
                    span("Email", className = "stat-label")
                    span(profile.email, className = "stat-value")
                }
            }
        }

        div(className = "doctor-patient-overview card") {
            h4("Clinical Summary", className = "doctor-patient-section-title")
            p(profile.summary, className = "doctor-patient-summary")
            div(className = "doctor-patient-columns") {
                div(className = "doctor-patient-column") {
                    span("Conditions", className = "doctor-patient-subtitle")
                    ul {
                        profile.conditions.forEach { condition ->
                            li(condition)
                        }
                    }
                }
                div(className = "doctor-patient-column") {
                    span("Medications", className = "doctor-patient-subtitle")
                    ul {
                        profile.medications.forEach { medication ->
                            li(medication)
                        }
                    }
                }
                div(className = "doctor-patient-column") {
                    span("Allergies", className = "doctor-patient-subtitle")
                    ul {
                        profile.allergies.forEach { allergy ->
                            li(allergy)
                        }
                    }
                }
            }
        }
    }

    fun Container.renderRecords() {
        div(className = "doctor-records-header") {
            h1("Medical Records", className = "doctor-records-title")
            button("Download All", className = "btn-ghost-sm").onClick {
                Toast.info("Скачивание всех записей скоро будет доступно")
            }
        }

        renderRecordItems()
        add(recordsContainer)
        createRecordForm(this)
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
                DoctorPatientTab.OVERVIEW -> column.renderOverview()
                DoctorPatientTab.RECORDS -> column.renderRecords()
            }
        }
    }

    div(className = "doctor container") {
        div(className = "doctor-patient grid") {
            div(className = "sidebar card doctor-patient-sidebar") {
                div(className = "avatar circle doctor-patient-avatar") { +profile.initials }
                h4(profile.fullName, className = "doctor-patient-sidebar-name")
                span("Patient ID: ${profile.id}", className = "doctor-patient-sidebar-id")

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
                button("Back to dashboard", className = "btn-primary-lg").onClick { onBack() }
            }

            mainColumn = div(className = "doctor-patient-main column")
        }
    }

    updateActive(activeTab)
}