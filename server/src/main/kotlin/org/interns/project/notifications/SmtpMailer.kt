package org.interns.project.notifications

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

class SmtpMailer(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String,
    private val ssl: Boolean,
    private val starttls: Boolean,
    private val connectTimeoutMs: Int = 8000,
    private val writeTimeoutMs: Int = 8000,
    private val readTimeoutMs: Int = 8000,
    private val defaultFrom: String,
    private val defaultReplyTo: String?
) : Mailer {

    private val session: Session by lazy {
        val props = Properties().apply {
            put("mail.smtp.host", host)
            put("mail.smtp.port", port.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.ssl.enable", ssl.toString())
            put("mail.smtp.starttls.enable", starttls.toString())
            put("mail.smtp.connectiontimeout", connectTimeoutMs.toString())
            put("mail.smtp.writetimeout", writeTimeoutMs.toString())
            put("mail.smtp.timeout", readTimeoutMs.toString())
        }
        Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, password)
            }
        }).apply { debug = false }
    }

    override fun send(message: Mailer.Message) {
        val msg = MimeMessage(session)
        msg.setFrom(InternetAddress(message.from ?: defaultFrom))
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(message.to))
        if (!message.replyTo.isNullOrBlank()) {
            msg.replyTo = arrayOf(InternetAddress(message.replyTo))
        } else if (!defaultReplyTo.isNullOrBlank()) {
            msg.replyTo = arrayOf(InternetAddress(defaultReplyTo))
        }
        msg.subject = message.subject
        when {
            !message.htmlBody.isNullOrBlank() -> msg.setContent(message.htmlBody, "text/html; charset=UTF-8")
            !message.textBody.isNullOrBlank() -> msg.setText(message.textBody, Charsets.UTF_8.name())
            else -> msg.setText("", Charsets.UTF_8.name())
        }
        Transport.send(msg)
    }
}