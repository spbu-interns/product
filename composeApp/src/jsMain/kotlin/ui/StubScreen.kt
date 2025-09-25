package ui

import io.kvision.core.Container
import io.kvision.html.Button
import io.kvision.html.ButtonStyle
import io.kvision.html.div
import io.kvision.panel.vPanel
import io.kvision.utils.px


fun Container.stubScreen(message: String, onBack: () -> Unit) = vPanel(spacing = 12) {
    width = 520.px
    addCssClass("mx-auto")
    div("✅ $message")
    add(Button("Назад", style = ButtonStyle.SECONDARY).apply { onClick { onBack() } })
}