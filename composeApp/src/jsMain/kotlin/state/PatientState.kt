// state/PatientState.kt
package state

import api.PatientApiClient
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.interns.project.dto.FullUserProfileDto
import ui.Session

object PatientState {
    private val scope = MainScope()
    private val apiClient = PatientApiClient()

    var dashboardData: FullUserProfileDto? = null
        private set
    var isLoading: Boolean = false
        private set
    var error: String? = null
        private set

    var onUpdate: (() -> Unit)? = null

    // Основной метод загрузки всех данных
    fun loadPatientDashboard(userId: Long) {
        if (isLoading) return

        isLoading = true
        error = null
        notifyUpdate()

        scope.launch {
            try {
                dashboardData = apiClient.getPatientDashboard(userId).getOrThrow()
                dashboardData?.user?.let { user ->
                    Session.updateFrom(user)
                }
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

    private fun notifyUpdate() {
        onUpdate?.invoke()
    }
}