import api.ApiConfig
import io.kvision.Application
import io.kvision.i18n.I18n
import io.kvision.panel.root
import ui.Navigator
import ui.stubScreen
import ui.homeScreen
import ui.AuthTab
import ui.Session
import ui.patientScreen
import ui.authScreen
import ui.confirmEmailScreen
import ui.doctorPatientScreen
import ui.doctorScreen
import ui.passwordResetFormScreen
import ui.passwordResetSuccessScreen
import kotlinx.browser.window
import org.w3c.dom.url.URLSearchParams
import ui.findDoctorScreen
import ui.myRecordsScreen
import ui.recordEditorScreen
import ui.resetPasswordScreen

class App : Application() {
    override fun start(state: Map<String, Any>) {
        I18n.language = "en"
        val r = root("kvapp")

        var showAuth: (AuthTab) -> Unit = {}

        fun showHome() {
            r.removeAll()
            r.homeScreen()
        }

        fun showFind() {
            r.removeAll()
            r.findDoctorScreen(
                onLogout = {
                    ApiConfig.clearToken()
                    Session.clear()
                    showHome()
                }
            )
        }

        fun showPatient() {
            r.removeAll()
            r.patientScreen(
                onLogout = {
                    ApiConfig.clearToken()
                    Session.clear()
                    showHome()
                }
            )
        }

        fun showDoctor() {
            r.removeAll()
            r.doctorScreen(
                onLogout = { showHome() }
            )
        }

        fun showDoctorPatient(patientUserId: Long, patientRecordId: Long?) {
            r.removeAll()
            r.doctorPatientScreen(
                patientUserId = patientUserId,
                patientRecordId = patientRecordId,
                onLogout = {
                    ApiConfig.clearToken()
                    Session.clear()
                    showHome()
                },
                onBack = { showDoctor() }
            )
        }

        fun showDoctorPatient(patientId: Long) {
            r.removeAll()
            r.doctorPatientScreen(
                patientUserId = patientId,
                patientRecordId = null,
                onLogout = {
                    ApiConfig.clearToken()
                    Session.clear()
                    showHome()
                },
                onBack = { showDoctor() }
            )
        }

        fun showMyRecords() {
            r.removeAll()
            r.myRecordsScreen(
                onLogout = {
                    ApiConfig.clearToken()
                    Session.clear()
                    showHome()
                }
            )
        }

        fun showRecordEditor(id: String) {
            r.removeAll()
            r.recordEditorScreen(recordId = id) { showMyRecords() }
        }

        fun showResetPassword() {
            r.removeAll()
            r.resetPasswordScreen()
        }

        fun showPasswordResetForm(token: String?) {
            r.removeAll()
            r.passwordResetFormScreen(token)
        }

        fun showPasswordResetSuccess() {
            r.removeAll()
            r.passwordResetSuccessScreen()
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
                onLogin = { data ->
                    Session.isLoggedIn = true
                    when (data.accountType.uppercase()) {
                        "DOCTOR" -> showDoctor()
                        else -> showPatient()
                    }
                },
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
        Navigator.showMyRecords = ::showMyRecords
        Navigator.showRecordEditor = ::showRecordEditor
        Navigator.showDoctor = ::showDoctor
        Navigator.showDoctorPatient = ::showDoctorPatient

        Navigator.showPasswordResetSuccess = ::showPasswordResetSuccess
        val currentPath = window.location.pathname
        if (currentPath == "/auth/password/reset") {
            val params = URLSearchParams(window.location.search)
            val token = params.get("token")
            showPasswordResetForm(token)
        } else {
            showDoctor()
        }
    }
}


