package state

import api.BookingApiClient
import api.DoctorApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.interns.project.dto.DoctorPatientDto
import org.interns.project.dto.FullUserProfileDto
import org.interns.project.dto.SlotResponse
import kotlin.js.Date

object DoctorState {
    private val scope = MainScope()
    private val apiClient = DoctorApiClient()
    private val bookingApiClient = BookingApiClient()

    var dashboardData: FullUserProfileDto? = null
        private set
    var isLoading: Boolean = false
        private set
    var error: String? = null
        private set

    var todaysSlots: List<SlotResponse> = emptyList()
        private set
    var slotsError: String? = null
        private set

    var activeAppointment: ActiveAppointmentInfo? = null
        private set

    var isCompleting: Boolean = false
        private set

    private val listeners = mutableSetOf<() -> Unit>()
    private var activeWatcher: Job? = null
    private var finishedAt: Double? = null

    fun subscribe(listener: () -> Unit): () -> Unit {
        listeners += listener
        return { listeners -= listener }
    }

    fun loadDoctorDashboard(userId: Long) {
        if (isLoading) return

        isLoading = true
        error = null
        notifyUpdate()

        scope.launch {
            try {
                dashboardData = apiClient.getDoctorDashboard(userId).getOrThrow()
                error = null

                dashboardData?.doctor?.id?.let { doctorId ->
                    loadTodaySlots(doctorId)
                }

                updateActiveAppointment(forceNotify = true)
                startWatcher()
            } catch (e: Exception) {
                error = "Ошибка загрузки данных: ${e.message}"
                dashboardData = null
                todaysSlots = emptyList()
                activeAppointment = null
            } finally {
                isLoading = false
                notifyUpdate()
            }
        }
    }

    fun refresh(userId: Long) {
        loadDoctorDashboard(userId)
    }

    fun clear() {
        dashboardData = null
        error = null
        isLoading = false
        todaysSlots = emptyList()
        activeAppointment = null
        activeWatcher?.cancel()
        activeWatcher = null
        notifyUpdate()
    }

    private fun notifyUpdate() {
        listeners.forEach { it.invoke() }
    }

    private fun startWatcher() {
        if (activeWatcher?.isActive == true) return
        activeWatcher = scope.launch {
            while (true) {
                updateActiveAppointment()
                delay(30_000)
            }
        }
    }

    private suspend fun loadTodaySlots(doctorId: Long) {
        slotsError = null
        val today = currentDate()
        val response = bookingApiClient.listDoctorSlots(doctorId, today)
        todaysSlots = response.getOrElse { emptyList() }
        slotsError = response.exceptionOrNull()?.message
    }

    private fun updateActiveAppointment(forceNotify: Boolean = false) {
        val previous = activeAppointment
        val next = calculateActiveAppointment()

        if (next?.status == ActiveAppointmentStatus.FINISHED) {
            val hasNewFinish = previous?.appointment?.id != next.appointment.id ||
                    previous.status != ActiveAppointmentStatus.FINISHED

            val finishTime = next.appointment.completedAt?.let { Date(it).getTime() }
                ?: next.end.getTime()

            if (hasNewFinish || finishedAt == null) {
                finishedAt = finishTime
            }

            val cutoff = finishedAt ?: finishTime
            activeAppointment = if (Date().getTime() - cutoff < 60_000) next else null
        } else {
            finishedAt = null
            activeAppointment = next
        }

        val changed = previous?.appointment?.id != activeAppointment?.appointment?.id ||
                previous?.status != activeAppointment?.status

        if (forceNotify || changed) {
            notifyUpdate()
        }
    }

    private fun calculateActiveAppointment(): ActiveAppointmentInfo? {
        val appointments = dashboardData?.appointments ?: return null
        if (appointments.isEmpty()) return null

        val slotMap = todaysSlots.associateBy { it.id }
        val patients = dashboardData?.patients?.associateBy(DoctorPatientDto::clientId) ?: emptyMap()
        val now = Date()

        val candidates = appointments.mapNotNull { appointment ->
            val slot = slotMap[appointment.slotId] ?: return@mapNotNull null
            val start = Date(slot.startTime)
            val end = Date(slot.endTime)
            if (now.getTime() < start.getTime()) return@mapNotNull null

            val status = if (appointment.status.equals("COMPLETED", ignoreCase = true) ||
                now.getTime() >= end.getTime()
            ) {
                ActiveAppointmentStatus.FINISHED
            } else {
                ActiveAppointmentStatus.ACTIVE
            }

            ActiveAppointmentInfo(
                appointment = appointment,
                slot = slot,
                patient = patients[appointment.clientId],
                status = status,
                start = start,
                end = end
            )
        }

        return candidates.maxByOrNull { it.start.getTime() }
    }

    fun completeAppointment(appointmentId: Long, userId: Long, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (isCompleting) return

        isCompleting = true
        notifyUpdate()

        scope.launch {
            try {
                val result = bookingApiClient.completeAppointment(appointmentId)
                val success = result.getOrElse { false }
                if (success) {
                    onResult(true, null)
                    loadDoctorDashboard(userId)
                } else {
                    onResult(false, "Не удалось завершить приём")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            } finally {
                isCompleting = false
                notifyUpdate()
            }
        }
    }

    private fun currentDate(): String = Date().toISOString().substring(0, 10)
}

data class ActiveAppointmentInfo(
    val appointment: org.interns.project.dto.AppointmentDto,
    val slot: SlotResponse,
    val patient: DoctorPatientDto?,
    val status: ActiveAppointmentStatus,
    val start: Date,
    val end: Date
)

enum class ActiveAppointmentStatus { ACTIVE, FINISHED }