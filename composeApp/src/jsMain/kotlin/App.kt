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
import ui.patientAppointmentsScreen
import ui.patientMedicalRecordsScreen
import ui.findPatientScreen
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.url.URLSearchParams
import ui.findDoctorScreen
import ui.myRecordsScreen
import ui.recordEditorScreen
import ui.resetPasswordScreen
import ui.patientProfileEditScreen
import ui.doctorProfileEditScreen
import ui.DoctorSection

class App : Application() {

    override fun start(state: Map<String, Any>) {
        I18n.language = "en"
        val r = root("kvapp")
        val appScope = MainScope()
        val initialParams = URLSearchParams(window.location.search)
        Session.restoreFromStorage()
        if (window.location.pathname != "/auth/password/reset") {
            Session.ensureTokenFromLink(initialParams.get("token"))
        }

        lateinit var renderRoute: (String, URLSearchParams) -> Unit
        lateinit var navigate: (String, URLSearchParams) -> Unit

        fun go(path: String, params: URLSearchParams = URLSearchParams()) = navigate(path, params)

        fun showHome() {
            r.removeAll()
            r.homeScreen()
        }

        fun showFind() {
            r.removeAll()
            if (Session.accountType == "DOCTOR") {
                r.findPatientScreen(
                    onLogout = {
                        ApiConfig.clearToken()
                        Session.clear()
                        go("/")
                    }
                )
            } else {
                r.findDoctorScreen(
                    onLogout = {
                        ApiConfig.clearToken()
                        Session.clear()
                        go("/")
                    }
                )
            }
        }

        fun showFindPatients() {
            r.removeAll()
            r.findPatientScreen(
                onLogout = {
                    ApiConfig.clearToken()
                    Session.clear()
                    go("/")
                }
            )
        }

        fun showPatient() {
            r.removeAll()
            r.patientScreen(
                onLogout = {
                    ApiConfig.clearToken()
                    Session.clear()
                    go("/")
                }
            )
        }

        fun showPatientMedicalRecords() {
            r.removeAll()
            r.patientMedicalRecordsScreen(
                onLogout = {
                    ApiConfig.clearToken()
                    Session.clear()
                    go("/")
                }
            )
        }

        fun showAppointments(appointmentId: Long? = null) {
            r.removeAll()
            r.patientAppointmentsScreen(
                appointmentId = appointmentId,
                onLogout = {
                    ApiConfig.clearToken()
                    Session.clear()
                    go("/")
                }
            )
        }

        fun showDoctor(initialSection: DoctorSection = DoctorSection.OVERVIEW) {
            r.removeAll()
            r.doctorScreen(
                initialSection = initialSection,
                onLogout = { go("/") }
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
                    go("/")
                },
                onBack = { go("/doctor/find") }
            )
        }

        fun showMyRecords() {
            r.removeAll()
            r.myRecordsScreen(
                onLogout = {
                    ApiConfig.clearToken()
                    Session.clear()
                    go("/")
                }
            )
        }

        fun showRecordEditor(id: String) {
            r.removeAll()
            r.recordEditorScreen(recordId = id) { go("/patient/records") }
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
            r.stubScreen(message = message) { window.history.back() }
        }

        fun showConfirmEmail(email: String) {
            r.removeAll()
            r.confirmEmailScreen(email)
        }

        fun showPatientProfileEdit() {
            r.removeAll()
            r.patientProfileEditScreen(
                onBack = { go("/patient") }
            )
        }

        fun showDoctorProfileEdit() {
            r.removeAll()
            r.doctorProfileEditScreen(
                onBack = {
                    if (window.history.length > 0) {
                        window.history.back()
                    } else {
                        go("/doctor")
                    }
                }
            )
        }

        fun showAuth(tab: AuthTab) {
            r.removeAll()
            r.authScreen(
                initial = tab,
                onLogin = { data ->
                    Session.setSession(
                        token = data.token,
                        userId = data.userId,
                        email = data.email,
                        accountType = data.accountType,
                        firstName = data.firstName,
                        lastName = data.lastName
                    )

                    appScope.launch {
                        Session.hydrateFromBackend()
                        val needsProfile = Session.requiresProfileCompletion()
                        when (data.accountType.uppercase()) {
                            "DOCTOR" -> if (needsProfile) go("/doctor/profile") else go("/doctor")
                            else -> if (needsProfile) go("/patient/profile") else go("/patient")
                        }
                    }
                },
                onRegister = { email, _, accountType ->
                    go(
                        "/auth/confirm",
                        URLSearchParams().apply { set("email", email); set("role", accountType) }
                    )
                },
                onGoHome = { go("/") }
            )
        }

