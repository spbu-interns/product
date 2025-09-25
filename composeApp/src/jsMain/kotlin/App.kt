import io.kvision.Application
import io.kvision.core.Container
import io.kvision.panel.root
import ui.registrationScreen
import ui.successScreen

class App : Application() {
    override fun start(state: Map<String, Any>) {
        root("kvapp") {
            showRegistration()
        }
    }
}

private fun Container.showRegistration() {
    removeAll()
    registrationScreen(onRegistered = {
        showSuccess()
    })
}

private fun Container.showSuccess() {
    removeAll()
    successScreen()
}
