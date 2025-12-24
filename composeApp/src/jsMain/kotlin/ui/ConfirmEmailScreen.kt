package ui

import api.AuthApiClient
import io.kvision.core.Container
import io.kvision.form.text.Text
import io.kvision.html.Button
import io.kvision.html.ButtonStyle
import io.kvision.html.Span
import io.kvision.html.div
import io.kvision.html.h3
import io.kvision.html.p
import io.kvision.panel.vPanel
import io.kvision.toast.Toast
import io.kvision.utils.perc
import io.kvision.utils.px
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.interns.project.dto.LoginRequest

fun Container.confirmEmailScreen(email: String) {
    headerBar(mode = HeaderMode.PUBLIC, active = NavTab.NONE)
    val uiScope = MainScope()
    vPanel(spacing = 16) {
        width = 520.px
        addCssClass("mx-auto")

        h3("Подтверждение электронной почты")
        
        p("Ссылка для подтверждения электронной почты была отправлена на адрес: $email")
        p("Перейдите по ней, чтобы завершить регистрацию.")
        
        p("Введите код подтверждения, который был отправлен на вашу почту:")
        
        val codeField = Text(label = "Код подтверждения").apply {
            width = 100.perc
        }
        
        val error = Span("").apply {
            addCssClass("text-danger")
        }
        
        div {
            add(codeField)
            add(error)
        }
        fun localizeVerificationError(message: String?): String = when {
            message.isNullOrBlank() -> "Ошибка подтверждения"
            message.contains("invalid or expired token", ignoreCase = true) ->
                "Код подтверждения недействителен или просрочен. Войдите и запросите новый."

            message.contains("invalid token", ignoreCase = true) ->
                "Неверный код подтверждения. Проверьте правильность ввода."

            message.contains("expired token", ignoreCase = true) ->
                "Код подтверждения просрочен. Нажмите 'Отправить код повторно'."

            message.contains("verification failed", ignoreCase = true) ->
                "Не удалось подтвердить email. Попробуйте ещё раз."

            else -> "Ошибка подтверждения email"
        }
        add(Button("Подтвердить", style = ButtonStyle.PRIMARY).apply {
            width = 100.perc
            onClick {
                val code = codeField.value ?: ""
                if (code.isBlank()) {
                    Toast.danger("Введите код подтверждения")
                    return@onClick
                }

                this.disabled = true

                uiScope.launch {
                    val authClient = AuthApiClient()
                    val result = authClient.verifyEmail(code)

                    result.fold(
                        onSuccess = { response ->
                            if (response.success) {
                                val pending = Session.pendingRegistration
                                if (pending != null) {
                                    val loginResult = authClient.login(
                                        LoginRequest(
                                            email = pending.email,
                                            password = pending.password,
                                            accountType = pending.accountType
                                        )
                                    )

                                    loginResult.fold(
                                        onSuccess = { loginData ->
                                            Session.setSession(
                                                token = loginData.token,
                                                userId = loginData.userId,
                                                email = loginData.email,
                                                accountType = loginData.accountType,
                                                firstName = loginData.firstName,
                                                lastName = loginData.lastName
                                            )
                                            Session.pendingRegistration = null

                                            MainScope().launch {
                                                Session.hydrateFromBackend()
                                                val needsProfile = Session.requiresProfileCompletion()
                                                if (loginData.accountType.uppercase() == "DOCTOR") {
                                                    if (needsProfile) Navigator.showDoctorProfileEdit() else Navigator.showDoctor()
                                                } else {
                                                    if (needsProfile) Navigator.showPatientProfileEdit() else Navigator.showPatient()
                                                }
                                            }
                                        },
                                        onFailure = { authError ->
                                            Toast.danger(authError.message ?: "Ошибка входа после подтверждения")
                                            this@apply.disabled = false
                                        }
                                    )
                                } else {
                                    Navigator.showLogin()
                                }
                            } else {
                                Toast.danger(localizeVerificationError(response.message))
                                this@apply.disabled = false
                            }
                        },
                        onFailure = { e ->
                            Toast.danger(localizeVerificationError(e.message))
                            this@apply.disabled = false
                        }
                    )
                }
            }
        })
    }
}