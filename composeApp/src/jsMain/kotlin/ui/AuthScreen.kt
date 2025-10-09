package ui

import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.core.onEvent
import io.kvision.form.select.Select
import io.kvision.form.text.Text
import io.kvision.html.Button
import io.kvision.html.ButtonStyle
import io.kvision.html.InputType
import io.kvision.html.Link
import io.kvision.html.Span
import io.kvision.html.div
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel
import io.kvision.utils.perc
import io.kvision.utils.px

enum class AuthTab { LOGIN, REGISTER }

fun Container.authScreen(
    initial: AuthTab = AuthTab.LOGIN,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onGoHome: () -> Unit
) = vPanel(spacing = 16) {
    width = 520.px
    addCssClass("mx-auto")

    homeIconButton { onGoHome() }

    var current = initial

    var renderForm: () -> Unit = {}
    var updateTabs: () -> Unit = {}

    val loginTab  = Span("Вход", className = "auth-tab")
    val registerTab = Span("Регистрация", className = "auth-tab")

    loginTab.onClick {
        current = AuthTab.LOGIN
        renderForm()
        updateTabs()
    }
    registerTab.onClick {
        current = AuthTab.REGISTER
        renderForm()
        updateTabs()
    }

    div(className = "auth-switch") {
        add(loginTab)
        add(registerTab)
    }

    val content = div(className = "auth-content") {}

    updateTabs = {
        if (current == AuthTab.LOGIN) {
            loginTab.addCssClass("is-active")
            registerTab.removeCssClass("is-active")
        } else {
            registerTab.addCssClass("is-active")
            loginTab.removeCssClass("is-active")
        }
    }

    renderForm = {
        content.removeAll()

        when (current) {
            AuthTab.LOGIN -> {
                val accountType = Select(
                    options = listOf(
                        "Пациент" to "Пациент",
                        "Медицинский работник" to "Медицинский работник"
                    ),
                    label = "Тип аккаунта"
                ).apply { width = 100.perc}

                val emailField = Text(label = "Email", type = InputType.EMAIL).apply {
                    width = 100.perc
                }
                var passVisible = false
                val passField = Text(label = "Пароль", type = InputType.PASSWORD).apply {
                    width = 100.perc
                }
                val passRow = hPanel(spacing = 8, className = "input-with-eye") {
                    add(passField)
                    add(Button("🙈", style = ButtonStyle.LIGHT).apply {
                        addCssClass("eye-toggle")
                        onClick {
                            passVisible = !passVisible
                            passField.type = if (passVisible) InputType.TEXT else InputType.PASSWORD
                            text = if (passVisible) "\uD83D\uDE49" else "🙈"
                        }
                    })
                }
                val error = Span("").apply { addCssClass("text-danger") }

                content.add(accountType)
                content.add(emailField)
                content.add(passRow)

                content.add(div(className = "aux-row") {
                    add(Link("Забыли пароль?", "#", className = "forgot-link"))
                })

                content.add(error)

                content.add(Button("Войти", style = ButtonStyle.PRIMARY).apply {
                    width = 100.perc
                    onClick {
                        val emailOk = EMAIL_REGEX.matches(emailField.value ?: "")
                        val passOk = PASSWORD_REGEX.matches(passField.value ?: "")
                        when {
                            !emailOk -> error.content = "Некорректный email"
                            !passOk -> error.content = "Пароль должен содержать от 8 до 71 символов, латинские буквы и 1 цифру"
                            else -> { error.content = ""; onLogin() }
                        }
                    }
                })
            }

            AuthTab.REGISTER -> {
                val accountType = Select(
                    options = listOf(
                        "Пациент" to "Пациент",
                        "Медицинский работник" to "Медицинский работник"
                    ),
                    label = "Тип аккаунта"
                ).apply { width = 100.perc}

                val emailField = Text(label = "Email", type = InputType.EMAIL).apply {
                    width = 100.perc
                }
                var passVisible = false
                var pass2Visible = false
                val passField = Text(label = "Пароль", type = InputType.PASSWORD).apply {
                    width = 100.perc
                }
                val pass2Field = Text(label = "Подтверждение пароля", type = InputType.PASSWORD).apply {
                    width = 100.perc
                }
                val passRow = hPanel(spacing = 8, className = "input-with-eye") {
                    add(passField)
                    add(Button("🙈", style = ButtonStyle.LIGHT).apply {
                        addCssClass("eye-toggle")
                        onClick {
                            passVisible = !passVisible
                            passField.type = if (passVisible) InputType.TEXT else InputType.PASSWORD
                            text = if (passVisible) "\uD83D\uDE49" else "🙈"
                        }
                    })
                }
                val pass2Row = hPanel(spacing = 8, className = "input-with-eye") {
                    add(pass2Field)
                    add(Button("🙈", style = ButtonStyle.LIGHT).apply {
                        addCssClass("eye-toggle")
                        onClick {
                            pass2Visible = !pass2Visible
                            pass2Field.type = if (pass2Visible) InputType.TEXT else InputType.PASSWORD
                            text = if (pass2Visible) "\uD83D\uDE49" else "🙈"
                        }
                    })
                }

                val codeField = Text(label = "Код для подтверждения", type = InputType.TEXT).apply {
                    width = 100.perc
                    visible = false
                }
                val codeRow = hPanel(spacing = 8, className = "input-with-eye") {
                    add(codeField)
                }

                val error = Span("").apply { addCssClass("text-danger") }

                content.add(accountType)
                content.add(emailField)
                content.add(codeRow)
                content.add(passRow)
                content.add(pass2Row)
                content.add(error)

                var codeRequsted = false
                val registerButton = Button("Зарегистрироваться", style = ButtonStyle.PRIMARY).apply {
                    width = 100.perc
                    onClick {
                        if (!codeRequsted) {
                            codeRequsted = true
                            codeField.visible = true
                            error.content = ""
                            return@onClick
                        }

                        val emailOk = EMAIL_REGEX.matches(emailField.value ?: "")
                        val passOk = PASSWORD_REGEX.matches(passField.value ?: "")
                        val same = (passField.value ?: "") == (pass2Field.value ?: "")
                        when {
                            !emailOk -> error.content = "Некорректный email"
                            !passOk -> error.content = "Пароль: 8–71 символ, латиница, минимум 1 цифра"
                            !same   -> error.content = "Пароли не совпадают"
                            else -> { error.content = ""; onRegister() }
                        }
                    }
                }

                codeField.onEvent {
                    input = {
                        val codeLen = (codeField.value ?: "").trim().length
                        registerButton.style = if (codeLen == 6) ButtonStyle.SUCCESS else ButtonStyle.PRIMARY
                    }
                }

                content.add(registerButton)
            }
        }
    }

    updateTabs()
    renderForm()
}

private val EMAIL_REGEX =
    Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}\$")

private val PASSWORD_REGEX =
    Regex("^(?=.*\\d)[A-Za-z\\d!@#\$%^&*()_+\\-={}\\[\\]|:;\"'<>,.?/`~]{8,71}\$")