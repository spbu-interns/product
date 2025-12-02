package ui

import api.AuthApiClient
import api.UnverifiedEmailException
import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.form.select.Select
import io.kvision.form.text.Text
import io.kvision.html.*
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel
import io.kvision.utils.perc
import io.kvision.utils.px
import io.kvision.toast.Toast
import io.kvision.toast.ToastOptions
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.interns.project.dto.LoginRequest
import org.interns.project.dto.LoginResponse
import org.interns.project.dto.RegisterRequest
import ui.components.PASSWORD_REGEX

enum class AuthTab { LOGIN, REGISTER }

fun Container.authScreen(
    initial: AuthTab = AuthTab.LOGIN,
    onLogin: (LoginResponse) -> Unit,
    onRegister: (email: String, password: String, accountType: String) -> Unit,
    onGoHome: () -> Unit
) = vPanel(spacing = 16) {
    val uiScope = MainScope()

    headerBar(mode = HeaderMode.PUBLIC, active = NavTab.NONE)

    vPanel(spacing = 16) {
        width = 520.px
        addCssClass("mx-auto")

        var current = initial

        var renderForm: () -> Unit = {}
        var updateTabs: () -> Unit = {}

        val loginTab = Span("Вход", className = "auth-tab")
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
                    val authClient = AuthApiClient()

                    val emailField = Text(label = "Email", type = InputType.EMAIL).apply {
                        width = 100.perc
                    }.also { content.add(it) }

                    val passField = content.addPasswordRow("Пароль")

                    content.add(div(className = "aux-row") {
                        add(Link("Забыли пароль?", "#", className = "forgot-link").apply {
                            onClick {
                                it.preventDefault()
                                Navigator.showResetPassword()
                            }
                        })
                    })

                    val error = Span("").apply { addCssClass("text-danger") }

                    content.add(div { add(error) })

                    content.add(Button("Войти", style = ButtonStyle.PRIMARY).apply {
                        width = 100.perc
                        onClick {
                            val email = (emailField.value ?: "").trim()
                            val password = passField.value ?: ""

                            val emailOk = EMAIL_REGEX.matches(email)
                            val passOk = PASSWORD_REGEX.matches(password)

                            when {
                                !emailOk -> Toast.danger("Некорректный email")
                                !passOk -> Toast.danger("Проверьте пароль")
                                else -> {
                                    error.content = ""
                                    this.disabled = true

                                    uiScope.launch {
                                        val result = authClient.login(
                                            LoginRequest(
                                                email = email,
                                                password = password,
                                                accountType = ""
                                            )
                                        )

                                        result.fold(
                                            onSuccess = { data ->
                                                Session.setSession(
                                                    token = data.token,
                                                    userId = data.userId,
                                                    email = data.email,
                                                    accountType = data.accountType,
                                                    firstName = data.firstName,
                                                    lastName = data.lastName
                                                )
                                                uiScope.cancel()
                                                onLogin(data)
                                            },
                                            onFailure = { e ->
                                                if (e is UnverifiedEmailException) {
                                                    val emailForResend = e.email.ifBlank { email }
                                                    val accTypeForResend = e.accountType ?: ""
                                                    val passwordForResend = password

                                                    val msg = "Аккаунт не подтверждён. " +
                                                            "Нажмите, чтобы отправить письмо с кодом ещё раз."

                                                    Toast.info(
                                                        msg,
                                                        options = ToastOptions(
                                                            close = true,
                                                            duration = 5000,
                                                            stopOnFocus = true,
                                                            onClick = {
                                                                // клик по тосту = отправить код ещё раз
                                                                uiScope.launch {
                                                                    val resendResult =
                                                                        authClient.startEmailVerification(emailForResend)

                                                                    resendResult.fold(
                                                                        onSuccess = {
                                                                            Toast.success("Письмо с подтверждением отправлено повторно")

                                                                            Session.pendingRegistration =
                                                                                Session.PendingRegistration(
                                                                                    email = emailForResend,
                                                                                    password = passwordForResend,
                                                                                    accountType = accTypeForResend
                                                                                )

                                                                            uiScope.cancel()
                                                                            onRegister(
                                                                                emailForResend,
                                                                                passwordForResend,
                                                                                accTypeForResend
                                                                            )
                                                                        },
                                                                        onFailure = { err ->
                                                                            Toast.danger(
                                                                                err.message
                                                                                    ?: "Не удалось отправить письмо повторно"
                                                                            )
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                        )
                                                    )
                                                } else {
                                                    error.content = localizeLoginError(e.message)
                                                    Toast.danger(localizeLoginError(e.message))
                                                }
                                                this@apply.disabled = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    })
                }

                AuthTab.REGISTER -> {
                    val accountType = content.addAccountTypeSelect()

                    val emailField = Text(label = "Email", type = InputType.EMAIL).apply {
                        width = 100.perc
                    }.also { content.add(it) }

                    val passField  = content.addPasswordRow("Пароль")
                    val pass2Field = content.addPasswordRow("Подтверждение пароля")

                    val error = Span("").apply { addCssClass("text-danger") }.also { content.add(it) }

                    val registerButton = Button("Зарегистрироваться", style = ButtonStyle.PRIMARY).apply {
                        width = 100.perc
                        onClick {
                            val email = (emailField.value ?: "").trim()
                            val password = passField.value ?: ""
                            val password2 = pass2Field.value ?: ""
                            val accType = accountType.value ?: "Пациент"

                            val emailOk = EMAIL_REGEX.matches(email)
                            val passOk = PASSWORD_REGEX.matches(password)
                            val same = password == password2

                            when {
                                !emailOk -> Toast.danger("Некорректный email")
                                !same   -> Toast.danger("Пароли не совпадают")
                                !passOk -> Toast.danger("Пароль должен содержать от 8 до 71 символов, латинские буквы и 1 цифру")
                                else -> {
                                    error.content = ""
                                    this.disabled = true

                                    uiScope.launch {
                                        val authClient = AuthApiClient()
                                        val result = authClient.register(
                                            RegisterRequest(
                                                email = email,
                                                password = password,
                                                accountType = accType
                                            )
                                        )

                                        result.fold(
                                            onSuccess = { response ->
                                                if (response.success) {
                                                    Session.pendingRegistration = Session.PendingRegistration(email, password, accType)
                                                    uiScope.cancel()
                                                    onRegister(email, password, accType)
                                                } else {
                                                    Toast.danger(localizeRegistrationError(response.message))
                                                    this@apply.disabled = false
                                                }
                                            },
                                            onFailure = { e ->
                                                Toast.danger(localizeRegistrationError(e.message))
                                                this@apply.disabled = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    content.add(registerButton)
                }
            }
        }

        updateTabs()
        renderForm()
    }
}

private val EMAIL_REGEX =
    Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$")

private fun localizeLoginError(message: String?): String = when {
    message.isNullOrBlank() -> "Ошибка входа"
    message.contains("invalid email or password", ignoreCase = true) -> "Неверный email или пароль"
    message.contains("unauthorized", ignoreCase = true) -> "Неверный email или пароль"
    else -> message
}

private fun localizeRegistrationError(message: String?): String = when {
    message.isNullOrBlank() -> "Ошибка регистрации"
    message.contains("существует", ignoreCase = true) -> "Пользователь с таким email уже существует"
    message.contains("already exists", ignoreCase = true) -> "Пользователь с таким email уже существует"
    message.contains("invalid registration data", ignoreCase = true) -> "Некорректные данные для регистрации"
    message.contains("unprocessable", ignoreCase = true) -> "Некорректные данные для регистрации"
    else -> message
}

private fun accountTypeSelect(): Select = Select(
    options = listOf(
        "Пациент" to "Пациент",
        "Медицинский работник" to "Медицинский работник"
    ),
    label = "Тип аккаунта"
).apply { width = 100.perc }

private fun Container.addAccountTypeSelect(): Select =
    accountTypeSelect().also { add(it) }

private fun Container.addPasswordRow(label: String): Text {
    var visible = false
    val field = Text(label = label, type = InputType.PASSWORD).apply { width = 100.perc }
    hPanel(spacing = 8, className = "input-with-eye") {
        add(field)
        add(Button("🙈", style = ButtonStyle.LIGHT).apply {
            addCssClass("eye-toggle")
            onClick {
                visible = !visible
                field.type = if (visible) InputType.TEXT else InputType.PASSWORD
                text = if (visible) "\uD83D\uDE49" else "🙈"
            }
        })
    }
    return field
}