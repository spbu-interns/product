package ui

import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.core.onEvent
import io.kvision.form.text.Text
import io.kvision.html.Button
import io.kvision.html.InputType
import io.kvision.panel.vPanel
import io.kvision.utils.px
import io.kvision.utils.perc
import io.kvision.html.ButtonStyle
import io.kvision.html.Span
import io.kvision.panel.hPanel

fun Container.registrationScreen(
    onRegistered: () -> Unit,
    onGoToLogin: () -> Unit,
    onGoHome: () -> Unit
) = vPanel(spacing = 16) {
    width = 520.px
    addCssClass("mx-auto")

    homeIconButton { onGoHome() }

    val email = Text(label = "Email", type = InputType.EMAIL).apply { width = 100.perc }
    val password = Text(label = "Пароль", type = InputType.PASSWORD).apply { width = 100.perc }
    val confirmation = Text(label = "Подтверждение пароля", type = InputType.PASSWORD).apply { width = 100.perc }

    var pv = false
    val togglePassword = Button("👁", style = ButtonStyle.LIGHT).apply {
        onClick {
            pv = !pv
            password.type = if (pv) InputType.TEXT else InputType.PASSWORD
            text = if (pv) "🙈" else "👁"
        }
    }

    var cv = false
    val toggleConfirmation = Button("👁", style = ButtonStyle.LIGHT).apply {
        onClick {
            cv = !cv
            confirmation.type = if (cv) InputType.TEXT else InputType.PASSWORD
            text = if (cv) "🙈" else "👁"
        }
    }

    confirmation.onEvent {
        paste = { it.preventDefault() }
        drop = { it.preventDefault() }
    }

    val error = Span("").apply { addCssClass("text-danger") }

    add(email)
    hPanel(spacing = 8) { add(password); add(togglePassword) }
    hPanel(spacing = 8) { add(confirmation); add(toggleConfirmation) }
    add(error)

    add(Button("Зарегистрироваться", style = ButtonStyle.PRIMARY).apply {
        width = 100.perc
        onClick {
            val emailOk = EMAIL_REGEX.matches(email.value ?: "")
            val passwordOk = PASSWORD_REGEX.matches(password.value ?: "")
            val same = (password.value ?: "") == (confirmation.value ?: "")
            when {
                !emailOk -> error.content = "Некорректный email"
                !passwordOk -> error.content = "Пароль: 8 - 71 символ, латиница, минимум 1 цифра"
                !same -> error.content = "Пароли не совпадают"
                else -> { error.content = ""; onRegistered() }
            }
        }
    })

    val link = Span("Уже есть аккаунт? Войти").apply {
        addCssClass("text-primary")
        addCssClass("text-decoration-underline")
        onClick { onGoToLogin() }
    }
    add(link)
}
private val EMAIL_REGEX =
    Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

private val PASSWORD_REGEX =
    Regex("^(?=.*\\d)[A-Za-z\\d]{8,71}$")
