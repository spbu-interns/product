package org.interns.project.notifications

object EmailTemplates {
    fun emailVerificationCode(to: String, code: String, ttlMinutes: Int): Mailer.Message {
        val subject = "Код подтверждения — действует $ttlMinutes мин"
        val text = """
            Здравствуйте!

            Ваш код подтверждения: $code
            Срок действия — $ttlMinutes минут(ы).
            Если вы не запрашивали код — просто проигнорируйте это письмо.
        """.trimIndent()
        val html = """
            <p>Здравствуйте!</p>
            <p>Ваш код подтверждения: <b style=\"font-size:18px\">$code</b></p>
            <p>Срок действия — $ttlMinutes минут(ы).</p>
            <p style=\"color:#777\">Если вы не запрашивали код — просто проигнорируйте это письмо.</p>
        """.trimIndent()
        return Mailer.Message(to = to, subject = subject, htmlBody = html, textBody = text)
    }

    fun passwordResetLink(to: String, link: String, ttlMinutes: Int): Mailer.Message {
        val subject = "Сброс пароля — ссылка действует $ttlMinutes мин"
        val text = """
            Здравствуйте!
            Вы запросили смену пароля
            Ссылка для сброса (действительна $ttlMinutes мин.):
            $link
            Если это были не вы — проигнорируйте письмо.
        """.trimIndent()
        val html = """
            <p>Здравствуйте!</p>
            <p>Вы запросили смену пароля</p>
            <p>Ссылка для сброса (действительна $ttlMinutes мин.):<br>
            <a href="$link">$link</a></p>
            <p>Если это были не вы — проигнорируйте письмо.</p>
        """.trimIndent()
        return Mailer.Message(to = to, subject = subject, htmlBody = html, textBody = text)
    }
}
