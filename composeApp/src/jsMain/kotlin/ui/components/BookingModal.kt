package ui.components

import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.core.onEvent
import io.kvision.form.select.select
import io.kvision.form.text.textArea
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h3
import io.kvision.html.p
import io.kvision.panel.SimplePanel
import io.kvision.panel.simplePanel
import io.kvision.toast.Toast

data class BookingDay(
    val day: Int,
    val isCurrentMonth: Boolean = true,
    val isAvailable: Boolean = false
)

class BookingModalController internal constructor(
    private val renderModal: () -> Unit,
    private val openAction: (String) -> Unit,
    private val closeAction: () -> Unit
) {
    fun open(doctorName: String) = openAction(doctorName)
    fun close() = closeAction()
    internal fun render() = renderModal()
}

fun Container.bookingModal(
    bookingMonthTitle: String = "Ноябрь 2025",
    bookingCalendar: List<List<BookingDay>> = defaultBookingCalendar(),
    daySlotMap: Map<Int, List<String>> = defaultDaySlotMap()
): BookingModalController {
    var bookingVisible = false
    var selectedDoctor: String? = null
    var selectedDay: BookingDay? = null
    var selectedTime: String? = null
    var showingTimes = false

    lateinit var renderBookingModal: () -> Unit

    val bookingOverlay: SimplePanel = simplePanel(className = "booking-overlay-root") {
        visible = false
    }

    fun resetBookingState() {
        bookingVisible = false
        selectedDoctor = null
        selectedDay = null
        selectedTime = null
        showingTimes = false
    }

    fun closeBookingModal() {
        resetBookingState()
        renderBookingModal()
    }

    fun openBookingModal(doctorName: String) {
        bookingVisible = true
        selectedDoctor = doctorName
        selectedDay = null
        selectedTime = null
        showingTimes = false
        renderBookingModal()
    }

    renderBookingModal = render@{
        bookingOverlay.removeAll()
        bookingOverlay.visible = bookingVisible

        if (!bookingVisible) return@render

        bookingOverlay.div(className = "booking-overlay") {
            div(className = "booking-backdrop").onClick { closeBookingModal() }

            div(className = "booking-modal") {
                div(className = "booking-modal-header") {
                    h3("Запись к ${selectedDoctor ?: "врачу"}", className = "booking-title")
                    button("×", className = "booking-close").onClick { closeBookingModal() }
                }

                div(className = "booking-body") {
                    p(
                        "Выберите удобную дату и время приёма. После выбора времени нажмите «Подтвердить запись».",
                        className = "booking-hint"
                    )

                    div(className = "booking-card") {
                        if (!showingTimes) {
                            div(className = "booking-card-header") {
                                h3(bookingMonthTitle, className = "booking-card-title")
                                p("Выберите дату", className = "booking-subtitle")
                            }

                            div(className = "booking-weekdays") {
                                listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { weekday ->
                                    div(weekday, className = "booking-weekday")
                                }
                            }

                            bookingCalendar.forEach { week ->
                                div(className = "booking-week") {
                                    week.forEach { day ->
                                        val dayClasses = mutableListOf("booking-day")
                                        if (!day.isCurrentMonth) dayClasses += "is-muted"
                                        if (day.isAvailable && day.isCurrentMonth) dayClasses += "is-available"
                                        if (selectedDay?.day == day.day && day.isCurrentMonth) dayClasses += "is-selected"

                                        div(day.day.toString(), className = dayClasses.joinToString(" ")) {
                                            if (day.isAvailable && day.isCurrentMonth) {
                                                onClick {
                                                    selectedDay = day
                                                    showingTimes = true
                                                    renderBookingModal()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val targetDay = selectedDay?.day
                            val slots = daySlotMap[targetDay] ?: emptyList()

                            div(className = "booking-card-header") {
                                h3("${targetDay ?: "День"} $bookingMonthTitle", className = "booking-card-title")
                                button("← Изменить дату", className = "booking-link").onClick {
                                    showingTimes = false
                                    selectedTime = null
                                    renderBookingModal()
                                }
                            }

                            if (slots.isEmpty()) {
                                p("На выбранный день нет свободных слотов.", className = "booking-empty")
                            } else {
                                p("Выберите время", className = "booking-subtitle")
                                div(className = "booking-time-grid") {
                                    slots.forEach { slot ->
                                        val slotClasses = mutableListOf("booking-time")
                                        if (slot == selectedTime) slotClasses += "is-selected"

                                        button(slot, className = slotClasses.joinToString(" ")) {
                                            onClick {
                                                selectedTime = slot
                                                renderBookingModal()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    div(className = "booking-card booking-form") {
                        h3("Детали записи", className = "booking-card-title")

                        div(className = "booking-form-row") {
                            p("Тип приёма", className = "booking-label")
                            val typeSelect = select(
                                options = listOf(
                                    "online" to "Онлайн консультация",
                                    "office" to "Очный приём",
                                    "home" to "Вызов на дом"
                                )
                            ) {
                                addCssClass("booking-select")
                                value = "online"
                            }
                            typeSelect.onEvent { change = { /* UI only */ } }
                        }

                        div(className = "booking-form-row") {
                            p("Комментарий", className = "booking-label")
                            textArea {
                                addCssClass("booking-notes")
                                placeholder = "Опишите симптомы или пожелания"
                            }
                        }

                        button("Подтвердить запись", className = "btn btn-primary booking-submit") {
                            disabled = selectedTime == null || selectedDay == null
                            onClick {
                                if (selectedDay != null && selectedTime != null) {
                                    Toast.success("Запись создана на ${selectedDay!!.day} $bookingMonthTitle, $selectedTime")
                                    closeBookingModal()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    return BookingModalController(
        renderModal = renderBookingModal,
        openAction = ::openBookingModal,
        closeAction = ::closeBookingModal
    )
}

private fun defaultDaySlotMap(): Map<Int, List<String>> = mapOf(
    1 to listOf("09:00", "10:30", "14:00", "15:30"),
    2 to listOf("09:00", "11:00", "12:30", "16:30"),
    3 to listOf("08:30", "10:00", "13:30", "15:00", "17:00"),
    6 to listOf("09:00", "10:00", "13:00", "15:00", "16:30"),
    7 to listOf("09:30", "11:30", "14:30", "16:00"),
    9 to listOf("10:00", "12:00", "15:00", "17:30"),
    12 to listOf("09:00", "11:00", "13:00", "15:30"),
    14 to listOf("09:30", "10:30", "14:00"),
    18 to listOf("08:30", "10:00", "12:30", "16:00"),
    20 to listOf("09:00", "11:30", "14:30", "17:00"),
    22 to listOf("10:00", "12:00", "15:00", "16:30"),
    25 to listOf("09:00", "10:30", "13:30", "15:30"),
    27 to listOf("09:30", "11:30", "14:30"),
    29 to listOf("10:00", "12:00", "15:00", "16:00"),
    30 to listOf("09:00", "11:00", "13:00", "16:00")
)

private fun defaultBookingCalendar(): List<List<BookingDay>> = listOf(
    listOf(
        BookingDay(26, isCurrentMonth = false),
        BookingDay(27, isCurrentMonth = false),
        BookingDay(28, isCurrentMonth = false),
        BookingDay(29, isCurrentMonth = false),
        BookingDay(30, isCurrentMonth = false),
        BookingDay(1, isAvailable = true),
        BookingDay(2, isAvailable = true)
    ),
    listOf(
        BookingDay(3, isAvailable = true),
        BookingDay(4, isAvailable = true),
        BookingDay(5, isAvailable = true),
        BookingDay(6, isAvailable = true),
        BookingDay(7, isAvailable = true),
        BookingDay(8, isAvailable = true),
        BookingDay(9, isAvailable = true)
    ),
    listOf(
        BookingDay(10, isAvailable = true),
        BookingDay(11, isAvailable = true),
        BookingDay(12, isAvailable = true),
        BookingDay(13, isAvailable = true),
        BookingDay(14, isAvailable = true),
        BookingDay(15, isAvailable = true),
        BookingDay(16, isAvailable = true)
    ),
    listOf(
        BookingDay(17, isAvailable = true),
        BookingDay(18, isAvailable = true),
        BookingDay(19, isAvailable = true),
        BookingDay(20, isAvailable = true),
        BookingDay(21, isAvailable = true),
        BookingDay(22, isAvailable = true),
        BookingDay(23, isAvailable = true)
    ),
    listOf(
        BookingDay(24, isAvailable = true),
        BookingDay(25, isAvailable = true),
        BookingDay(26, isAvailable = true),
        BookingDay(27, isAvailable = true),
        BookingDay(28, isAvailable = true),
        BookingDay(29, isAvailable = true),
        BookingDay(30, isAvailable = true)
    )
)