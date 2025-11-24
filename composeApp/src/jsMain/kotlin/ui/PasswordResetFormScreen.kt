package ui

import api.AuthApiClient
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.form.text.Text
import io.kvision.html.Button
import io.kvision.html.ButtonStyle
import io.kvision.html.InputType
import io.kvision.html.Span
import io.kvision.html.div
import io.kvision.html.h3
import io.kvision.panel.vPanel
import io.kvision.utils.perc
import io.kvision.utils.px
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import ui.components.PASSWORD_REGEX

fun Container.passwordResetFormScreen(token: String?) = vPanel(spacing = 16) {
    headerBar(mode = HeaderMode.PUBLIC, active = NavTab.NONE)
    val uiScope = MainScope()

    vPanel(spacing = 16) {
        width = 520.px
        addCssClass("mx-auto")

        h3("Смена пароля")

        val newPasswordField = Text(label = "Новый пароль", type = InputType.PASSWORD).apply {
            width = 100.perc
        }
        val confirmPasswordField = Text(label = "Подтверждение пароля", type = InputType.PASSWORD).apply {
            width = 100.perc
        }

        val error = Span("").apply {
            addCssClass("text-danger")
        }

        div {
            add(newPasswordField)
            add(confirmPasswordField)
            add(error)
        }

        add(Button("Сменить пароль", style = ButtonStyle.PRIMARY).apply {
            width = 100.perc
            onClick {
                val newPassword = newPasswordField.value ?: ""
                val confirmPassword = confirmPasswordField.value ?: ""
                val resetToken = token?.trim().orEmpty()

                when {
                    resetToken.isBlank() -> {
                        error.content = "Недействительная ссылка для восстановления пароля"
                        return@onClick
                    }
                    newPassword.isBlank() -> {
                        error.content = "Введите новый пароль"
                        return@onClick
                    }
                    !PASSWORD_REGEX.matches(newPassword) -> {
                        error.content = "Пароль: 8–71 символ, латиница, минимум 1 цифра"
                        return@onClick
                    }
                    newPassword != confirmPassword -> {
                        error.content = "Пароли не совпадают"
                        return@onClick
                    }
                }

                error.content = ""
                this.disabled = true

                uiScope.launch {
                    val authClient = AuthApiClient()
                    val result = authClient.resetPassword(resetToken, newPassword)
                    result.fold(
                        onSuccess = {
                            Navigator.showPasswordResetSuccess()
                        },
                        onFailure = { e ->
                            error.content = e.message ?: "Не удалось сменить пароль"
                            this@apply.disabled = false
                        }
                    )
                }
            }
        })
    }
}