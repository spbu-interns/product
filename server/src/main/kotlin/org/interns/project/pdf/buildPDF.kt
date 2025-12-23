package org.interns.project.pdf

import org.interns.project.dto.ClientProfileDto
import org.interns.project.dto.MedicalRecordOutDto
import org.interns.project.dto.UserResponseDto
import org.xhtmlrenderer.pdf.ITextRenderer
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun mapGender(gender: String?): String {
    return when (gender) {
        "FEMALE" -> "Женский"
        "MALE" -> "Мужской"
        else -> "—"
    }
}

fun buildMedicalRecordHtml(
    record: MedicalRecordOutDto,
    user: UserResponseDto,
    profile: ClientProfileDto
): String {

    val fullName = listOfNotNull(
        user.surname,
        user.name,
        user.patronymic
    ).joinToString(" ")

    return """
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <style>
        body {
            font-family: "DejaVu Sans";
            font-size: 12px;
            line-height: 1.5;
            margin: 40px;
        }

        h1 {
            text-align: center;
            letter-spacing: 1px;
            margin-bottom: 30px;
        }

        .section {
            margin-bottom: 18px;
        }

        .row {
            margin-bottom: 4px;
        }

        .label {
            font-weight: bold;
        }

        .patient-box {
            border: 1px solid #000;
            padding: 12px;
            margin-bottom: 30px;
        }

        .medical-text {
            border: 1px solid #000;
            padding: 10px;
            margin-top: 6px;
        }

        .footer-date {
            margin-top: 50px;
            text-align: right;
            font-style: italic;
        }
    </style>
</head>
<body>

<h1>МЕДИЦИНСКИЙ ПРОТОКОЛ</h1>

<div class="patient-box">
    <div class="row"><span class="label">ФИО пациента:</span> $fullName</div>
    <div class="row"><span class="label">Дата рождения:</span> ${formatRussianDate(user.dateOfBirth)}</div>
    <div class="row"><span class="label">Пол:</span> ${mapGender(user.gender)}</div>
    <div class="row"><span class="label">Телефон:</span> ${user.phoneNumber ?: "—"}</div>
    <div class="row"><span class="label">Адрес:</span> ${profile.address ?: "—"}</div>
    <div class="row"><span class="label">Группа крови:</span> ${profile.bloodType ?: "—"}</div>
</div>

${record.diagnosis?.let {
        """
        <div class="section">
            <div class="label">Диагноз:</div>
            <div class="medical-text">$it</div>
        </div>
        """
    } ?: ""}

${record.symptoms?.let {
        """
        <div class="section">
            <div class="label">Жалобы и симптомы:</div>
            <div class="medical-text">$it</div>
        </div>
        """
    } ?: ""}

${record.treatment?.let {
        """
        <div class="section">
            <div class="label">Назначенное лечение:</div>
            <div class="medical-text">$it</div>
        </div>
        """
    } ?: ""}

${record.recommendations?.let {
        """
        <div class="section">
            <div class="label">Рекомендации:</div>
            <div class="medical-text">$it</div>
        </div>
        """
    } ?: ""}

<div class="footer-date">
    Дата оформления: ${formatRussianDate(record.createdAt)}
</div>

</body>
</html>
""".trimIndent()
}

fun generatePdf(html: String): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val renderer = ITextRenderer()

    val fontPath = requireNotNull(
        Thread.currentThread().contextClassLoader
            .getResource("fonts/DejaVuSans.ttf")
    ).toString()

    renderer.fontResolver.addFont(
        fontPath,
        com.lowagie.text.pdf.BaseFont.IDENTITY_H,
        com.lowagie.text.pdf.BaseFont.EMBEDDED
    )

    renderer.setDocumentFromString(html)
    renderer.layout()
    renderer.createPDF(outputStream)

    return outputStream.toByteArray()
}

fun formatRussianDate(date: String?): String {
    if (date == null) return "—"

    val parsed = LocalDate.parse(date.substring(0, 10))
    val formatter = DateTimeFormatter.ofPattern(
        "d MMMM yyyy года",
        Locale("ru")
    )
    return parsed.format(formatter)
}