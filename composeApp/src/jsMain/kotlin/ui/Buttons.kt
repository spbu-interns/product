package ui

import io.kvision.core.Container
import io.kvision.core.Position

fun Container.homeIconButton(onClick: () -> Unit) = div {
    addCssClass("home-button")

    image(src = "image")
}