package org.interns.project.notifications

interface Mailer {
    data class Message(
        val to: String,
        val subject: String,
        val htmlBody: String?,
        val textBody: String?,
        val from: String? = null,
        val replyTo: String? = null
    )
    fun send(message: Message)
}