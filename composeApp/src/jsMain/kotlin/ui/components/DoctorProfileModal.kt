package ui.components

import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h3
import io.kvision.html.p
import io.kvision.panel.simplePanel
import ui.DoctorProfile
import ui.components.updateAvatar
import kotlin.math.roundToInt
import kotlin.toString

class DoctorProfileModalController internal constructor(
    private val renderModal: () -> Unit,
    private val openAction: (DoctorProfile) -> Unit,
    private val closeAction: () -> Unit
) {
    fun open(profile: DoctorProfile) = openAction(profile)
    fun close() = closeAction()
    internal fun render() = renderModal()
}

private fun Double?.format1(): String {
    return this?.takeIf { it.isFinite() }?.let { (it * 10).roundToInt() / 10.0 }?.toString() ?: "—"
}

fun Container.doctorProfileModal(
    onBook: (DoctorProfile) -> Unit
): DoctorProfileModalController {
    var visible = false
    var currentProfile: DoctorProfile? = null

    lateinit var renderProfileModal: () -> Unit

    val overlay = simplePanel(className = "doctor-profile-overlay-root") {
        visible = false
    }

    fun closeModal() {
        visible = false
        renderProfileModal()
    }

    fun openModal(profile: DoctorProfile) {
        currentProfile = profile
        visible = true
        renderProfileModal()
    }

    renderProfileModal = render@{
        overlay.removeAll()
        overlay.visible = visible

        if (!visible || currentProfile == null) return@render

        val profile = currentProfile!!
        val initials = profile.name
            .split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .take(2)

        overlay.div(className = "doctor-profile-overlay") {
            div(className = "doctor-profile-backdrop").onClick { closeModal() }

            div(className = "doctor-profile-modal") {
                div(className = "doctor-profile-header") {
                    div(className = "doctor-profile-heading") {
                        div(className = "doctor-profile-avatar")
                            .apply { updateAvatar(profile.avatarUrl, initials.ifBlank { "Фото" }) }
                        div(className = "doctor-profile-title") {
                            h3(profile.name, className = "doctor-profile-name")
                            p(profile.specialty, className = "doctor-profile-specialty")
                        }
                    }
                    button("×", className = "doctor-profile-close").onClick { closeModal() }
                }

                div(className = "doctor-profile-body") {
                    div(className = "doctor-profile-meta") {
                        metaItem("Рейтинг", "★ ${profile.rating.format1()}")
                        metaItem("Стаж", "${profile.experienceYears} лет")
                        metaItem("Локация", profile.location)
                        metaItem(
                            label = "Стоимость",
                            value = "${profile.price} ₽ / приём",
                            note = "Может вырасти при добавлении услуг"
                        )
                    }

                    p(profile.bio, className = "doctor-profile-bio")
                }

                div(className = "doctor-profile-actions") {
                    button("Записаться", className = "btn btn-primary") {
                        onClick {
                            closeModal()
                            onBook(profile)
                        }
                    }
                    button("Закрыть", className = "btn btn-secondary") {
                        onClick { closeModal() }
                    }
                }
            }
        }
    }

    return DoctorProfileModalController(
        renderModal = renderProfileModal,
        openAction = ::openModal,
        closeAction = ::closeModal
    )
}

private fun Container.metaItem(label: String, value: String, note: String? = null) {
    div(className = "doctor-profile-meta-item") {
        p(label, className = "doctor-profile-meta-label")
        p(value, className = "doctor-profile-meta-value")
        note?.let {
            p(it, className = "doctor-profile-meta-note")
        }
    }
}
