package ui

import kotlinx.serialization.Serializable
import io.kvision.core.Container
import io.kvision.form.formPanel
import io.kvision.form.select.Select
import io.kvision.form.text.Password
import io.kvision.form.text.Text
import io.kvision.html.InputType
import io.kvision.html.button
import io.kvision.panel.vPanel
import io.kvision.utils.px
import io.kvision.utils.perc
import io.kvision.html.ButtonStyle

@Serializable
data class RegData(
    var role: String? = null,
    var lastName: String? = "",
    var firstName: String? = "",
    var middleName: String? = "",
    var login: String? = "",
    var email: String? = "",
    var password: String? = "",
    var phoneNumber: String? = ""
)

fun Container.registrationScreen(onRegistered: () -> Unit) = vPanel(spacing = 12) {
    addCssClass("mx-auto")
    width = 600.px

    val roles = listOf("Медицинский работник", "Пациент")

    val form = formPanel<RegData> {
        add(RegData::role, Select(options = roles.map { it to it }, label = "Вид аккаунта"), required = true)
        add(RegData::lastName, Text(label = "Фамилия"), required = true)
        add(RegData::firstName, Text(label = "Имя"), required = true)
        add(RegData::middleName, Text(label = "Отчество"))
        add(RegData::login, Text(label = "Логин"), required = true)
        add(RegData::email, Text(label = "Email", type = InputType.EMAIL), required = true)
        add(RegData::password, Password(label = "Пароль"), required = true)
        add(RegData::phoneNumber, Text(label = "Номер телефона", type = InputType.TEL))
    }

    button(
        text = "Зарегистрироваться",
        style = ButtonStyle.PRIMARY
    ).apply {
        width = 100.perc
        onClick {
            if (form.validate()) onRegistered()
        }
    }
}
