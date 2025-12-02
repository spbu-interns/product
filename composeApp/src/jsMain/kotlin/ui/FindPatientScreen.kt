package ui

import api.ApiConfig
import api.PatientApiClient
import io.kvision.core.Container
import io.kvision.form.text.text
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h2
import io.kvision.html.h4
import io.kvision.html.span
import io.kvision.panel.vPanel
import io.kvision.toast.Toast
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.interns.project.dto.UserResponseDto

fun Container.findPatientScreen(onLogout: () -> Unit = { Navigator.showHome() }) = vPanel(spacing = 12) {
    val uiScope = MainScope()
    val api = PatientApiClient()
    lateinit var resultsContainer: Container

    fun renderResults(results: List<UserResponseDto>) {
        resultsContainer.removeAll()
        if (results.isEmpty()) {
            resultsContainer.div(className = "card block") {
                span("Пациенты не найдены", className = "record subtitle")
            }
            return
        }

        results.forEach { patient ->
            resultsContainer.div(className = "record item card") {
                h4(listOfNotNull(patient.surname, patient.name, patient.patronymic).joinToString(" ").ifBlank { "Пациент #${patient.id}" }, className = "record title")
                patient.phoneNumber?.let { span(it, className = "record subtitle") }
                span("ID: ${patient.id}", className = "record subtitle")
                span(patient.email, className = "record subtitle")
                button("Открыть профиль", className = "btn-secondary").onClick {
                    val userId = patient.id
                    uiScope.launch {
                        val clientId = api.getClientProfile(userId).getOrNull()?.id
                        uiScope.cancel()
                        Navigator.showDoctorPatient(userId, clientId)
                    }
                }
            }
        }
    }

    headerBar(
        mode = HeaderMode.DOCTOR,
        active = NavTab.FIND,
        onLogout = {
            ApiConfig.clearToken()
            Session.clear()
            uiScope.cancel()
            onLogout()
        }
    )

    div(className = "container") {
        h2("Найти пациента", className = "find-title")

        val searchField = text(label = "ФИО или ID") {
            placeholder = "Введите фамилию и имя или числовой ID"
            addCssClass("kv-input")
        }

        val searchButton = button("Поиск", className = "btn btn-primary")

        div(className = "card block") {
            div(className = "form row") {
                div(className = "form field") { add(searchField) }
                div(className = "form actions") {
                    add(searchButton)
                }
            }
        }

        resultsContainer = div(className = "search-results")

        searchButton.onClick {
            val query = searchField.value?.trim().orEmpty()
            if (query.isBlank()) {
                Toast.danger("Введите ФИО или числовой ID")
                return@onClick
            }
            resultsContainer.removeAll()
            resultsContainer.div(className = "card block") { span("Поиск...") }

            uiScope.launch {
                val idValue = query.toLongOrNull()
                val result = if (idValue != null) {
                    api.getPatientProfile(idValue).mapCatching { user ->
                        val clientProfile = api.getClientProfile(user.id).getOrThrow()
                        if (user.role != "CLIENT" || clientProfile == null) {
                            throw IllegalStateException("Пользователь не является пациентом")
                        }
                        listOf(user)
                    }
                } else {
                    api.listPatients().map { list ->
                        list.filter {
                            val fullName = listOfNotNull(it.surname, it.name, it.patronymic).joinToString(" ").lowercase()
                            fullName.contains(query.lowercase()) || (it.id.toString() == query)
                        }
                    }
                }

                result.fold(
                    onSuccess = { renderResults(it) },
                    onFailure = { err ->
                        resultsContainer.removeAll()
                        resultsContainer.div(className = "card block") {
                            span(err.message ?: "Ошибка поиска", className = "record subtitle")
                        }
                        Toast.danger(err.message ?: "Ошибка поиска")
                    }
                )
            }
        }
    }
}