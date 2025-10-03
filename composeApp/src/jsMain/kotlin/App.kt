import api.ApiConfig
import api.ApiTest
import api.AuthApiClient
import io.kvision.Application
import io.kvision.core.Container
import io.kvision.panel.root
import ui.registrationScreen
import ui.stubScreen
import ui.loginScreen
import ui.homeScreen

class App : Application() {
    private val authClient = AuthApiClient()
    
    override fun start(state: Map<String, Any>) {
        ApiTest.runDiagnostics()
        
        root("kvapp") {
            if (authClient.isLoggedIn()) {
                val role = authClient.getUserRole() ?: "unknown"
                showStub("Вы авторизованы с ролью: $role")
            } else {
                showHome()
            }
        }
    }
}

private fun Container.showLogin() {
    removeAll()
    loginScreen(
        onLogin = {
            val role = ApiConfig.getRole() ?: "unknown"
            showStub("Вы вошли в систему с ролью: $role")
        },
        onGoToRegister = { showRegister() },
        onGoHome = { showHome() }
    )
}

private fun Container.showRegister() {
    removeAll()
    registrationScreen(
        onRegistered = { showStub("Регистрация успешна. Теперь вы можете войти в систему.") },
        onGoToLogin = { showLogin() },
        onGoHome = { showHome() }
    )
}

private fun Container.showStub(msg: String) {
    removeAll()
    stubScreen(
        message = msg,
        onBack = {
            val authClient = AuthApiClient()
            authClient.logout()
            showLogin()
        }
    )
}

private fun Container.showHome() {
    removeAll()
    homeScreen(
        onGoToLogin = { showLogin() },
        onGoToRegister = { showRegister() }
    )
}
