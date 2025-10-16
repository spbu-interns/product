import io.kvision.i18n.I18n
import io.kvision.startApplication

fun main() {
    I18n.language = "en"
    println("i18n lang=" + I18n.language)
    startApplication(::App)
}
