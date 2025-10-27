import io.kvision.Application
import io.kvision.panel.root
import i18n.t
import i18n.Localization
import ui.Navigator
import ui.stubScreen
import ui.homeScreen
import ui.AuthTab
import ui.Session
import ui.patientScreen
import ui.authScreen
import ui.confirmEmailScreen
import ui.doctorScreen
import ui.myRecordsScreen
import ui.recordEditorScreen
import ui.resetPasswordScreen

class App : Application() {
    override fun start(state: Map<String, Any>) {
        val r = root("kvapp")

        var currentRenderer: () -> Unit = {}

        fun showHome() {
            currentRenderer = ::showHome
            r.removeAll()
            r.homeScreen()
        }

        fun showStub(messageProvider: () -> String) {
            currentRenderer = { showStub(messageProvider) }
            r.removeAll()
            r.stubScreen(message = messageProvider()) { showHome() }
        }

        fun showFind() {
            showStub { t("stub.inDevelopment") }
        }

        fun showPatient() {
            currentRenderer = ::showPatient
            r.removeAll()
            r.patientScreen(
                onLogout = {
                    Session.isLoggedIn = false
                    showHome()
                }
            )
        }

        fun showDoctor() {
            currentRenderer = ::showDoctor
            r.removeAll()
            r.doctorScreen(
                onLogout = {
                    Session.isLoggedIn = true
                    showHome()
                }
            )
        }

        fun showMyRecords() {
            currentRenderer = ::showMyRecords
            r.removeAll()
            r.myRecordsScreen(
                onLogout = {
                    Session.isLoggedIn = false
                    showHome()
                }
            )
        }

        fun showRecordEditor(id: String) {
            currentRenderer = { showRecordEditor(id) }
            r.removeAll()
            r.recordEditorScreen(recordId = id) { showMyRecords() }
        }

        fun showResetPassword() {
            currentRenderer = ::showResetPassword
            r.removeAll()
            r.resetPasswordScreen()
        }

        fun showConfirmEmail(email: String) {
            currentRenderer = { showConfirmEmail(email) }
            r.removeAll()
            r.confirmEmailScreen(email)
        }

        fun showAuth(tab: AuthTab) {
            currentRenderer = { showAuth(tab) }
            r.removeAll()
            r.authScreen(
                initial = tab,
                onLogin = {
                    Session.isLoggedIn = true
                    showPatient() },
                onRegister = {
                    Session.isLoggedIn = true
                    showPatient() },
                onGoHome = { showHome() }
            )
        }

        Navigator.showHome = ::showHome
        Navigator.showFind = ::showFind
        Navigator.showLogin = { showAuth(AuthTab.LOGIN)}
        Navigator.showPatient = ::showPatient
        Navigator.showResetPassword = ::showResetPassword
        Navigator.showStub = { message: String ->
            showStub { message }
        }
        Navigator.showRegister = { showAuth(AuthTab.REGISTER) }
        Navigator.showConfirmEmail = ::showConfirmEmail
        Navigator.showMyRecords = ::showMyRecords
        Navigator.showRecordEditor = ::showRecordEditor
        Navigator.showDoctor = ::showDoctor

        Localization.addLanguageChangeListener { currentRenderer() }

        if (Session.isLoggedIn) showPatient() else showHome()
    }
}
