package org.interns.project.controller

import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.interns.project.pdf.buildMedicalRecordHtml
import org.interns.project.pdf.generatePdf
import org.interns.project.users.repo.ApiUserRepo

class MedicalDocumentController(private val api: ApiUserRepo) {
    fun registerRoutes(routing: Routing){
        /**
         * GET /clients/{clientId}/medical-documents/{recordId}/download
         * Скачать файл
         */
        routing.get("/clients/{clientId}/medical-documents/{recordId}/download") {
            val recId = call.parameters["recordId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "invalid recordId")

            val clId = call.parameters["clientId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "invalid clientId")

            val medicalRecord = api.getMedicalRecord(recId)
            val clientProfile = api.findClientById(clId)
            val user = api.findUserByClientId(clId)

            val html = buildMedicalRecordHtml(
                medicalRecord,
                user,
                clientProfile
            )

            val fileBytes = generatePdf(html)

            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(
                        ContentDisposition.Parameters.FileName,
                        "medical_protocol_$recId.pdf"
                    )
                    .toString()
            )

            call.respondBytes(
                bytes = fileBytes,
                contentType = ContentType.Application.Pdf
            )
        }
    }
}