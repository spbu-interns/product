// state/DoctorState.kt
package state

import api.DoctorApiClient
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.interns.project.dto.FullUserProfileDto

object DoctorState {
    private val scope = MainScope()
    private val apiClient = DoctorApiClient()

    var dashboardData: FullUserProfileDto? = null
        private set
    var isLoading: Boolean = false
        private set
    var error: String? = null
        private set

    var onUpdate: (() -> Unit)? = null

    fun loadDoctorDashboard(userId: Long) {
        if (isLoading) return

        isLoading = true
        error = null
        notifyUpdate()

        scope.launch {
            try {
                dashboardData = apiClient.getDoctorDashboard(userId).getOrThrow()
                error = null
            } catch (e: Exception) {
                error = "Ошибка загрузки данных: ${e.message}"
                dashboardData = null
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
        notifyUpdate()
    }

    private fun notifyUpdate() {
        onUpdate?.invoke()
    }
}