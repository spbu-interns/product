package ui

import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.html.div
import io.kvision.html.image

fun Container.homeIconButton(onHomeClick: () -> Unit) = div {
    addCssClass("home-button")

    this.onClick { onHomeClick() }
}