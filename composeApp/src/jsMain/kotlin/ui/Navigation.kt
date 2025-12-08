package ui

object Navigator {
    var showHome: () -> Unit = {}
    var showFind: () -> Unit = {}
    var showFindPatient: () -> Unit = {}
    var showLogin: () -> Unit = {}
    var showPatient: () -> Unit = {}
    var showPatientMedicalRecords: () -> Unit = {}
    var showDoctor: () -> Unit = {}
    var showDoctorSchedule: () -> Unit = {}
    var showStub: (String) -> Unit = {}
    var showResetPassword: () -> Unit = {}
    var showPasswordResetSuccess: () -> Unit = {}
    var showRegister: () -> Unit = {}
    var showConfirmEmail: (String) -> Unit = {}
    var showMyRecords: () -> Unit = {}
    var showRecordEditor: (String) -> Unit = {}
    var showDoctorPatient: (Long?, Long?) -> Unit = { _, _ -> }
    var showAppointments: () -> Unit = {}
    var showAppointmentDetails: (Long) -> Unit = {}
    var showPatientProfileEdit: () -> Unit = {}
    var showDoctorProfileEdit: () -> Unit = {}
}