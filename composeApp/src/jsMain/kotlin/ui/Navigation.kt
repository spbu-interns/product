package ui

object Navigator {
    var showHome: () -> Unit = {}
    var showFind: () -> Unit = {}
    var showLogin: () -> Unit = {}
    var showPatient: () -> Unit = {}
    var showDoctor: () -> Unit = {}
    var showStub: (String) -> Unit = {}
    var showResetPassword: () -> Unit = {}
    var showRegister: () -> Unit = {}
    var showConfirmEmail: (String) -> Unit = {}
    var showMyRecords: () -> Unit = {}
    var showRecordEditor: (String) -> Unit = {}
    var showDoctorPatient: (Long, Long?) -> Unit = { _, _ -> }

    var showPatientProfileEdit: () -> Unit = {}
    var showDoctorProfileEdit: () -> Unit = {}
}