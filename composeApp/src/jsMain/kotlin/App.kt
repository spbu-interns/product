import io.kvision.Application
import io.kvision.panel.root
import ui.Navigator
import ui.stubScreen
import ui.homeScreen
import ui.AuthTab
import ui.Session
import ui.patientScreen
import ui.authScreen
import ui.confirmEmailScreen
import ui.resetPasswordScreen

class App : Application() {
    override fun start(state: Map<String, Any>) {
        val r = root("kvapp")

        var showAuth: (AuthTab) -> Unit = {}

        fun showHome() {
            r.removeAll()
            r.homeScreen()
        }

        fun showFind() {
            r.removeAll()
            r.stubScreen(message = "В разработке") { showHome() }
        }

        fun showPatient() {
            r.removeAll()
            r.patientScreen(
                onLogout = {
                    Session.isLoggedIn = false
                    showHome()
                }
            )
        }

        fun showResetPassword() {
            r.removeAll()
            r.resetPasswordScreen()
        }

        fun showStub(message: String) {
            r.removeAll()
            r.stubScreen(message = message) { showHome() }
        }

        fun showConfirmEmail(email: String) {
            r.removeAll()
            r.confirmEmailScreen(email)
        }

        showAuth = { tab ->
            r.removeAll()
            r.authScreen(
                initial = tab,
                onLogin = {
                    Session.isLoggedIn = true
                    showPatient() },
                onRegister = { Session.isLoggedIn = true
                    showPatient() },
                onGoHome = { showHome() }
            )
        }

        Navigator.showHome = ::showHome
        Navigator.showFind = ::showFind
        Navigator.showLogin = {
            showAuth(AuthTab.LOGIN) }
        Navigator.showPatient = ::showPatient
        Navigator.showResetPassword = ::showResetPassword
        Navigator.showStub = ::showStub
        Navigator.showRegister = {
            showAuth(AuthTab.REGISTER)
        }
        Navigator.showConfirmEmail = ::showConfirmEmail

        if (Session.isLoggedIn) showPatient() else showHome()
    }
}
