package ui.components

import api.BookingApiClient
import api.PatientApiClient
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.core.onEvent
import io.kvision.form.select.Select
import io.kvision.form.text.textArea
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h3
import io.kvision.html.p
import io.kvision.panel.SimplePanel
import io.kvision.panel.simplePanel
import io.kvision.toast.Toast
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.interns.project.dto.AppointmentCreateRequest
import state.PatientState
import ui.DoctorProfile
import ui.Session
import kotlin.js.Date

data class SlotOption(
    val id: Long,
    val label: String
)

class BookingModalController internal constructor(
    private val renderModal: () -> Unit,
    private val openAction: (DoctorProfile) -> Unit,
    private val closeAction: () -> Unit
) {
    fun open(doctor: DoctorProfile) = openAction(doctor)
    fun close() = closeAction()
    internal fun render() = renderModal()
}

fun Container.bookingModal(
    onAppointmentsUpdated: (() -> Unit)? = null
): BookingModalController {
    val uiScope = MainScope()
    val apiClient = BookingApiClient()
    val patientApiClient = PatientApiClient()

    var bookingVisible = false
    var selectedDoctor: DoctorProfile? = null
    var selectedDate: Date? = null
    var selectedSlotId: Long? = null
    var notesText = ""

    var availableDates: Set<String> = emptySet()
    var availableSlots: List<SlotOption> = emptyList()

    var isLoadingDates = false
    var isLoadingSlots = false
    var bookingInProgress = false
    var cachedClientId: Long? = null

    var minDate = todayDateOnly()
    var maxDate = addMonthsClamped(minDate, 6)

    var calendar: DateCalendar? = null
    lateinit var renderBookingModal: () -> Unit

    val bookingOverlay: SimplePanel = simplePanel(className = "booking-overlay-root") {
        visible = false
    }

    fun resetBookingState() {
        bookingVisible = false
        selectedDoctor = null
        selectedDate = null
        selectedSlotId = null
        notesText = ""
        availableDates = emptySet()
        availableSlots = emptyList()
        isLoadingDates = false
        isLoadingSlots = false
        bookingInProgress = false
    }

    fun closeBookingModal() {
        resetBookingState()
        renderBookingModal()
    }

    fun loadSlotsForDate(date: Date) {
        val doctorId = selectedDoctor?.doctorId ?: return
        isLoadingSlots = true
        selectedSlotId = null
        renderBookingModal()

        uiScope.launch {
            val response = apiClient.listDoctorSlots(doctorId, date.toIsoDate())
            response.onSuccess { slots ->
                availableSlots = slots
                    .filter { !it.isBooked }
                    .mapNotNull { slot ->
                        val start = extractDateAndTime(slot.startTime)?.second ?: return@mapNotNull null
                        val end = extractDateAndTime(slot.endTime)?.second
                        val label = end?.let { "$start–$it" } ?: start
                        SlotOption(id = slot.id, label = label)
                    }
                    .sortedBy { parseTimeToMinutes(it.label.substringBefore("–")) ?: Int.MAX_VALUE }

                if (availableSlots.isEmpty()) {
                    selectedSlotId = null
                }
            }.onFailure {
                Toast.danger("Не удалось загрузить слоты на выбранный день")
            }

            isLoadingSlots = false
            renderBookingModal()
        }
    }

    fun loadAvailableDates() {
        val doctorId = selectedDoctor?.doctorId ?: return
        isLoadingDates = true
        availableDates = emptySet()
        renderBookingModal()

        uiScope.launch {
            val response = apiClient.listDoctorSlots(doctorId)
            response.onSuccess { slots ->
                val allowedDates = slots
                    .filter { !it.isBooked }
                    .mapNotNull { slot ->
                        val isoDate = extractDateAndTime(slot.startTime)?.first ?: return@mapNotNull null
                        val date = parseIsoDate(isoDate) ?: return@mapNotNull null
                        if (isWithinRange(date, minDate, maxDate)) date.toIsoDate() else null
                    }
                    .toSet()

                availableDates = allowedDates

                if (availableDates.isNotEmpty()) {
                    calendar?.setAllowedDates(availableDates)
                }

                val firstDateIso = availableDates.minOrNull()
                selectedDate = firstDateIso?.let { parseIsoDate(it) }

                selectedDate?.let { date ->
                    calendar?.setSelectedDate(date)
                    loadSlotsForDate(date)
                }
            }.onFailure {
                Toast.danger("Не удалось загрузить доступные даты врача")
            }

            isLoadingDates = false
            renderBookingModal()
        }
    }

    fun openBookingModal(doctor: DoctorProfile) {
        bookingVisible = true
        selectedDoctor = doctor
        selectedDate = null
        selectedSlotId = null
        notesText = ""
        availableSlots = emptyList()
        availableDates = emptySet()
        minDate = todayDateOnly()
        maxDate = addMonthsClamped(minDate, 6)

        renderBookingModal()
        loadAvailableDates()
    }

    fun submitBooking() {
        val slotId = selectedSlotId
        val userId = Session.userId
        if (slotId == null || selectedDate == null) return
        if (userId == null) {
            Toast.danger("Требуется войти как пациент, чтобы создать запись")
            return
        }

        bookingInProgress = true
        renderBookingModal()

        uiScope.launch {
            val clientId = cachedClientId ?: patientApiClient.getClientId(userId).getOrElse { error ->
                Toast.danger(error.message ?: "Не удалось определить профиль пациента")
                null
            }

            if (clientId == null) {
                bookingInProgress = false
                renderBookingModal()
                return@launch
            }

            cachedClientId = clientId

            val request = AppointmentCreateRequest(
                slotId = slotId,
                clientId = clientId,
                comments = notesText.takeIf { it.isNotBlank() }
            )

            apiClient.bookAppointment(request)
                .onSuccess {
                    Toast.success("Запись подтверждена")
                    closeBookingModal()
                    onAppointmentsUpdated?.invoke()
                    Session.userId?.let { PatientState.loadPatientDashboard(it) }
                }
                .onFailure {
                    Toast.danger("Не удалось создать запись")
                }

            bookingInProgress = false
            renderBookingModal()
        }
    }

    renderBookingModal = render@{
        bookingOverlay.removeAll()
        bookingOverlay.visible = bookingVisible

        if (!bookingVisible) return@render

        bookingOverlay.div(className = "booking-overlay") {
            div(className = "booking-backdrop").onClick { closeBookingModal() }

            div(className = "booking-modal") {
                div(className = "booking-modal-header") {
                    h3(
                        "Запись к ${selectedDoctor?.name ?: "врачу"}",
                        className = "booking-title"
                    )
                    button("×", className = "booking-close").onClick { closeBookingModal() }
                }

                selectedDoctor?.let { doctor ->
                    div(className = "booking-price-line") {
                        p("${doctor.price} ₽ / приём", className = "booking-price-value")
                        p(
                            "Может вырасти при добавлении услуг",
                            className = "booking-price-note"
                        )
                    }
                }

                div(className = "booking-body") {
                    div(className = "booking-card") {
                        div(className = "booking-card-header") {
                            h3("Календарь", className = "booking-card-title")
                            p("Выберите дату", className = "booking-subtitle")
                        }
                        div(className = "booking-calendar-row") {
                            div(className = "booking-calendar-col") {
                                if (isLoadingDates) {
                                    p("Загружаем доступные даты...", className = "booking-hint")
                                } else if (availableDates.isEmpty()) {
                                    p(
                                        "Нет свободных дат в ближайшие 6 месяцев",
                                        className = "booking-empty"
                                    )
                                } else {
                                    calendar = dateCalendar(
                                        initialDate = selectedDate,
                                        minDate = minDate,
                                        maxDate = maxDate,
                                        availableDates = availableDates
                                    ) { date ->
                                        selectedDate = date
                                        selectedSlotId = null
                                        loadSlotsForDate(date)
                                    }
                                }
                            }
                            div(className = "booking-time-col") {
                                if (selectedDate != null) {
                                    div(className = "booking-card-header") {
                                        h3("Доступное время", className = "booking-card-title")
                                        p(selectedDate?.toIsoDate().orEmpty(), className = "booking-subtitle")
                                    }

                                    when {
                                        isLoadingSlots -> {
                                            p("Загружаем свободные слоты...", className = "booking-hint")
                                        }

                                        availableSlots.isEmpty() -> {
                                            p(
                                                "На выбранный день нет свободных слотов",
                                                className = "booking-empty"
                                            )
                                        }

                                        else -> {
                                            div(className = "booking-time-grid") {
                                                availableSlots.forEach { slot ->
                                                    val slotClasses = mutableListOf("booking-time")
                                                    if (slot.id == selectedSlotId) slotClasses += "is-selected"

                                                    button(slot.label, className = slotClasses.joinToString(" ")) {
                                                        onClick {
                                                            selectedSlotId = slot.id
                                                            renderBookingModal()
                                                        }
                                                    }
                                                }
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

                            val typeSelect = Select(
                                options = listOf(
                                    "first" to "Первичный приём",
                                    "secondary" to "Повторный приём",
                                    "regular" to "Плановый осмотр",
                                    "consultation" to "Консультация",
                                ),
                                label = "Укажите тип приёма",
                            ).apply {
                                addCssClass("booking-select")
                                value = "first"
                            }

                            add(typeSelect)

                            typeSelect.onEvent {
                                change = {
                                    // логики пока нет, но место есть
                                }
                            }
                        }

                        div(className = "booking-form-row") {
                            p("Комментарий", className = "booking-label")
                            textArea {
                                addCssClass("booking-notes")
                                placeholder = "Опишите симптомы или пожелания"
                                value = notesText
                                onEvent {
                                    input = {
                                        notesText = value ?: ""
                                    }
                                }
                            }
                        }

                        button("Подтвердить запись", className = "btn btn-primary booking-submit") {
                            disabled = selectedSlotId == null || selectedDate == null || bookingInProgress
                            onClick {
                                submitBooking()
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

private fun extractDateAndTime(iso: String): Pair<String, String>? {
    val date = iso.substringBefore("T", "")
    val timeRaw = iso.substringAfter("T", "").takeWhile { it != 'Z' && it != '+' }
    val timeParts = timeRaw.split(":")
    if (date.isBlank() || timeParts.size < 2) return null
    val hours = timeParts.getOrNull(0)?.padStart(2, '0') ?: return null
    val minutes = timeParts.getOrNull(1)?.padStart(2, '0') ?: return null
    return date to "$hours:$minutes"
}

private fun parseTimeToMinutes(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

private fun parseIsoDate(value: String): Date? {
    val parts = value.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull()?.minus(1) ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    return Date(year, month, day)
}

private fun Date.toIsoDate(): String {
    val year = this.getFullYear()
    val month = (this.getMonth() + 1).toString().padStart(2, '0')
    val day = this.getDate().toString().padStart(2, '0')
    return "$year-$month-$day"
}

private fun isWithinRange(date: Date, start: Date, end: Date): Boolean {
    val time = date.getTime()
    return time in start.getTime()..end.getTime()
}
