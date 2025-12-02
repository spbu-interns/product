package ui.components

import io.kvision.core.AlignItems
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.form.select.Select
import io.kvision.form.text.Text
import io.kvision.form.text.text
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h3
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.panel.SimplePanel
import io.kvision.panel.hPanel
import io.kvision.panel.simplePanel
import io.kvision.panel.vPanel
import io.kvision.toast.Toast
import kotlin.js.Date

private data class TimetableDay(
    val label: String,
    val slots: MutableList<String> = mutableListOf(),
)

private data class TimetableTemplate(
    val name: String,
    val weekSlots: List<TimetableDay>,
)

class TimetableModalController internal constructor(
    private val renderModal: () -> Unit,
    private val openAction: (String) -> Unit,
    private val closeAction: () -> Unit,
) {
    fun open(doctorName: String) = openAction(doctorName)
    fun close() = closeAction()
    internal fun render() = renderModal()
}

fun Container.timetableModal(): TimetableModalController {
    var timetableVisible = false
    var editingTemplate = false
    var pickingTemplate = false

    var selectedDoctorName = "врач"
    var selectedTemplateIndex: Int? = null

    var manualSchedule: MutableList<TimetableDay> = generateUpcomingSchedule()
    var templateDraftName: String = "Новый шаблон"
    var templateDraftWeek: MutableList<TimetableDay> = createBlankWeekTemplate()

    val templates = mutableListOf(
        TimetableTemplate(
            name = "Утренние смены",
            weekSlots = listOf(
                TimetableDay("Понедельник", mutableListOf("09:00-12:00", "14:00-16:00")),
                TimetableDay("Вторник", mutableListOf("10:00-13:00")),
                TimetableDay("Среда", mutableListOf("09:30-12:30", "15:00-17:00")),
                TimetableDay("Четверг", mutableListOf("10:00-13:00")),
                TimetableDay("Пятница", mutableListOf("09:00-12:00", "14:00-16:00")),
                TimetableDay("Суббота", mutableListOf()),
                TimetableDay("Воскресенье", mutableListOf()),
            ),
        ),
    )

    lateinit var renderTimetableModal: () -> Unit

    val overlay: SimplePanel = simplePanel(className = "timetable-overlay-root") {
        visible = false
    }

    fun closeTimetableModal() {
        timetableVisible = false
        editingTemplate = false
        pickingTemplate = false
        selectedTemplateIndex = null
        renderTimetableModal()
    }

    fun openTimetableModal(doctorName: String) {
        selectedDoctorName = doctorName
        timetableVisible = true
        renderTimetableModal()
    }

    fun TimetableDay.deepCopy() = TimetableDay(label = label, slots = slots.toMutableList())
    fun TimetableTemplate.deepCopy() = TimetableTemplate(
        name = name,
        weekSlots = weekSlots.map { it.deepCopy() },
    )

    fun applyTemplate(template: TimetableTemplate) {
        val nextWeekSchedule = generateUpcomingSchedule(days = 7, startOffset = 7)
        template.weekSlots.forEachIndexed { index, dayTemplate ->
            if (index < nextWeekSchedule.size) {
                nextWeekSchedule[index].slots.addAll(dayTemplate.slots)
            }
        }
        manualSchedule = nextWeekSchedule
        pickingTemplate = false
        Toast.success("Шаблон \"${template.name}\" применён на следующую неделю")
        renderTimetableModal()
    }

    fun Container.renderSlotEditor(title: String, schedule: TimetableDay, onChange: () -> Unit) {
        div(className = "booking-card timetable-day-card") {
            div(className = "booking-card-header") {
                h3(title, className = "booking-card-title")
                if (schedule.slots.isEmpty()) {
                    p("Нет доступных временных интервалов", className = "booking-subtitle")
                } else {
                    p("Указано ${schedule.slots.size} интервалов", className = "booking-subtitle")
                }
            }

            vPanel(spacing = 10, className = "timetable-slot-editor-layout") {
                hPanel(spacing = 10, alignItems = AlignItems.CENTER, className = "timetable-time-row") {
                    val hours = (0..23).map { it.toString() to it.toString().padStart(2, '0') }
                    val minutes = (0..59).map { it.toString() to it.toString().padStart(2, '0') }

                    val startHourSelect = Select(options = hours) {
                        value = "09"
                        addCssClass("timetable-input")
                    }
                    val startMinuteSelect = Select(options = minutes) {
                        value = "00"
                        addCssClass("timetable-input")
                    }
                    val endHourSelect = Select(options = hours) {
                        value = "12"
                        addCssClass("timetable-input")
                    }

                    val endMinuteSelect = Select(options = minutes) {
                        value = "00"
                        addCssClass("timetable-input")
                    }

                    div(className = "timetable-time-inputs") {
                        add(startHourSelect)
                        span(":", className = "timetable-time-separator")
                        add(startMinuteSelect)
                        span(" — ", className = "timetable-time-separator")
                        add(endHourSelect)
                        span(":", className = "timetable-time-separator")
                        add(endMinuteSelect)
                    }

                    div(className = "timetable-slot-actions") {
                        button("Добавить время", className = "btn btn-primary timetable-add-slot").onClick {
                            val startHour = startHourSelect.value?.toIntOrNull()
                            val startMinute = startMinuteSelect.value?.toIntOrNull()
                            val endHour = endHourSelect.value?.toIntOrNull()
                            val endMinute = endMinuteSelect.value?.toIntOrNull()

                            if (startHour != null && startMinute != null && endHour != null && endMinute != null) {
                                val slotLabel =
                                    "${startHour.toString().padStart(2, '0')}:${startMinute.toString().padStart(2, '0')}" +
                                            "-${endHour.toString().padStart(2, '0')}:${
                                                endMinute.toString().padStart(2, '0')
                                            }"
                                schedule.slots.add(slotLabel)
                                onChange()
                            }
                        }
                    }
                }

                hPanel(spacing = 10, className = "timetable-slot-row") {
                    div(className = "timetable-slot-list timetable-slot-list-editor") {
                        schedule.slots.forEachIndexed { index, slot ->
                            div(className = "timetable-slot is-selected") {
                                +slot
                                button("×", className = "timetable-slot-remove").onClick {
                                    schedule.slots.removeAt(index)
                                    onChange()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun Container.renderTemplateCreator() {
        div(className = "booking-body") {
            h3("Создание шаблона расписания", className = "booking-title")
            p("Настройте интервалы на каждый день недели и сохраните их как шаблон.", className = "booking-hint")

            div(className = "timetable-input-row") {
                val nameField: Text = text(value = templateDraftName) {
                    placeholder = "Название шаблона"
                    addCssClass("timetable-input")
                }
                button("Сохранить шаблон", className = "btn btn-primary timetable-add-slot").onClick {
                    val newName = nameField.value?.trim().orEmpty().ifBlank { "Шаблон ${templates.size + 1}" }
                    templateDraftName = newName
                    val copy = TimetableTemplate(
                        name = newName,
                        weekSlots = templateDraftWeek.map { it.deepCopy() },
                    )
                    templates.add(copy)
                    Toast.success("Шаблон \"$newName\" сохранён")
                    editingTemplate = false
                    renderTimetableModal()
                }
                button("Назад", className = "btn btn-secondary timetable-add-slot").onClick {
                    editingTemplate = false
                    renderTimetableModal()
                }
            }

            templateDraftWeek.forEach { day ->
                renderSlotEditor(day.label, day) { renderTimetableModal() }
            }
        }
    }

    fun Container.renderTemplatePicker() {
        div(className = "booking-body") {
            h3("Выбор шаблона", className = "booking-title")
            p("Выберите сохранённый вариант расписания, чтобы применить его на следующую неделю.", className = "booking-hint")

            if (templates.isEmpty()) {
                div(className = "booking-card") {
                    p("У вас пока нет шаблонов. Создайте новый, чтобы использовать его позже.", className = "booking-empty")
                }
            } else {
                div(className = "timetable-template-list") {
                    templates.forEachIndexed { index, template ->
                        val isSelected = selectedTemplateIndex == index
                        div(className = "timetable-template-card" + if (isSelected) " is-selected" else "") {
                            onClick {
                                selectedTemplateIndex = index
                                renderTimetableModal()
                            }
                            h3(template.name, className = "booking-card-title")
                            div(className = "timetable-template-preview") {
                                template.weekSlots.take(3).forEach { day ->
                                    div("${day.label.take(3)}: ${if (day.slots.isEmpty()) "—" else day.slots.joinToString(", ")}", className = "timetable-slot")
                                }
                            }
                            if (template.weekSlots.size > 3) {
                                p("…и ещё ${template.weekSlots.size - 3} дней", className = "booking-subtitle")
                            }
                        }
                    }
                }
            }

            div(className = "timetable-actions") {
                button("Использовать выбранный шаблон", className = "btn btn-primary timetable-add-slot") {
                    disabled = selectedTemplateIndex == null
                    onClick {
                        selectedTemplateIndex?.let { index ->
                            applyTemplate(templates[index].deepCopy())
                        }
                    }
                }
                button("Назад", className = "btn btn-secondary timetable-add-slot").onClick {
                    pickingTemplate = false
                    renderTimetableModal()
                }
            }
        }
    }

    fun Container.renderScheduleEditor() {
        div(className = "booking-body") {
            h3("Расписание врача ${selectedDoctorName}", className = "booking-title")
            p(
                "Заполните свободные интервалы на ближайшие дни или используйте сохранённые шаблоны.",
                className = "booking-hint",
            )

            div(className = "timetable-actions") {
                button("Сохранить расписание", className = "btn btn-primary timetable-add-slot").onClick {
                    Toast.success("Расписание сохранено. Оно доступно пациентам для записи.")
                    closeTimetableModal()
                }
                button("Создать шаблон", className = "btn btn-secondary timetable-add-slot").onClick {
                    templateDraftName = "Новый шаблон"
                    templateDraftWeek = createBlankWeekTemplate()
                    editingTemplate = true
                    renderTimetableModal()
                }
                button("Выбрать шаблон", className = "btn btn-secondary timetable-add-slot").onClick {
                    pickingTemplate = true
                    renderTimetableModal()
                }
            }

            manualSchedule.forEach { day ->
                renderSlotEditor(day.label, day) { renderTimetableModal() }
            }
        }
    }

    renderTimetableModal = render@{
        overlay.removeAll()
        overlay.visible = timetableVisible

        if (!timetableVisible) return@render

        overlay.div(className = "timetable-overlay") {
            div(className = "timetable-backdrop").onClick { closeTimetableModal() }

            div(className = "timetable-modal") {
                div(className = "booking-modal-header") {
                    h3("Управление расписанием", className = "booking-title")
                    button("×", className = "booking-close").onClick { closeTimetableModal() }
                }

                when {
                    editingTemplate -> renderTemplateCreator()
                    pickingTemplate -> renderTemplatePicker()
                    else -> renderScheduleEditor()
                }
            }
        }
    }

    return TimetableModalController(
        renderModal = renderTimetableModal,
        openAction = ::openTimetableModal,
        closeAction = ::closeTimetableModal,
    )
}

private fun generateUpcomingSchedule(days: Int = 7, startOffset: Int = 0): MutableList<TimetableDay> {
    val weekDays = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
    val months = listOf(
        "янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек",
    )

    val base = Date()
    val millisPerDay = 24 * 60 * 60 * 1000

    return (0 until days).map { offset ->
        val date = Date(base.getTime() + (startOffset + offset) * millisPerDay)
        val label = "${date.getDate()} ${months[date.getMonth()]}, ${weekDays[date.getDay()]}"
        TimetableDay(label = label)
    }.toMutableList()
}

private fun createBlankWeekTemplate(): MutableList<TimetableDay> = mutableListOf(
    TimetableDay("Понедельник"),
    TimetableDay("Вторник"),
    TimetableDay("Среда"),
    TimetableDay("Четверг"),
    TimetableDay("Пятница"),
    TimetableDay("Суббота"),
    TimetableDay("Воскресенье"),
)
