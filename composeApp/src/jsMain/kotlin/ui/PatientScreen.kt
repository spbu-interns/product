package ui

import api.ApiConfig
import io.kvision.core.Container
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.h3
import io.kvision.html.h4
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.panel.vPanel

fun Container.patientScreen(onLogout: () -> Unit = { Navigator.showHome() }) = vPanel(spacing = 12) {
    headerBar(
        mode = HeaderMode.PATIENT,
        active = NavTab.NONE,
        onLogout = {
            ApiConfig.clearToken()
            Session.clear()
            Navigator.showHome()
        }
    )

    patientAccountLayout(active = PatientSection.OVERVIEW) {
        h1("Аккаунт", className = "account title")

        div(className = "statistics grid") {
            // TODO: Заменить на реальные данные из API
            statisticsCard("0", "Предстоящие", "\uD83D\uDCC5")
            statisticsCard("0", "Записи", "\uD83D\uDCC4")
            statisticsCard("0", "Врачи", "\uD83D\uDC64")
        }

        div(className = "card block appointment-block") {
            h4("Следующий приём", className = "block title")
            // TODO: Заменить на реальные данные из API
            div(className = "empty-state") {
                p("Нет предстоящих приёмов")
                button("Найти врача", className = "btn-primary-lg").onClick {
                    Navigator.showFind()
                }
            }
        }

        h4("Последние медицинские записи", className = "block title")

        div(className = "card block") {
            div(className = "records list") {
                // TODO: Заменить на реальные данные из API
                div(className = "empty-state") {
                    p("Нет медицинских записей")
                }
            }
        }
    }
}

private fun Container.statisticsCard(value: String, label: String, icon: String) {
    div(className = "statistics card") {
        span(icon, className = "statistics icon")
        h3(value, className = "statistics value")
        span(label, className = "statistics label")
    }
}