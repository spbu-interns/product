import io.kvision.Application
import io.kvision.core.Container
import io.kvision.panel.root
import ui.registrationScreen
import ui.stubScreen
import ui.loginScreen

class App : Application() {
    override fun start(state: Map<String, Any>) {
        root("kvapp") {
            showLogin()
        }
    }
}

private fun Container.showLogin() {
    removeAll()
    loginScreen(
        onLogin = { showStub("Вы вошли в систему") },
        onGoToRegister = { showRegister() }
    )
}

private fun Container.showRegister() {
    removeAll()
    registrationScreen(
        onRegistered = { showStub("Регистрация успешна") },
        onGoToLogin = { showLogin() }
    )
}

private fun Container.showStub(msg: String) {
    removeAll()
    stubScreen(message = msg, onBack = { showLogin() })
}
