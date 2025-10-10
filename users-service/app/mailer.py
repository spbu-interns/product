import smtplib, ssl
from email.message import EmailMessage
from pydantic_settings import BaseSettings, SettingsConfigDict

class MailSettings(BaseSettings):
    SMTP_HOST: str = "smtp.yandex.ru"
    SMTP_PORT: int = 465
    SMTP_USERNAME: str
    SMTP_PASSWORD: str
    MAIL_FROM: str
    MAIL_REPLY_TO: str | None = None
    APP_BASE_URL: str = "http://localhost:8000"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

class Mailer:
    def __init__(self, s: MailSettings):
        self.s = s
        self._ctx = ssl.create_default_context()

    def send(self, *, to: str, subject: str, html: str, text: str | None = None):
        msg = EmailMessage()
        msg["From"] = self.s.MAIL_FROM
        msg["To"] = to
        if self.s.MAIL_REPLY_TO:
            msg["Reply-To"] = self.s.MAIL_REPLY_TO
        msg["Subject"] = subject
        msg.set_content(text or " ")
        msg.add_alternative(html, subtype="html")
        with smtplib.SMTP_SSL(self.s.SMTP_HOST, self.s.SMTP_PORT, context=self._ctx, timeout=15) as smtp:
            smtp.login(self.s.SMTP_USERNAME, self.s.SMTP_PASSWORD)
            smtp.send_message(msg)

def verification_email_link(to: str, token: str, base_url: str):
    link = f"{base_url}/auth/email/verify?token={token}"
    subject = "Подтверждение почты"
    html = f"""
    <p>Здравствуйте!</p>
    <p>Для подтверждения почты перейдите по ссылке:</p>
    <p><a href="{link}">{link}</a></p>
    <p>Если вы не запрашивали подтверждение — просто игнорируйте письмо.</p>
    """
    text = f"Подтвердите почту по ссылке: {link}"
    return subject, html, text

def reset_email_link(to: str, token: str, base_url: str, ttl_minutes: int):
    link = f"{base_url}/auth/password/reset?token={token}"
    subject = "Сброс пароля"
    html = f"""
    <p>Вы запросили смену пароля.</p>
    <p>Ссылка для сброса (действительна {ttl_minutes} мин.):<br>
    <a href="{link}">{link}</a></p>
    <p>Если это были не вы — проигнорируйте письмо.</p>
    """
    text = f"Ссылка для сброса (действительна {ttl_minutes} мин.): {link}"
    return subject, html, text