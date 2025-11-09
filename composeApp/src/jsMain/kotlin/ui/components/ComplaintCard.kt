package ui.components

import io.kvision.core.*
import io.kvision.html.div
import io.kvision.html.span
import io.kvision.utils.perc
import io.kvision.utils.px
import org.interns.project.dto.ComplaintResponse

fun Container.complaintCard(
    complaint: ComplaintResponse,
    onOpen: (ComplaintResponse) -> Unit
) {
    div(className = "record item card").apply {
        onClick { onOpen(complaint) }
        div(className = "record-row") {
            display = Display.FLEX
            flexDirection = FlexDirection.ROW
            alignItems = AlignItems.CENTER
            justifyContent = JustifyContent.SPACEBETWEEN
            width = 100.perc
            padding = 12.px
            textAlign = TextAlign.LEFT

            div(className = "record-left") {
                display = Display.FLEX
                flexDirection = FlexDirection.COLUMN
                justifyContent = JustifyContent.CENTER
                textAlign = TextAlign.LEFT

                span(complaint.title, className = "record title") {
                    fontWeight = FontWeight.BOLD
                    fontSize = 16.px
                }
                span(complaint.createdAt, className = "record date") {
                    opacity = 0.7
                    fontSize = 12.px
                }
            }

            div(className = "record-right") {
                display = Display.FLEX
                alignItems = AlignItems.CENTER
                justifyContent = JustifyContent.FLEXEND
                minWidth = 20.perc
                textAlign = TextAlign.RIGHT
                span("Открыть →", className = "link subtle")
            }
        }
    }
}
