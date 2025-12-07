package ui.components

import api.BookingApiClient
import io.kvision.core.AlignItems
import io.kvision.core.Container
import io.kvision.core.onChange
import io.kvision.core.onClick
import io.kvision.form.check.CheckBox
import io.kvision.form.select.Select
import io.kvision.form.text.Text
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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.interns.project.dto.SlotCreateRequest
import kotlin.js.Date

private enum class TemplateTarget { CURRENT, NEXT, BOTH }
private enum class PatternType { WEEKDAYS, ODD_DATES, EVEN_DATES, TWO_ON_TWO }

private data class DaySchedule(
    val date: Date,
    val label: String,
    val slots: MutableList<String> = mutableListOf(),
    val lockedSlots: MutableSet<String> = mutableSetOf(), // слоты, которые нельзя удалить (забронированы)
)

private data class ScheduleTemplate(
    val name: String,
    val description: String,
    val availableDurations: List<Int> = listOf(30),
    val defaultDuration: Int = availableDurations.first(),
    val generator: (date: Date, slotDuration: Int) -> List<String>,
)

class TimetableModalController internal constructor(
    private val renderModal: () -> Unit,
    private val openAction: (String, Long?) -> Unit,
    private val closeAction: () -> Unit,
) {
    fun open(doctorName: String, doctorId: Long?) = openAction(doctorName, doctorId)
    fun close() = closeAction()
    internal fun render() = renderModal()
}

