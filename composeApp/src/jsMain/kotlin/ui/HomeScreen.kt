package ui

import io.kvision.core.Container
import io.kvision.html.Button
import io.kvision.html.ButtonStyle
import io.kvision.html.h1
import io.kvision.html.p
import io.kvision.panel.vPanel
import io.kvision.utils.px

fun Container.homeScreen(
    onGoToLogin : () -> Unit,
    onGoToRegister : () -> Unit
) = vPanel(spacing = 24) {
    width = 720.px
    addCssClass("mx-auto")
    addCssClass("text-center")

    h1("Добро пожаловать!") {
        addCssClass("fw-bold")
    }

    p("")

    add(Button("Войти", style = ButtonStyle.PRIMARY).apply {
        onClick { onGoToLogin() }
    })
    add(Button("Зарегистрироваться", style = ButtonStyle.PRIMARY).apply {
        onClick { onGoToRegister() }
    })
}