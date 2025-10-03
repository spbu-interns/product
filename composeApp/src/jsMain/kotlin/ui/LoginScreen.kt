package ui

import api.AuthApiClient
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.form.text.Text
import io.kvision.html.Button
import io.kvision.html.ButtonStyle
import io.kvision.html.InputType
import io.kvision.html.Span
import io.kvision.html.p
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel
import io.kvision.utils.perc
import io.kvision.utils.px
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun Container.loginScreen(
    onLogin: () -> Unit,
    onGoToRegister: () -> Unit,
    onGoHome: () -> Unit
) = vPanel(spacing = 16) {
    width = 520.px
    addCssClass("mx-auto")

    homeIconButton{ onGoHome() }
    
    val emailField = Text(label = "Email", type = InputType.EMAIL).apply {
        width = 100.perc
    }

    var passVisible = false
    var passField = Text(label = "Пароль", type = InputType.PASSWORD).apply {
        width = 100.perc
    }
    val toggleButton = Button("👁", style = ButtonStyle.LIGHT).apply {
        onClick {
            passVisible = !passVisible
            passField.type = if (passVisible) InputType.TEXT else InputType.PASSWORD
            text = if (passVisible) "🙈" else "👁"
        }
    }

    val error = Span("").apply { addCssClass("text-danger") }

    add(emailField)
    hPanel(spacing = 8) {
        add(passField)
        add(toggleButton)
    }
    add(error)

    val loginButton = Button("Войти", style = ButtonStyle.PRIMARY).apply {
        width = 100.perc
    }

    val authClient = AuthApiClient()

    loginButton.onClick {
        val email = emailField.value ?: ""
        val password = passField.value ?: ""
        
        val emailOk = EMAIL_REGEX.matches(email)
        val passOk = PASSWORD_REGEX.matches(password)
        
        when {
            !emailOk -> error.content = "Некорректный email"
            !passOk -> error.content = "Пароль: 8-71 символ, латиница, минимум 1 цифра"
            else -> {

                error.content = "Выполняется вход..."
                loginButton.disabled = true

                CoroutineScope(Dispatchers.Main).launch {
                    val result = authClient.login(email, password)

                    result.fold(
                        onSuccess = { response ->
                            if (response.success) {
                                error.content = ""
                                onLogin()
                            } else {
                                error.content = response.error ?: "Ошибка авторизации"
                                loginButton.disabled = false
                            }
                        },
                        onFailure = { e ->
                            error.content = e.message ?: "Ошибка соединения с сервером"
                            loginButton.disabled = false
                        }
                    )
                }
            }
        }
    }
    
    add(loginButton)

    p {
        val link = Span("У вас нет аккаунта? Зарегистрируйтесь!").apply {
            addCssClass("text-primary")
            addCssClass("text-decoration-underline")
            onClick { onGoToRegister() }
        }
        add(link)
    }
}

private val EMAIL_REGEX =
    Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

private val PASSWORD_REGEX =
    Regex("^(?=.*\\d)[A-Za-z\\d]{8,71}$")
