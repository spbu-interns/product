package ui.components

import io.kvision.core.Cursor
import io.kvision.core.AlignItems
import io.kvision.core.Container
import io.kvision.core.Display
import io.kvision.core.FlexDirection
import io.kvision.core.FlexWrap
import io.kvision.core.FontWeight
import io.kvision.core.JustifyContent
import io.kvision.core.TextAlign
import io.kvision.core.onEvent
import io.kvision.form.select.Select
import io.kvision.form.select.select
import io.kvision.html.Button
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.panel.SimplePanel
import io.kvision.utils.px
import kotlin.js.Date

class DateCalendar(
    initialDate: Date?,
    private val minDate: Date? = null,
    private val maxDate: Date? = null,
    private val availableDates: Set<String>? = null,
    private val onDateSelected: (Date) -> Unit
) : SimplePanel() {
    private val monthNames = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )

    private val effectiveMinDate = minDate ?: Date(1900, 0, 1)
    private val effectiveMaxDate = maxDate ?: Date(2100, 11, 31)

    private var allowedDates: Set<String>? = availableDates?.toSet()

    private var selectedDate: Date? = initialDate?.let { clampToRange(it) }?.takeIf { isAllowed(it) }
    private var displayedMonth: Int = (selectedDate ?: clampToRange(Date())).getMonth()
    private var displayedYear: Int = (selectedDate ?: clampToRange(Date())).getFullYear()

    private lateinit var daysContainer: SimplePanel
    private lateinit var monthSelect: Select
    private lateinit var yearSelect: Select
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button

    init {
        addCssClass("date-calendar")
        buildCalendar()
    }

    fun setSelectedDate(date: Date?) {
        selectedDate = date?.let { clampToRange(it) }?.takeIf { isAllowed(it) }
        val target = selectedDate ?: Date(displayedYear, displayedMonth, 1)
        displayedMonth = target.getMonth()
        displayedYear = target.getFullYear()
        syncSelectors()
        renderDays()
    }

    fun setAllowedDates(dates: Set<String>?) {
        allowedDates = dates?.toSet()
        if (selectedDate?.let { !isAllowed(it) } == true) {
            selectedDate = null
        }
        renderDays()
    }

    private fun buildCalendar() {
        addCssClass("card")
        padding = 12.px
        width = 340.px

        // Заголовок
        div(className = "calendar-header") {
            display = Display.FLEX
            flexDirection = FlexDirection.ROW
            alignItems = AlignItems.CENTER
            justifyContent = JustifyContent.CENTER

            // Левая стрелка
            prevButton = button("←", className = "btn-secondary calendar-nav-btn") {
                onClick { changeMonth(-1) }
                marginRight = 8.px
            }

            // Месяц
            monthSelect = select(
                options = monthNames.mapIndexed { index, title -> index.toString() to title },
                label = null
            ) {
                value = displayedMonth.toString()
                marginRight = 8.px
                addCssClass("calendar-header-select")
                onEvent {
                    change = {
                        displayedMonth = value?.toIntOrNull()?.coerceIn(0, 11) ?: displayedMonth
                        renderDays()
                    }
                }
            }

            // Год
            yearSelect = select(
                options = buildYearOptions(),
                label = null
            ) {
                value = displayedYear.toString()
                addCssClass("calendar-header-select")
                onEvent {
                    change = {
                        displayedYear = value?.toIntOrNull() ?: displayedYear
                        renderDays()
                    }
                }
            }

            // Правая стрелка
            nextButton = button("→", className = "btn-secondary calendar-nav-btn") {
                onClick { changeMonth(1) }
                marginLeft = 8.px
            }
        }

        // Заголовки дней недели — тот же flex-лейаут, что и у дней
        div(className = "calendar-weekdays") {
            display = Display.FLEX
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEXSTART
            marginTop = 8.px
            marginBottom = 4.px
            fontWeight = FontWeight.BOLD
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                div(day) {
                    textAlign = TextAlign.CENTER
                    width = 40.px
                    marginRight = 4.px
                }
            }
        }

        // Сетка дней
        daysContainer = div(className = "calendar-days") {
            display = Display.FLEX
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
        }

        renderDays()
    }

    private fun renderDays() {
        daysContainer.removeAll()
        syncSelectors()
        updateNavButtons()

        val firstDayOfMonth = Date(displayedYear, displayedMonth, 1)
        val daysInMonth = Date(displayedYear, displayedMonth + 1, 0).getDate()
        // getDay(): 0 = Вс, 1 = Пн, ..., 6 = Сб -> сдвиг, чтобы Пн был 0
        val startOffset = ((firstDayOfMonth.getDay() + 6) % 7)

        // 1) Плейсхолдеры перед первым днём
        repeat(startOffset) {
            daysContainer.button("") {
                width = 40.px
                height = 32.px
                marginRight = 4.px
                marginBottom = 4.px

                paddingLeft = 0.px
                paddingRight = 0.px
                textAlign = TextAlign.CENTER

                addCssClass("btn-secondary")
                addCssClass("calendar-day")
                addCssClass("calendar-day-placeholder") // на случай кастомного CSS

                disabled = true
                cursor = Cursor.DEFAULT
                opacity = 0.5   // чуть светлее, чем обычные
            }
        }

        // 2) Реальные дни месяца
        for (day in 1..daysInMonth) {
            val currentDate = Date(displayedYear, displayedMonth, day)
            val disabled = !isWithinRange(currentDate) || !isAllowed(currentDate)
            val isSelected = selectedDate?.let { sameDate(it, currentDate) } == true

            daysContainer.button(day.toString()) {
                width = 40.px
                height = 32.px
                marginRight = 4.px
                marginBottom = 4.px

                paddingLeft = 0.px
                paddingRight = 0.px
                textAlign = TextAlign.CENTER

                addCssClass("calendar-day")

                cursor = if (disabled) Cursor.DEFAULT else Cursor.POINTER

                if (isSelected) {
                    addCssClass("btn-primary")
                } else {
                    addCssClass("btn-secondary")
                }

                this.disabled = disabled
                if (!disabled) onClick {
                    val clamped = clampToRange(currentDate)
                    selectedDate = clamped
                    renderDays()
                    onDateSelected(clamped)
                }
            }
        }
    }

    private fun changeMonth(delta: Int) {
        val newMonth = displayedMonth + delta
        val newDate = Date(displayedYear, newMonth, 1)
        val minStart = Date(effectiveMinDate.getFullYear(), effectiveMinDate.getMonth(), 1)
        val maxStart = Date(effectiveMaxDate.getFullYear(), effectiveMaxDate.getMonth(), 1)

        val clamped = when {
            newDate.getTime() < minStart.getTime() -> minStart
            newDate.getTime() > maxStart.getTime() -> maxStart
            else -> newDate
        }

        displayedMonth = clamped.getMonth()
        displayedYear = clamped.getFullYear()
        renderDays()
    }

    private fun updateNavButtons() {
        val currentStart = Date(displayedYear, displayedMonth, 1)
        val minStart = Date(effectiveMinDate.getFullYear(), effectiveMinDate.getMonth(), 1)
        val maxStart = Date(effectiveMaxDate.getFullYear(), effectiveMaxDate.getMonth(), 1)

        val currentMillis = currentStart.getTime()
        val minMillis = minStart.getTime()
        val maxMillis = maxStart.getTime()

        val canGoPrev = currentMillis > minMillis
        val canGoNext = currentMillis < maxMillis

        prevButton.disabled = !canGoPrev
        nextButton.disabled = !canGoNext

        prevButton.cursor = if (canGoPrev) Cursor.POINTER else Cursor.DEFAULT
        nextButton.cursor = if (canGoNext) Cursor.POINTER else Cursor.DEFAULT
    }

    private fun sameDate(first: Date, second: Date): Boolean {
        return first.getFullYear() == second.getFullYear() &&
                first.getMonth() == second.getMonth() &&
                first.getDate() == second.getDate()
    }

    private fun isAllowed(date: Date): Boolean {
        return allowedDates?.contains(date.toIsoDate()) != false
    }

    private fun isWithinRange(date: Date): Boolean {
        val time = date.getTime()
        return time in effectiveMinDate.getTime()..effectiveMaxDate.getTime()
    }

    private fun clampToRange(date: Date): Date {
        val time = date.getTime()
        val min = effectiveMinDate.getTime()
        val max = effectiveMaxDate.getTime()
        return when {
            time < min -> Date(effectiveMinDate.getTime())
            time > max -> Date(effectiveMaxDate.getTime())
            else -> date
        }
    }

    private fun Date.toIsoDate(): String {
        val year = this.getFullYear()
        val month = (this.getMonth() + 1).toString().padStart(2, '0')
        val day = this.getDate().toString().padStart(2, '0')
        return "$year-$month-$day"
    }

    private fun syncSelectors() {
        monthSelect.value = displayedMonth.toString()
        yearSelect.value = displayedYear.toString()
    }

    private fun buildYearOptions(): List<Pair<String, String>> {
        val start = effectiveMinDate.getFullYear()
        val end = effectiveMaxDate.getFullYear()
        val years = (start..end).toList()
        return years.reversed().map { it.toString() to it.toString() }
    }
}

fun Container.dateCalendar(
    initialDate: Date?,
    minDate: Date? = null,
    maxDate: Date? = null,
    availableDates: Set<String>? = null,
    onDateSelected: (Date) -> Unit
): DateCalendar = DateCalendar(initialDate, minDate, maxDate, availableDates, onDateSelected).also { add(it) }
