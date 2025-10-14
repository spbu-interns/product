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
import io.kvision.utils.perc
import io.kvision.utils.px
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.interns.project.dto.VerifyEmailRequest

fun Container.confirmEmailScreen(email: String) {
    headerBar(mode = HeaderMode.PUBLIC, active = NavTab.NONE)

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
        
        add(Button("Подтвердить", style = ButtonStyle.PRIMARY).apply {
            width = 100.perc
            onClick {
                val code = codeField.value ?: ""
                if (code.isBlank()) {
                    error.content = "Введите код подтверждения"
                    return@onClick
                }
                
                this.disabled = true
                
                GlobalScope.launch {
                    val authClient = AuthApiClient()
                    val result = authClient.verifyEmail(code)
                    
                    result.fold(
                        onSuccess = { response ->
                            if (response.success) {
                                Navigator.showStub("Email успешно подтвержден! Теперь вы можете войти в систему.")
                            } else {
                                error.content = response.message ?: "Ошибка подтверждения email"
                                this@apply.disabled = false
                            }
                        },
                        onFailure = { e ->
                            error.content = e.message ?: "Ошибка подтверждения email"
                            this@apply.disabled = false
                        }
                    )
                }
            }
        })
    }
}