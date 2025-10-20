package ui

import io.kvision.core.*
import io.kvision.form.select.select
import io.kvision.form.text.text
import io.kvision.html.Button
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.h3
import io.kvision.html.li
import io.kvision.html.nav
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.html.ul
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel
import io.kvision.toast.Toast
import io.kvision.utils.perc
import io.kvision.utils.px
import kotlinx.browser.document
import kotlinx.browser.window

fun Container.recordEditorScreen(recordId: String, onBack: () -> Unit) = vPanel(spacing = 12) {
    headerBar(mode = HeaderMode.PATIENT, active = NavTab.NONE)

    val rec = RecordsStore.byId(recordId)
    if (rec == null) {
        div(className = "card block") { +"Record not found" }
        return@vPanel
    }

    div(className = "account container") {
        div(className = "account grid") {

            div(className = "sidebar card") {
                div(className = "avatar circle") { +"NS" }
                h3("Name Surname", className = "account name")
                p("Patient ID: 12345", className = "account id")
                nav {
                    ul(className = "side menu") {
                        li(className = "side_item") {
                            span("Overview"); span("\uD83D\uDC64", className = "side icon"); onClick { Navigator.showPatient() }
                        }
                        li(className = "side_item") {
                            span("Appointments"); span("\uD83D\uDCC5", className = "side icon")
                        }
                        li(className = "side_item") {
                            span("Medical Records"); span("\uD83D\uDCC4", className = "side icon")
                        }
                        li(className = "side_item is-active") {
                            span("My Records"); span("\uD83D\uDCDD", className = "side icon")
                            onClick { Navigator.showMyRecords() }
                        }
                    }
                }
            }

            div(className = "main column") {
                h1("Edit Record", className = "account title")

                val topicInput = text(label = "Topic", value = rec.topic)
                val specialtySelect = select(
                    options = listOf(
                        "Cardiology","Neurology","Pediatrics","Orthopedics","Ophthalmology","Other"
                    ).map { it to it },
                    label = "Specialty"
                ).apply { value = rec.specialty }

                val editor = div(className = "custom-editor", rich = true).apply {
                    width = 100.perc
                    height = 420.px
                    border = Border(1.px, BorderStyle.SOLID, Color.name(Col.LIGHTGRAY))
                    borderRadius = 8.px
                    padding = 12.px
                    background = Background(Color.hex(0xFFFFFF))
                    overflowY = Overflow.AUTO
                    setAttribute("contenteditable", "true")
                    content = rec.content ?: ""
                }

                var btnBold: Button? = null
                var btnItalic: Button? = null
                var btnUnderline: Button? = null

                fun Button?.setActive(active: Boolean) {
                    this?.let {
                        if (active) {
                            it.addCssClass("btn-toolbar-active")
                        } else {
                            it.removeCssClass("btn-toolbar-active")
                        }
                    }
                }

                fun syncToolbar() {
                    val b = document.queryCommandState("bold")
                    val i = document.queryCommandState("italic")
                    val u = document.queryCommandState("underline")
                    btnBold.setActive(b == true)
                    btnItalic.setActive(i == true)
                    btnUnderline.setActive(u == true)
                }

                fun applyCmd(cmd: String) {
                    editor.getElement()?.unsafeCast<org.w3c.dom.HTMLElement>()?.focus()
                    document.execCommand(cmd, false, "")
                    window.setTimeout({ syncToolbar() }, 0)
                }

                div(className = "card block") {
                    add(topicInput)
                    add(specialtySelect)

                    hPanel(className = "toolbar", spacing = 8) {
                        justifyContent = JustifyContent.SPACEBETWEEN
                        alignItems = AlignItems.CENTER

                        span("Content", className = "kv-form-label").apply {
                            fontWeight = FontWeight.NORMAL
                        }
                        hPanel(spacing = 8) {
                            btnBold = button("B", className = "btn-toolbar").apply {
                                fontWeight = FontWeight.BOLD
                                onClick { applyCmd("bold") }
                            }
                            btnItalic = button("I", className = "btn-toolbar").apply {
                                fontStyle = FontStyle.ITALIC
                                onClick { applyCmd("italic") }
                            }
                            btnUnderline = button("U", className = "btn-toolbar text-underline").apply {
                                onClick { applyCmd("underline") }
                            }
                        }
                    }

                    add(editor)
                }

                editor.onEvent {
                    keyup = { _ -> syncToolbar() }
                    mouseup = { _ -> syncToolbar() }
                    input = { syncToolbar() }
                }
                document.addEventListener("selectionchange", { syncToolbar() })
                syncToolbar()

                hPanel(spacing = 8) {
                    button("Save", className = "btn-primary").onClick {
                        val html = editor.content ?: ""
                        RecordsStore.update(
                            id = rec.id,
                            topic = topicInput.value?.trim().orEmpty(),
                            specialty = specialtySelect.value ?: "Other",
                            content = html
                        )
                        Toast.success("Saved")
                        Navigator.showMyRecords()
                    }
                    button("Delete", className = "btn-danger").onClick {
                        RecordsStore.delete(rec.id)
                        Toast.danger("Deleted")
                        Navigator.showMyRecords()
                    }
                    button("Back", className = "btn").onClick { onBack() }
                }
            }
        }
    }
}
