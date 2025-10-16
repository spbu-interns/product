package ui

import io.kvision.core.AlignItems
import io.kvision.core.Container
import io.kvision.core.Display
import io.kvision.core.FlexDirection
import io.kvision.core.FontWeight
import io.kvision.core.JustifyContent
import io.kvision.core.Overflow
import io.kvision.core.TextAlign
import io.kvision.core.onClick
import io.kvision.form.select.select
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h3
import io.kvision.html.h4
import io.kvision.html.li
import io.kvision.html.nav
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.html.ul
import io.kvision.panel.vPanel
import io.kvision.form.text.text
import io.kvision.toast.Toast
import io.kvision.utils.perc
import io.kvision.utils.px

fun Container.myRecordsScreen(
    onLogout: () -> Unit = { Navigator.showHome() }
) = vPanel(spacing = 12) {
    headerBar(
        mode = HeaderMode.PATIENT,
        active = NavTab.NONE,
        onLogout = {
            Session.isLoggedIn = false
            onLogout()
        }
    )

    div(className = "account container") {
        div(className = "account grid") {
            div(className = "sidebar card") {
                div(className = "avatar circle") { + "NS" }
                h3("Name Surname", className = "account name")
                p("Patient ID: 12345", className = "account id")

                nav {
                    ul(className = "side menu") {
                        li(className = "side_item") { span("Overview")
                            span("\uD83D\uDC64", className = "side icon")
                            onClick { Navigator.showPatient() }
                        }
                        li(className = "side_item") { span("Appointments"); span("\uD83D\uDCC5", className = "side icon") }
                        li(className = "side_item") { span("Medical Records"); span("\uD83D\uDCC4", className = "side icon") }
                        li(className = "side_item is-active") {
                            span("My Records")
                            span("\uD83D\uDCDD", className = "side icon")
                            onClick { Navigator.showMyRecords() }
                        }
                    }
                }

                div(className = "side button")
                button("Find New Doctor", className = "btn-primary-lg").onClick {
                    Navigator.showFind()
                }
            }

            div(className = "main column") {
                val specialties = listOf(
                    "Cardiology", "Neurology", "Pediatrics",
                    "Orthopedics", "Ophthalmology", "Other"
                )

                val topicInput = text(label = "Topic").apply {
                    placeholder = "Enter record topic"
                    addCssClass("kv-input")
                }

                val specialtySelect = select(
                    options = specialties.map { it to it },
                    label = "Specialty"
                ).apply {
                    value = "Other"
                    marginBottom = 12.px
                }

                val recordsContainer = vPanel(spacing = 8, className = "records list").apply {
                    width = 100.perc
                    flexDirection = FlexDirection.COLUMNREV
                }

                val recordsScrollBox = div(className = "records scrollbox") {
                    maxHeight = 320.px
                    overflowY = Overflow.AUTO
                    overflowX = Overflow.HIDDEN
                    width = 100.perc
                    paddingRight = 6.px
                    add(recordsContainer)
                }

                div(className = "card block") {
                    h4("Add a Record", className = "block title")
                    div(className = "form row") {
                        div(className = "form field") { add(topicInput) }
                        div(className = "form field") { add(specialtySelect) }
                        div(className = "form actions") {
                            button("Add", className = "btn-primary").onClick {
                                val topic = topicInput.value?.trim().orEmpty()
                                val specialty = specialtySelect.value ?: "Other"
                                if (topic.isBlank()) {
                                    Toast.warning("Please enter the topic")
                                    return@onClick
                                }
                                val rec = RecordsStore.add(topic, specialty)
                                renderRecordCard(recordsContainer, rec)
                                topicInput.value = ""
                                specialtySelect.value = "Other"
                            }
                        }
                    }
                }

                h4("My Records", className = "block title")
                div(className = "card block") {
                    add(recordsScrollBox)
                }
            }
        }
    }
}

private fun renderRecordCard(container: Container, rec: MyRecord) {
    container.div(className = "record item card") {
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

                span(rec.topic, className = "record title").apply {
                    fontWeight = FontWeight.BOLD
                    fontSize = 16.px
                }
                span(rec.createdAt, className = "record date").apply {
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

                span(rec.specialty, className = "status info")
            }.onClick {
                Navigator.showRecordEditor(rec.id)
            }
        }
    }
}