fun Container.timetableModal(): TimetableModalController {
    val uiScope = MainScope()
    val apiClient = BookingApiClient()

    var visible = false
    var doctorName: String = ""
    var doctorId: Long? = null

    var saving = false

    // Диапазон редактируемых дат: сегодня — минимум, +6 месяцев — максимум
    var today = todayDateOnly()
    var maxDateLimit = addMonthsClamped(today, 6)
    var allDays: MutableList<DaySchedule> = generateDaysRange(today, maxDateLimit)

    var selectedDayKey: String? = allDays.firstOrNull()?.date?.toIsoDate()

    // Панель шаблонов / конструктора / отпуска
    var showAdvanced = false

    // состояние конструктора
    var constructorPattern = PatternType.WEEKDAYS
    var constructorWeekdays: MutableSet<Int> = mutableSetOf(1, 2, 3, 4, 5)
    var constructorStartTime = "09:00"
    var constructorEndTime = "18:00"
    var constructorLunchStart = "13:00"
    var constructorLunchEnd = "14:00"
    var constructorDurationMinutes = 60

    // состояние ручного добавления слота (выпадающие списки)
    var manualStartMinutes: Int? = 9 * 60  // по умолчанию 09:00
    var manualEndMinutes: Int? = 10 * 60   // по умолчанию 10:00

    // все возможные моменты времени с шагом 15 минут
    val quarterMinutes: List<Int> = (0 until 24 * 60 step 15).toList()

    // показывать ли недельный обзор
    var showWeekView = false

    // состояние шаблонов
    val templateDurationOverrides = mutableMapOf<String, Int>()
    var selectedTemplateName: String? = null
    var templateTarget = TemplateTarget.CURRENT

    // состояние отпуска
    var vacationStartDate: Date = today
    var vacationEndDate: Date = today

    val templates = listOf(
        ScheduleTemplate(
            name = "Пятидневка 9–18 с обедом",
            description = "Пн–Пт, 09:00–18:00, обед 13:00–14:00, слоты 30 или 60 минут",
            availableDurations = listOf(30, 60),
            generator = { date, duration ->
                val dow = normalizeDayOfWeek(date.getDay())
                if (dow in 1..5) generateSlots("09:00", "18:00", duration, listOf("13:00" to "14:00"))
                else emptyList()
            },
        ),
        ScheduleTemplate(
            name = "Только утро",
            description = "Пн–Пт, 09:00–13:00, без перерывов, слоты 30 минут",
            generator = { date, _ ->
                val dow = normalizeDayOfWeek(date.getDay())
                if (dow in 1..5) generateSlots("09:00", "13:00", 30) else emptyList()
            },
        ),
        ScheduleTemplate(
            name = "Только вечер",
            description = "Пн–Пт, 16:00–20:00, без перерывов, слоты 30 минут",
            generator = { date, _ ->
                val dow = normalizeDayOfWeek(date.getDay())
                if (dow in 1..5) generateSlots("16:00", "20:00", 30) else emptyList()
            },
        ),
        ScheduleTemplate(
            name = "Через день (нечётные даты)",
            description = "Только нечётные числа месяца, 10:00–16:00, слоты 60 минут",
            generator = { date, _ ->
                val day = date.getDate()
                if (day % 2 == 1) generateSlots("10:00", "16:00", 60) else emptyList()
            },
        ),
        ScheduleTemplate(
            name = "Через день (чётные даты)",
            description = "Только чётные числа месяца, 10:00–16:00, слоты 60 минут",
            generator = { date, _ ->
                val day = date.getDate()
                if (day % 2 == 0) generateSlots("10:00", "16:00", 60) else emptyList()
            },
        ),
        ScheduleTemplate(
            name = "Только выходные",
            description = "Сб–Вс, 10:00–15:00, слоты 60 минут",
            generator = { date, _ ->
                val dow = normalizeDayOfWeek(date.getDay())
                if (dow == 6 || dow == 7) generateSlots("10:00", "15:00", 60) else emptyList()
            },
        ),
    )

    lateinit var renderModal: () -> Unit

    val overlay: SimplePanel = simplePanel(className = "timetable-overlay-root") { visible = false }

    fun resetDays() {
        today = todayDateOnly()
        maxDateLimit = addMonthsClamped(today, 6)
        allDays = generateDaysRange(today, maxDateLimit)
        selectedDayKey = allDays.firstOrNull()?.date?.toIsoDate()
        vacationStartDate = today
        vacationEndDate = today
    }

    fun openModal(name: String, id: Long?) {
        doctorName = name
        doctorId = id
        resetDays()
        visible = true
        showAdvanced = false
        saving = false
        renderModal()
    }

    fun closeModal() {
        visible = false
        renderModal()
    }

    fun getWeekDays(offset: Int): List<DaySchedule> {
        val baseMonday = startOfWeekFrom(today)
        val weekStartMillis = baseMonday.getTime() + offset * 7 * MILLIS_PER_DAY
        val weekEndMillis = weekStartMillis + 6 * MILLIS_PER_DAY
        return allDays.filter { day ->
            val t = day.date.getTime()
            t >= weekStartMillis && t <= weekEndMillis
        }
    }

    fun addSlot(day: DaySchedule, start: String, end: String) {
        val startMinutes = parseTimeToMinutes(start)
        val endMinutes = parseTimeToMinutes(end)
        if (startMinutes == null || endMinutes == null || endMinutes <= startMinutes) {
            Toast.danger("Неверное время слота")
            return
        }
        if (isSlotStartInPast(day.date, startMinutes)) {
            Toast.info("Нельзя добавлять слоты в прошлое")
            return
        }
        val label = "${formatMinutes(startMinutes)}-${formatMinutes(endMinutes)}"
        day.slots.add(label)
        day.slots.sortBy { parseTimeToMinutes(it.split("-")[0]) }
        renderModal()
    }

    fun removeSlot(day: DaySchedule, index: Int) {
        val slot = day.slots.getOrNull(index) ?: return
        if (day.lockedSlots.contains(slot)) {
            Toast.info("Этот слот уже забронирован пациентом и не может быть отменён")
            return
        }
        day.slots.removeAt(index)
        renderModal()
    }

    fun applyTemplate(template: ScheduleTemplate) {
        val duration = templateDurationOverrides[template.name] ?: template.defaultDuration
        val targetWeeks: List<List<DaySchedule>> = when (templateTarget) {
            TemplateTarget.CURRENT -> listOf(getWeekDays(0))
            TemplateTarget.NEXT -> listOf(getWeekDays(1))
            TemplateTarget.BOTH -> listOf(getWeekDays(0), getWeekDays(1))
        }

        targetWeeks.forEach { week ->
            week.forEach { day ->
                day.slots.clear()
                val generated = template.generator(day.date, duration)
                val filtered = generated.filter { label -> !isSlotInPast(day.date, label) }
                day.slots.addAll(filtered)
            }
        }
        Toast.success("Шаблон \"${template.name}\" применён")
        renderModal()
    }

    fun applyConstructorRule() {
        val breaks = mutableListOf<Pair<String, String>>()
        val lunchStart = constructorLunchStart.trim()
        val lunchEnd = constructorLunchEnd.trim()
        if (lunchStart.isNotEmpty() && lunchEnd.isNotEmpty()) {
            breaks += lunchStart to lunchEnd
        }

        val targetWeeks: List<List<DaySchedule>> = when (templateTarget) {
            TemplateTarget.CURRENT -> listOf(getWeekDays(0))
            TemplateTarget.NEXT -> listOf(getWeekDays(1))
            TemplateTarget.BOTH -> listOf(getWeekDays(0), getWeekDays(1))
        }

        targetWeeks.forEach { week ->
            week.forEach { day ->
                val isWorking = when (constructorPattern) {
                    PatternType.WEEKDAYS -> {
                        val dow = normalizeDayOfWeek(day.date.getDay())
                        constructorWeekdays.contains(dow)
                    }

                    PatternType.ODD_DATES -> day.date.getDate() % 2 == 1
                    PatternType.EVEN_DATES -> day.date.getDate() % 2 == 0
                    PatternType.TWO_ON_TWO -> {
                        // считаем от понедельника текущей недели
                        val baseMonday = startOfWeekFrom(today)
                        val indexFromBase =
                            ((day.date.getTime() - baseMonday.getTime()) / MILLIS_PER_DAY).toInt()
                        (indexFromBase % 4) in 0..1 // 2 дня работа, 2 выходных
                    }
                }

                if (isWorking) {
                    day.slots.clear()
                    val generated = generateSlots(
                        constructorStartTime,
                        constructorEndTime,
                        constructorDurationMinutes,
                        breaks,
                    )
                    val filtered = generated.filter { label -> !isSlotInPast(day.date, label) }
                    day.slots.addAll(filtered)
                } else {
                    day.slots.clear()
                }
            }
        }

        Toast.success("Расписание сгенерировано по конструктору")
        renderModal()
    }

    fun applyVacationRange() {
        val startMillis = vacationStartDate.getTime()
        val endMillis = vacationEndDate.getTime()
        var removedCount = 0
        var lockedKept = 0

        allDays.forEach { day ->
            val t = day.date.getTime()
            if (t >= startMillis && t <= endMillis) {
                val (locked, free) = day.slots.partition { slot -> day.lockedSlots.contains(slot) }
                removedCount += free.size
                lockedKept += locked.size
                day.slots.clear()
                day.slots.addAll(locked)
            }
        }

        when {
            removedCount == 0 && lockedKept == 0 ->
                Toast.info("На выбранный период слотов не найдено")

            removedCount > 0 && lockedKept == 0 ->
                Toast.success("Удалено $removedCount свободных слотов на выбранный период")

            removedCount > 0 && lockedKept > 0 ->
                Toast.success("Удалено $removedCount свободных слотов, $lockedKept забронированных слотов сохранены")

            removedCount == 0 && lockedKept > 0 ->
                Toast.info("Есть только забронированные слоты, удалить их нельзя")
        }

        renderModal()
    }

    fun saveSchedule() {
        val id = doctorId
        if (id == null) {
            Toast.danger("Не выбран врач")
            return
        }
        if (saving) return
        saving = true
        renderModal()

        uiScope.launch {
            val slotRequests = allDays.flatMap { day ->
                day.slots.mapNotNull { label ->
                    val range = parseSlotRange(label) ?: return@mapNotNull null
                    val dateIso = day.date.toIsoDate()
                    SlotCreateRequest(
                        doctorId = id,
                        startTime = combineDateWithTime(dateIso, range.first),
                        endTime = combineDateWithTime(dateIso, range.second),
                    )
                }
            }

            var successCount = 0
            for (request in slotRequests) {
                val result = apiClient.createSlot(id, request)
                if (result.isSuccess) successCount++ else break
            }

            if (successCount == slotRequests.size) {
                Toast.success("Расписание сохранено, опубликовано $successCount слотов")
            } else {
                Toast.danger("Ошибка при сохранении расписания")
            }

            saving = false
            renderModal()
        }
    }

    fun Container.renderDayCard(day: DaySchedule) {
        div(className = "booking-card timetable-day-card") {
            div(className = "timetable-day-header") {
                h3(day.label, className = "booking-card-title")
                p(day.date.readableDate(), className = "booking-hint")
            }

            if (day.slots.isEmpty()) {
                p("Нет слотов", className = "booking-hint")
            } else {
                div(className = "timetable-slot-list") {
                    day.slots.forEachIndexed { index, slot ->
                        val isLocked = day.lockedSlots.contains(slot)
                        val slotClass = "timetable-slot" + if (isLocked) " is-locked" else ""
                        div(className = slotClass) {
                            +slot
                            if (!isLocked) {
                                button("×", className = "timetable-slot-remove").onClick {
                                    removeSlot(day, index)
                                }
                            } else {
                                span("занят", className = "timetable-slot-label")
                            }
                        }
                    }
                }
            }

            // компактный редактор слотов: выбор времени из селектов с шагом 15 минут
            vPanel(spacing = 8, className = "timetable-slot-editor") {
                // нормализуем старт: либо из состояния, либо дефолт 09:00
                val startBase = (manualStartMinutes ?: (9 * 60))
                    .coerceIn(0, quarterMinutes.last())
                val startHourVal = startBase / 60
                val startMinuteVal = startBase % 60

                // допустимые окончания — строго позже старта
                val allowedEndTimes = quarterMinutes.filter { it > startBase }

                val effectiveEndMin: Int? = allowedEndTimes
                    .firstOrNull { it == manualEndMinutes }
                    ?: allowedEndTimes.firstOrNull()

                val endHourVal = effectiveEndMin?.div(60)
                val endMinuteVal = effectiveEndMin?.rem(60)

                val hourOptions = (0..23).map { h ->
                    val label = h.toString().padStart(2, '0')
                    h.toString() to label
                }
                val minuteBaseOptions = listOf(0, 15, 30, 45).map { m ->
                    val label = m.toString().padStart(2, '0')
                    m.toString() to label
                }

                hPanel(spacing = 6, alignItems = AlignItems.CENTER) {
                    span("От", className = "booking-subtitle")

                    // старт: часы
                    val startHourSelect = Select(
                        options = hourOptions,
                    ) {
                        value = startHourVal.toString()
                        addCssClass("timetable-input")
                        addCssClass("timetable-time-select")
                    }
                    add(startHourSelect)

                    span(":", className = "timetable-time-separator")

                    // старт: минуты
                    val startMinuteSelect = Select(
                        options = minuteBaseOptions,
                    ) {
                        value = startMinuteVal.toString()
                        addCssClass("timetable-input")
                        addCssClass("timetable-time-select")
                    }
                    add(startMinuteSelect)

                    span("До", className = "booking-subtitle")

                    // конец: какие часы вообще доступны
                    val allowedEndHours = allowedEndTimes.map { it / 60 }.distinct()
                    val endHourOptions = allowedEndHours.map { h ->
                        val label = h.toString().padStart(2, '0')
                        h.toString() to label
                    }

                    val noEndAvailable = allowedEndTimes.isEmpty()

                    val endHourSelect = Select(
                        options = endHourOptions,
                    ) {
                        value = endHourVal?.toString()
                        addCssClass("timetable-input")
                        addCssClass("timetable-time-select")
                        disabled = noEndAvailable
                    }
                    add(endHourSelect)

                    span(":", className = "timetable-time-separator")

                    // минуты для выбранного часа окончания
                    val currentEndHour = endHourSelect.value?.toIntOrNull()
                    val allowedEndMinutesForHour = if (currentEndHour != null) {
                        allowedEndTimes
                            .filter { it / 60 == currentEndHour }
                            .map { it % 60 }
                            .distinct()
                            .sorted()
                    } else emptyList()

                    val endMinuteOptions = allowedEndMinutesForHour.map { m ->
                        val label = m.toString().padStart(2, '0')
                        m.toString() to label
                    }

                    val endMinuteSelect = Select(
                        options = endMinuteOptions,
                    ) {
                        value = endMinuteVal?.toString()
                        addCssClass("timetable-input")
                        addCssClass("timetable-time-select")
                        disabled = noEndAvailable || endMinuteOptions.isEmpty()
                    }
                    add(endMinuteSelect)

                    val addButton = button(
                        "Добавить слот",
                        className = "btn btn-primary timetable-add-slot",
                    ) {
                        disabled = noEndAvailable
                    }

                    addButton.onClick {
                        val sh = startHourSelect.value?.toIntOrNull()
                        val sm = startMinuteSelect.value?.toIntOrNull()
                        val eh = endHourSelect.value?.toIntOrNull()
                        val em = endMinuteSelect.value?.toIntOrNull()

                        if (sh == null || sm == null || eh == null || em == null) {
                            Toast.info("Выберите время начала и конца")
                            return@onClick
                        }

                        val startMin = sh * 60 + sm
                        val endMin = eh * 60 + em

                        manualStartMinutes = startMin
                        manualEndMinutes = endMin

                        addSlot(day, formatMinutes(startMin), formatMinutes(endMin))
                    }

                    // реакции на изменения — пересчёт допустимых концов через полное перерисовывание
                    startHourSelect.onChange {
                        val sh = this.value?.toIntOrNull() ?: startHourVal
                        val sm = startMinuteSelect.value?.toIntOrNull() ?: startMinuteVal
                        manualStartMinutes = sh * 60 + sm
                        manualEndMinutes = null
                        renderModal()
                    }

                    startMinuteSelect.onChange {
                        val sh = startHourSelect.value?.toIntOrNull() ?: startHourVal
                        val sm = this.value?.toIntOrNull() ?: startMinuteVal
                        manualStartMinutes = sh * 60 + sm
                        manualEndMinutes = null
                        renderModal()
                    }

                    endHourSelect.onChange {
                        val eh = this.value?.toIntOrNull()
                        if (eh != null) {
                            val minsForHour = allowedEndTimes
                                .filter { it / 60 == eh }
                                .map { it % 60 }
                                .sorted()
                            val chosenMinute = minsForHour.firstOrNull()
                            if (chosenMinute != null) {
                                manualEndMinutes = eh * 60 + chosenMinute
                                renderModal()
                            }
                        }
                    }

                    endMinuteSelect.onChange {
                        val eh = endHourSelect.value?.toIntOrNull()
                        val em = this.value?.toIntOrNull()
                        if (eh != null && em != null) {
                            manualEndMinutes = eh * 60 + em
                        }
                    }
                }
            }
        }
    }

    fun Container.renderWeekView(anchorDate: Date) {
        val monday = startOfWeekFrom(anchorDate)
        val weekDays = (0 until 7).map { shift ->
            val d = Date(monday.getTime() + shift * MILLIS_PER_DAY)
            val iso = d.toIsoDate()
            allDays.firstOrNull { it.date.toIsoDate() == iso }
                ?: DaySchedule(date = d, label = iso)
        }

        div(className = "booking-card timetable-week-view-card") {
            h3("Расписание на неделю", className = "booking-card-title")
            weekDays.forEach { day ->
                div(className = "timetable-week-row") {
                    span(day.label, className = "timetable-week-day-label")
                    if (day.slots.isEmpty()) {
                        span("нет слотов", className = "timetable-week-day-empty")
                    } else {
                        val preview = day.slots.take(5).joinToString(", ")
                        val more = day.slots.size - 5
                        val text = if (more > 0) "$preview, +$more" else preview
                        span(text, className = "timetable-week-day-slots")
                    }
                }
            }
        }
    }

    fun Container.renderTopControls() {
        div(className = "timetable-topbar") {
            hPanel(spacing = 8, alignItems = AlignItems.CENTER, className = "timetable-actions") {
                span("Автозаполнение расписания", className = "booking-subtitle")
                button(
                    if (showAdvanced) "Скрыть шаблоны, конструктор и отпуск"
                    else "Шаблоны, конструктор и отпуск",
                    className = "btn btn-outline timetable-toggle-advanced",
                ).onClick {
                    showAdvanced = !showAdvanced
                    renderModal()
                }
            }
        }
    }

    fun Container.renderAdvancedPanel() {
        div(className = "booking-body timetable-advanced") {
            // Общая карточка с шаблонами и конструктором
            div(className = "booking-card timetable-advanced-card") {
                div(className = "timetable-advanced-header") {
                    h3("Автозаполнение расписания", className = "booking-card-title")
                    p(
                        "Шаблоны, конструктор и отпуск — для массового изменения текущей и следующей недели.",
                        className = "booking-hint",
                    )
                }

                // Куда применяем — общее для шаблонов и конструктора
                hPanel(spacing = 8, alignItems = AlignItems.CENTER, className = "timetable-actions") {
                    span("Куда применить", className = "booking-subtitle")
                    Select(options = TemplateTarget.entries.map { it.name to itLabel(it) }) {
                        value = templateTarget.name
                        addCssClass("timetable-input")
                    }.onChange {
                        templateTarget = TemplateTarget.valueOf(this.value ?: TemplateTarget.CURRENT.name)
                    }
                }

                // Две колонки: слева шаблоны, справа конструктор
                hPanel(spacing = 16, alignItems = AlignItems.CENTER, className = "timetable-advanced-columns") {
                    // Шаблоны
                    div(className = "booking-card timetable-templates-card") {
                        h3("Готовые шаблоны", className = "booking-card-title")

                        vPanel(spacing = 8) {
                            hPanel(spacing = 8, alignItems = AlignItems.CENTER) {
                                span("Шаблон", className = "booking-subtitle")
                                Select(
                                    options = listOf("" to "Не выбрано") + templates.map { it.name to it.name },
                                ) {
                                    value = selectedTemplateName
                                    addCssClass("timetable-input")
                                }.onChange {
                                    selectedTemplateName = this.value
                                    renderModal()
                                }
                            }

                            val currentTemplate = templates.firstOrNull { it.name == selectedTemplateName }
                            if (currentTemplate != null) {
                                hPanel(spacing = 8, alignItems = AlignItems.CENTER) {
                                    if (currentTemplate.availableDurations.size > 1) {
                                        span("Длительность", className = "booking-subtitle")
                                        Select(
                                            options = currentTemplate.availableDurations.map { it.toString() to "$it мин" },
                                        ) {
                                            value = (templateDurationOverrides[currentTemplate.name]
                                                ?: currentTemplate.defaultDuration).toString()
                                            addCssClass("timetable-input")
                                        }.onChange {
                                            val chosen = this.value?.toIntOrNull()
                                            if (chosen != null) {
                                                templateDurationOverrides[currentTemplate.name] = chosen
                                            }
                                        }
                                    }
                                    button(
                                        "Применить",
                                        className = "btn btn-secondary timetable-apply-template",
                                    ).onClick {
                                        applyTemplate(currentTemplate)
                                    }
                                }
                                p(currentTemplate.description, className = "booking-hint")
                            } else {
                                p(
                                    "Выберите шаблон, чтобы применить его к текущей или следующей неделе.",
                                    className = "booking-hint",
                                )
                            }
                        }
                    }

                    // Конструктор
                    div(className = "booking-card timetable-constructor-card") {
                        h3("Конструктор расписания", className = "booking-card-title")
                        p(
                            "Выберите схему (по дням недели, чётные / нечётные, 2/2), " +
                                    "задайте рабочий день, обед и длительность слота. " +
                                    "Мы автоматически разобьём день на интервалы, пропуская обед и прошлое.",
                            className = "booking-hint",
                        )

                        var startField: Text? = null
                        var endField: Text? = null
                        var lunchStartField: Text? = null
                        var lunchEndField: Text? = null

                        vPanel(spacing = 8) {
                            // Тип расписания
                            hPanel(spacing = 8, alignItems = AlignItems.CENTER) {
                                span("Тип расписания", className = "booking-subtitle")
                                Select(
                                    options = listOf(
                                        PatternType.WEEKDAYS.name to "По дням недели",
                                        PatternType.ODD_DATES.name to "Нечётные даты",
                                        PatternType.EVEN_DATES.name to "Чётные даты",
                                        PatternType.TWO_ON_TWO.name to "2 через 2",
                                    ),
                                ) {
                                    value = constructorPattern.name
                                    addCssClass("timetable-input")
                                }.onChange {
                                    val raw = this.value
                                    constructorPattern =
                                        raw?.let { PatternType.valueOf(it) } ?: PatternType.WEEKDAYS
                                    renderModal()
                                }
                            }

                            // Чекбоксы дней недели для режима WEEKDAYS
                            if (constructorPattern == PatternType.WEEKDAYS) {
                                hPanel(spacing = 6, alignItems = AlignItems.CENTER) {
                                    span("Дни недели", className = "booking-subtitle")
                                    val days = listOf(
                                        1 to "Пн",
                                        2 to "Вт",
                                        3 to "Ср",
                                        4 to "Чт",
                                        5 to "Пт",
                                        6 to "Сб",
                                        7 to "Вс",
                                    )
                                    days.forEach { (key, label) ->
                                        CheckBox(
                                            label = label,
                                            value = constructorWeekdays.contains(key),
                                        ).apply {
                                            addCssClass("booking-checkbox")
                                            onClick {
                                                if (constructorWeekdays.contains(key)) {
                                                    constructorWeekdays.remove(key)
                                                } else {
                                                    constructorWeekdays.add(key)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Рабочий день
                            hPanel(spacing = 8, alignItems = AlignItems.CENTER) {
                                span("Рабочий день", className = "booking-subtitle")
                                startField = Text(value = constructorStartTime) { addCssClass("timetable-input") }
                                span("—", className = "booking-subtitle")
                                endField = Text(value = constructorEndTime) { addCssClass("timetable-input") }
                            }

                            // Обед
                            hPanel(spacing = 8, alignItems = AlignItems.CENTER) {
                                span("Обед", className = "booking-subtitle")
                                lunchStartField =
                                    Text(value = constructorLunchStart) { addCssClass("timetable-input") }
                                span("—", className = "booking-subtitle")
                                lunchEndField =
                                    Text(value = constructorLunchEnd) { addCssClass("timetable-input") }
                                span("(можно оставить пустым)", className = "booking-hint")
                            }

                            // Длительность
                            hPanel(spacing = 8, alignItems = AlignItems.CENTER) {
                                span("Длительность слота", className = "booking-subtitle")
                                Select(
                                    options = listOf(
                                        "30" to "30 мин",
                                        "45" to "45 мин",
                                        "60" to "60 мин",
                                    ),
                                ) {
                                    value = constructorDurationMinutes.toString()
                                    addCssClass("timetable-input")
                                }.onChange {
                                    val chosen = this.value?.toIntOrNull()
                                    if (chosen != null) {
                                        constructorDurationMinutes = chosen
                                    }
                                }
                            }

                            // Кнопка "Сгенерировать"
                            hPanel(spacing = 8, alignItems = AlignItems.CENTER, className = "timetable-actions") {
                                button(
                                    "Сгенерировать расписание",
                                    className = "btn btn-primary timetable-generate",
                                ).onClick {
                                    val newStart = startField?.value?.trim()
                                    val newEnd = endField?.value?.trim()
                                    if (!newStart.isNullOrEmpty()) constructorStartTime = newStart
                                    if (!newEnd.isNullOrEmpty()) constructorEndTime = newEnd
                                    constructorLunchStart = lunchStartField?.value?.trim().orEmpty()
                                    constructorLunchEnd = lunchEndField?.value?.trim().orEmpty()
                                    applyConstructorRule()
                                }
                                span(
                                    "Будет заполнена текущая, следующая или обе недели — в зависимости от выбора.",
                                    className = "booking-hint",
                                )
                            }
                        }
                    }
                }
            }

            // Карточка отпуска
            div(className = "booking-card timetable-vacation-card") {
                h3("Отпуск", className = "booking-card-title")
                p(
                    "Выберите период отпуска — все свободные слоты в этом промежутке будут удалены. " +
                            "Забронированные слоты останутся, чтобы не потерять записи пациентов.",
                    className = "booking-hint",
                )

                vPanel(spacing = 8) {
                    p("Начало отпуска", className = "booking-subtitle")
                    dateCalendar(
                        initialDate = vacationStartDate,
                        minDate = today,
                        maxDate = maxDateLimit,
                    ) { date ->
                        vacationStartDate = date
                        if (vacationEndDate.getTime() < vacationStartDate.getTime()) {
                            vacationEndDate = vacationStartDate
                        }
                        renderModal()
                    }

                    p("Конец отпуска", className = "booking-subtitle")
                    dateCalendar(
                        initialDate = vacationEndDate,
                        minDate = vacationStartDate,
                        maxDate = maxDateLimit,
                    ) { date ->
                        vacationEndDate = date
                        renderModal()
                    }

                    hPanel(spacing = 8, alignItems = AlignItems.CENTER, className = "timetable-actions") {
                        button(
                            "Удалить слоты в период отпуска",
                            className = "btn btn-warning timetable-vacation-apply",
                        ).onClick {
                            applyVacationRange()
                        }
                    }
                }
            }
        }
    }

    fun Container.renderMainContent() {
        if (selectedDayKey == null && allDays.isNotEmpty()) {
            selectedDayKey = today.toIsoDate()
        }

        val selectedDay = allDays.firstOrNull { it.date.toIsoDate() == selectedDayKey }

        div(className = "booking-body timetable-main") {
            hPanel(spacing = 16, alignItems = AlignItems.CENTER, className = "timetable-main-layout") {
                // левая колонка — календарь
                div(className = "timetable-calendar-col") {
                    if (allDays.isNotEmpty()) {
                        dateCalendar(
                            initialDate = selectedDay?.date ?: today,
                            minDate = today,
                            maxDate = maxDateLimit,
                        ) { date ->
                            val iso = date.toIsoDate()
                            selectedDayKey = iso
                            renderModal()
                        }
                    } else {
                        p("Нет доступных дат для редактирования", className = "booking-hint")
                    }
                }

                // правая колонка — карточка выбранного дня + сохранение
                div(className = "timetable-day-column") {
                    if (selectedDay != null) {
                        renderDayCard(selectedDay)

                        // кнопка показать/скрыть недельное расписание
                        hPanel(
                            spacing = 8,
                            alignItems = AlignItems.CENTER,
                            className = "timetable-week-toggle-row",
                        ) {
                            button(
                                if (showWeekView) "Скрыть расписание на неделю"
                                else "Показать расписание на неделю",
                                className = "btn btn-outline timetable-week-toggle",
                            ).onClick {
                                showWeekView = !showWeekView
                                renderModal()
                            }
                        }

                        if (showWeekView) {
                            renderWeekView(selectedDay.date)
                        }
                    } else {
                        p(
                            "Выберите дату в календаре, чтобы добавить или отредактировать слоты.",
                            className = "booking-hint",
                        )
                    }

                    // Кнопка сохранения — всегда внизу колонки
                    hPanel(spacing = 10, alignItems = AlignItems.CENTER, className = "timetable-actions") {
                        button("Сохранить расписание", className = "btn btn-primary") {
                            disabled = saving
                        }.onClick { saveSchedule() }
                        if (saving) {
                            span("Сохраняем слоты...", className = "booking-hint")
                        }
                    }
                }
            }
        }
    }

    renderModal = fun() {
        overlay.visible = visible
        overlay.removeAll()
        if (!visible) return

        overlay.div(className = "timetable-overlay") {
            div(className = "timetable-backdrop").onClick { closeModal() }
            div(className = "timetable-modal") {
                div(className = "booking-modal-header") {
                    h3("Расписание врача $doctorName", className = "booking-title")
                    button("×", className = "booking-close").onClick { closeModal() }
                }
                renderTopControls()
                renderMainContent()
                if (showAdvanced) {
                    renderAdvancedPanel()
                }
            }
        }
    }

    renderModal()

    return TimetableModalController(
        renderModal = renderModal,
        openAction = ::openModal,
        closeAction = ::closeModal,
    )
}

private fun itLabel(target: TemplateTarget): String = when (target) {
    TemplateTarget.CURRENT -> "Текущая неделя"
    TemplateTarget.NEXT -> "Следующая неделя"
    TemplateTarget.BOTH -> "Две недели"
}

private fun todayDateOnly(): Date {
    val now = Date()
    return Date(now.getFullYear(), now.getMonth(), now.getDate())
}

private fun addMonthsClamped(base: Date, months: Int): Date {
    val year = base.getFullYear()
    val month = base.getMonth()
    val day = base.getDate()
    return Date(year, month + months, day)
}

private fun generateDaysRange(startDate: Date, endDate: Date): MutableList<DaySchedule> {
    val months = listOf(
        "янв",
        "фев",
        "мар",
        "апр",
        "май",
        "июн",
        "июл",
        "авг",
        "сен",
        "окт",
        "ноя",
        "дек",
    )
    val weekdays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    val result = mutableListOf<DaySchedule>()

    val startOnly = Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate())
    val endOnly = Date(endDate.getFullYear(), endDate.getMonth(), endDate.getDate())
    var currentMillis = startOnly.getTime()
    val endMillis = endOnly.getTime()

    while (currentMillis <= endMillis) {
        val date = Date(currentMillis)
        val dowIndex = normalizeDayOfWeek(date.getDay()) - 1
        val label = "${date.getDate().toString().padStart(2, '0')} ${months[date.getMonth()]}, ${weekdays[dowIndex]}"
        result.add(DaySchedule(date = date, label = label))
        currentMillis += MILLIS_PER_DAY
    }

    return result
}

private fun startOfWeekFrom(date: Date): Date {
    val day = normalizeDayOfWeek(date.getDay()) // 1 = Пн, 7 = Вс
    val mondayMillis = date.getTime() - (day - 1) * MILLIS_PER_DAY
    return Date(mondayMillis)
}

private fun Date.readableDate(): String = toIsoDate()

private fun Date.toIsoDate(): String {
    val year = this.getFullYear()
    val month = (this.getMonth() + 1).toString().padStart(2, '0')
    val day = this.getDate().toString().padStart(2, '0')
    return "$year-$month-$day"
}

private fun normalizeDayOfWeek(jsDay: Int): Int = if (jsDay == 0) 7 else jsDay

private fun parseSlotRange(label: String): Pair<String, String>? {
    val parts = label.split("-")
    if (parts.size != 2) return null
    return parts[0].trim() to parts[1].trim()
}

private fun parseTimeToMinutes(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

private fun formatMinutes(value: Int): String =
    "${(value / 60).toString().padStart(2, '0')}:${(value % 60).toString().padStart(2, '0')}"

private fun generateSlots(
    startTime: String,
    endTime: String,
    slotDurationMinutes: Int,
    breaks: List<Pair<String, String>> = emptyList(),
): List<String> {
    val start = parseTimeToMinutes(startTime) ?: return emptyList()
    val end = parseTimeToMinutes(endTime) ?: return emptyList()
    if (end <= start) return emptyList()
    val duration = slotDurationMinutes.coerceAtLeast(10)
    val normalizedBreaks = breaks.mapNotNull { (bStart, bEnd) ->
        val s = parseTimeToMinutes(bStart)
        val e = parseTimeToMinutes(bEnd)
        if (s != null && e != null && e > s) s to e else null
    }
    val slots = mutableListOf<String>()
    var pointer = start
    while (pointer + duration <= end) {
        val slotEnd = pointer + duration
        val hasOverlap = normalizedBreaks.any { (bStart, bEnd) -> pointer < bEnd && slotEnd > bStart }
        if (!hasOverlap) {
            slots.add("${formatMinutes(pointer)}-${formatMinutes(slotEnd)}")
        }
        pointer += duration
    }
    return slots
}

private fun isSlotInPast(day: Date, label: String): Boolean {
    val range = parseSlotRange(label) ?: return true
    val startMinutes = parseTimeToMinutes(range.first) ?: return true
    return isSlotStartInPast(day, startMinutes)
}

private fun isSlotStartInPast(day: Date, startMinutes: Int): Boolean {
    val now = Date()

    val todayDate = Date(now.getFullYear(), now.getMonth(), now.getDate())
    val dayDate = Date(day.getFullYear(), day.getMonth(), day.getDate())

    val todayMillis = todayDate.getTime()
    val dayMillis = dayDate.getTime()

    return when {
        dayMillis < todayMillis -> true
        dayMillis > todayMillis -> false
        else -> {
            val nowMinutes = now.getHours() * 60 + now.getMinutes()
            startMinutes <= nowMinutes
        }
    }
}

private fun combineDateWithTime(date: String, time: String): String = "${date}T${time}:00Z"

private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000