        renderRoute = { path: String, params: URLSearchParams ->
            if (path != "/auth/password/reset") {
                Session.ensureTokenFromLink(params.get("token"))
            }

            val role = Session.accountType?.uppercase()
            val normalizedRole = when (role) {
                "CLIENT" -> "PATIENT"
                else -> role
            }
            val redirected = when {
                path.startsWith("/patient") && normalizedRole == "DOCTOR" -> {
                    go("/doctor")
                    true
                }
                path.startsWith("/doctor") && normalizedRole == "PATIENT" -> {
                    go("/patient")
                    true
                }
                else -> false
            }

            if (!redirected) {
                when {
                    path == "/" -> showHome()
                    path == "/find" -> showFind()
                    path == "/doctor/find" -> showFindPatients()
                    path == "/auth/login" -> showAuth(AuthTab.LOGIN)
                    path == "/auth/register" -> showAuth(AuthTab.REGISTER)
                    path == "/auth/password/forgot" -> showResetPassword()
                    path == "/auth/password/reset" -> showPasswordResetForm(params.get("token"))
                    path == "/auth/password/reset/success" -> showPasswordResetSuccess()
                    path == "/auth/confirm" -> showConfirmEmail(params.get("email") ?: Session.email ?: "")
                    path == "/patient" -> showPatient()
                    path == "/patient/medical-records" -> showPatientMedicalRecords()
                    path == "/patient/appointments" -> showAppointments()
                    path.startsWith("/patient/appointments/") -> {
                        val appointmentId = path.removePrefix("/patient/appointments/").toLongOrNull()
                        showAppointments(appointmentId)
                    }
                    path == "/patient/records" -> showMyRecords()
                    path.startsWith("/patient/records/") -> showRecordEditor(path.removePrefix("/patient/records/"))
                    path == "/patient/profile" -> showPatientProfileEdit()
                    path == "/doctor" -> showDoctor()
                    path == "/doctor/schedule" -> showDoctor(DoctorSection.SCHEDULE)
                    path.startsWith("/doctor/patient/") -> {
                        val patientId = path.removePrefix("/doctor/patient/").toLongOrNull()
                        val recordId = params.get("recordId")?.toLongOrNull()
                        if (patientId != null) {
                            showDoctorPatient(patientId, recordId)
                        } else {
                            showDoctor()
                        }
                    }
                    path == "/doctor/profile" -> showDoctorProfileEdit()
                    path == "/stub" -> showStub(params.get("message") ?: "Раздел в разработке")
                    path == "/auth" -> showAuth(AuthTab.LOGIN)
                    else -> {
                        window.history.replaceState(null, "", "/")
                        showHome()
                    }
                }
            }
        }

        navigate = { path: String, params: URLSearchParams ->
            val merged = URLSearchParams(params)
            (Session.token ?: ApiConfig.getToken())?.let { merged.set("token", it) }

            val queryString = merged.toString()
            val url = if (queryString.isNotEmpty()) "$path?$queryString" else path

            val currentQuery = window.location.search.removePrefix("?")
            if (window.location.pathname != path || currentQuery != queryString) {
                window.history.pushState(null, "", url)
            }

            renderRoute(path, merged)
        }

        Navigator.showHome = { go("/") }
        Navigator.showFind = { go("/find") }
        Navigator.showFindPatient = { go("/doctor/find") }
        Navigator.showLogin = { go("/auth/login") }
        Navigator.showPatient = { go("/patient") }
        Navigator.showResetPassword = { go("/auth/password/forgot") }
        Navigator.showStub = { message -> go("/stub", URLSearchParams().apply { set("message", message) }) }
        Navigator.showRegister = { go("/auth/register") }
        Navigator.showConfirmEmail = { email ->
            go("/auth/confirm", URLSearchParams().apply { set("email", email) })
        }
        Navigator.showPatientMedicalRecords = { go("/patient/medical-records") }
        Navigator.showMyRecords = { go("/patient/records") }
        Navigator.showRecordEditor = { id -> go("/patient/records/$id") }
        Navigator.showDoctor = { go("/doctor") }
        Navigator.showDoctorSchedule = { go("/doctor/schedule") }
        Navigator.showDoctorPatient = { patientId, patientRecordId ->
            val params = URLSearchParams()
            patientRecordId?.let { params.set("recordId", it.toString()) }
            go("/doctor/patient/$patientId", params)
        }
        Navigator.showAppointments = { go("/patient/appointments") }
        Navigator.showAppointmentDetails = { id -> go("/patient/appointments/$id") }
        Navigator.showPatientProfileEdit = { go("/patient/profile") }
        Navigator.showDoctorProfileEdit = { go("/doctor/profile") }
        Navigator.showPasswordResetSuccess = { go("/auth/password/reset/success") }

        window.onpopstate = {
            renderRoute(window.location.pathname.ifBlank { "/" }, URLSearchParams(window.location.search))
        }

        renderRoute(window.location.pathname.ifBlank { "/" }, initialParams)
    }
}