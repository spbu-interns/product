package ui

import io.kvision.core.Container
import io.kvision.html.p
import io.kvision.panel.vPanel
import io.kvision.utils.px

fun Container.confirmEmailScreen(email: String) {
    headerBar(mode = HeaderMode.PUBLIC, active = NavTab.NONE)

    vPanel(spacing = 16) {
        width = 520.px
        addCssClass("mx-auto")

        p("Ссылка для подтверждения электронной почты была отправлена на адрес: $email")
        p("Перейдите по ней, чтобы завершить регистрацию.")
    }
}