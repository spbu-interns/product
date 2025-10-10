package ui

import io.kvision.core.Container
import io.kvision.form.text.Text
import io.kvision.html.Button
import io.kvision.html.ButtonStyle
import io.kvision.html.Span
import io.kvision.html.div
import io.kvision.html.h3
import io.kvision.panel.vPanel
import io.kvision.utils.perc
import io.kvision.utils.px

fun Container.resetPasswordScreen() = vPanel(spacing = 16) {
    headerBar(mode = HeaderMode.PUBLIC, active = NavTab.NONE)

    vPanel(spacing = 16) {
        width = 520.px
        addCssClass("mx-auto")

        h3("Восстановление пароля")

        val emailField = Text(label = "Email").apply {
            width = 100.perc
        }

        val error = Span("").apply {
            addCssClass("text-danger")
        }

        div {
            add(emailField)
            add(error)
        }

        add(Button("Отправить ссылку для восстановления пароля", style = ButtonStyle.PRIMARY).apply {
            width = 100.perc
            onClick {
                val email = emailField.value ?: ""
                if (EMAIL_REGEX.matches(email)) {
                    Navigator.showStub("Ссылка отправлена на $email")
                } else {
                    error.content = "Некорректный email"
                    return@onClick
                }
            }
        })
    }
}

private val EMAIL_REGEX =
    Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}\$")