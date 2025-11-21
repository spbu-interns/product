package ui

import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.html.Button
import io.kvision.html.ButtonStyle
import io.kvision.html.div
import io.kvision.html.h3
import io.kvision.panel.vPanel
import io.kvision.utils.perc
import io.kvision.utils.px

fun Container.passwordResetSuccessScreen() = vPanel(spacing = 16) {
    headerBar(mode = HeaderMode.PUBLIC, active = NavTab.NONE)

    vPanel(spacing = 16) {
        width = 520.px
        addCssClass("mx-auto")
        addCssClass("text-center")

        h3("Пароль изменён")
        div("Ваш пароль был успешно изменён, теперь вы можете войти в аккаунт по нему")

        add(Button("Перейти ко входу", style = ButtonStyle.PRIMARY).apply {
            width = 100.perc
            onClick { Navigator.showLogin() }
        })
    }
}