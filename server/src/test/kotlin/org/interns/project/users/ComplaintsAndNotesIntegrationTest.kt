package org.interns.project.users

import kotlinx.coroutines.runBlocking
import org.interns.project.users.model.UserInDto
import org.interns.project.users.model.*
import org.interns.project.users.repo.ApiUserRepo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComplaintsAndNotesIntegrationTest {

    @Test
    fun patientComplaint_crud_realServer() = runBlocking {
        val repo = ApiUserRepo(baseUrl = "http://127.0.0.1:8001")
        try {
            val uniq = System.currentTimeMillis().toString()
            val email = "pc_$uniq@example.com"
            val login = "pc_$uniq"
            val password = "secret123"

            val patient = repo.saveByApi(UserInDto(email, login, password, role = "CLIENT"))
            assertTrue(patient.id > 0)
            val patientId = patient.id

            //println("👉 PATIENT_ID=$patientId")

            val created = repo.createComplaint(
                patientId = patientId,
                input = ComplaintIn(
                    title = "Болит горло",
                    body = "Третий день температура 38.1"
                )
            )

            assertTrue(created.id > 0)

            //println("👉 COMPLAINT_ID=${created.id}")

            assertEquals(patientId, created.patientId)
            assertEquals(ComplaintStatus.OPEN, created.status)

            val all = repo.listComplaints(patientId)
            assertTrue(all.any { it.id == created.id })

            val patched = repo.patchComplaint(created.id, ComplaintPatch(status = ComplaintStatus.IN_PROGRESS))
            assertEquals(ComplaintStatus.IN_PROGRESS, patched.status)

            val deleted = repo.deleteComplaint(created.id)
            assertTrue(deleted)
        } finally {
            repo.close()
        }
    }

    @Test
    fun doctorNote_crud_realServer() = runBlocking {
        val repo = ApiUserRepo(baseUrl = "http://127.0.0.1:8001")
        try {
            val uniq = System.currentTimeMillis().toString()
            val pEmail = "dn_p_$uniq@example.com"
            val pLogin = "dn_p_$uniq"
            val dEmail = "dn_d_$uniq@example.com"
            val dLogin = "dn_d_$uniq"
            val password = "secret123"

            // пациент
            val patient = repo.saveByApi(UserInDto(pEmail, pLogin, password, role = "CLIENT"))
            assertTrue(patient.id > 0)
            val patientId = patient.id

            //println("👉 PATIENT_ID=$patientId")

            // врач — ОБЯЗАТЕЛЬНО с firstName и lastName
            val doctor = repo.saveByApi(
                UserInDto(
                    email = dEmail,
                    login = dLogin,
                    password = password,
                    role = "DOCTOR",
                    firstName = "John",
                    lastName = "Doe"
                    // clinicId = 1
                )
            )
            assertTrue(doctor.id > 0)
            val doctorId = doctor.id

            //println("👉 DOCTOR_ID=$doctorId")

            // создаём заметку врача
            val note = repo.createNote(
                patientId = patientId,
                input = NoteIn(
                    doctorId = doctorId,
                    note = "Осмотр: ангина. Назначить антибиотик.",
                    visibility = NoteVisibility.INTERNAL
                )
            )
            assertTrue(note.id > 0)

            //println("👉 NOTE_ID=${note.id}")

            assertEquals(patientId, note.patientId)
            assertEquals(doctorId, note.doctorId)
            assertEquals(NoteVisibility.INTERNAL, note.visibility)

            val notesAll = repo.listNotes(patientId, includeInternal = true)
            assertTrue(notesAll.any { it.id == note.id })

            val notesPatientOnlyBefore = repo.listNotes(patientId, includeInternal = false)
            assertTrue(notesPatientOnlyBefore.none { it.id == note.id })

            val patched = repo.patchNote(note.id, NotePatch(visibility = NoteVisibility.PATIENT))
            assertEquals(NoteVisibility.PATIENT, patched.visibility)

            val notesPatientOnlyAfter = repo.listNotes(patientId, includeInternal = false)
            assertTrue(notesPatientOnlyAfter.any { it.id == note.id })

            val deleted = repo.deleteNote(note.id)
            assertTrue(deleted)
        } finally {
            repo.close()
        }
    }
}
