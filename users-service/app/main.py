from typing import List, Optional
from fastapi import FastAPI, HTTPException, Query, BackgroundTasks
from sqlalchemy.exc import IntegrityError
from psycopg2 import errors as pgerr
from fastapi import HTTPException
from sqlalchemy import text
from .db import get_session
from .models import (
    UserIn, UserOut,
    EmailStartVerificationIn, EmailVerifyIn,
    PasswordForgotIn, PasswordResetIn, RegistrationIn
)
from . import repository as repo
from .mailer import MailSettings, Mailer, verification_email_link, reset_email_link
from .repository import RESET_TOKEN_TTL_MIN

app = FastAPI(title="Users DB API")

settings = MailSettings()
mailer = Mailer(settings)

@app.get("/health")
def health():
    return {"status": "ok"}

@app.get("/users", response_model=List[UserOut])
def get_users(role: Optional[str] = Query(None)):
    s = get_session()
    try:
        return repo.list_users(s, role)
    finally:
        s.close()

@app.get("/users/by-email/{email}", response_model=Optional[UserOut])
def find_by_email(email: str):
    s = get_session()
    try:
        return repo.find_by_email(s, email)
    finally:
        s.close()

@app.get("/users/by-login/{login}", response_model=Optional[UserOut])
def find_by_login(login: str):
    s = get_session()
    try:
        return repo.find_by_login(s, login)
    finally:
        s.close()

@app.get("/users/exists/email/{email}", response_model=bool)
def exists_by_email(email: str):
    s = get_session()
    try:
        return repo.exists_by_email(s, email)
    finally:
        s.close()

@app.get("/users/exists/login/{login}", response_model=bool)
def exists_by_login(login: str):
    s = get_session()
    try:
        return repo.exists_by_login(s, login)
    finally:
        s.close()

@app.post("/users", response_model=UserOut, status_code=201)
def insert_user(user: UserIn):
    s = get_session()
    try:
        try:
            return repo.insert_user(s, user)
        except IntegrityError as e:
            s.rollback()
            raise _map_integrity(e)
        except ValueError as ve:
            s.rollback()
            raise HTTPException(status_code=400, detail=str(ve))
    finally:
        s.close()

@app.post("/register", response_model=UserOut, status_code=201)
def register(reg: RegistrationIn):
    """
    Соответствует требованиям "исчерпывающих полей регистрации":
    id (выдаёт БД), username, password, email, is_active, created_at/updated_at задаются БД.
    """
    s = get_session()
    try:
        try:
            return repo.register_user(s, reg)
        except IntegrityError:
            s.rollback()
            raise HTTPException(status_code=409, detail="email or login already exists")
    finally:
        s.close()

@app.post("/auth/email/start")
def start_email_verification(body: EmailStartVerificationIn, bt: BackgroundTasks):
    s = get_session()
    try:
        uid = repo.find_user_id_by_email(s, body.email)
        if not uid:
            raise HTTPException(404, "user not found")
        raw_token = repo.start_email_verification(s, uid)

        subj, html, text = verification_email_link(body.email, raw_token, settings.APP_BASE_URL)
        bt.add_task(mailer.send, to=body.email, subject=subj, html=html, text=text)

        return {"token": raw_token, "message": "verification link sent to email"}
    finally:
        s.close()

@app.post("/auth/email/verify")
def verify_email(body: EmailVerifyIn):
    s = get_session()
    try:
        ok = repo.verify_email_token(s, body.token)
        if not ok:
            raise HTTPException(400, "invalid or expired token")
        return {"status": "verified"}
    finally:
        s.close()

@app.get("/auth/email/verify")
def verify_email_by_link(token: str):
    s = get_session()
    try:
        ok = repo.verify_email_token(s, token)
        if not ok:
            raise HTTPException(400, "invalid or expired token")
        return {"status": "verified"}
    finally:
        s.close()

# == Password reset ==
@app.post("/auth/password/forgot")
def password_forgot(body: PasswordForgotIn, bt: BackgroundTasks):
    s = get_session()
    try:
        uid = repo.find_user_id_by_email(s, body.email)
        if not uid:
            # можем возвращать всегда 200, чтобы не палить существование адреса
            raise HTTPException(404, "user not found")

        raw = repo.start_password_reset(s, uid)

        subj, html, text = reset_email_link(
            to=body.email,
            token=raw,
            base_url=settings.APP_BASE_URL,
            ttl_minutes=RESET_TOKEN_TTL_MIN
        )
        bt.add_task(mailer.send, to=body.email, subject=subj, html=html, text=text)

        return {"token": raw, "message": "reset link sent to email"}
    finally:
        s.close()

@app.post("/auth/password/reset")
def password_reset(body: PasswordResetIn):
    s = get_session()
    try:
        ok = repo.consume_password_reset(s, body.token, body.new_password)
        if not ok:
            raise HTTPException(400, "invalid or expired token")
        return {"status": "password_changed"}
    finally:
        s.close()

def _map_integrity(e: IntegrityError) -> HTTPException:
    orig = getattr(e, "orig", None)
    if isinstance(orig, pgerr.UniqueViolation):
        return HTTPException(status_code=409, detail="email or login already exists")
    if isinstance(orig, pgerr.ForeignKeyViolation):
        return HTTPException(status_code=400, detail="clinic_id does not exist")
    if isinstance(orig, pgerr.CheckViolation):
        return HTTPException(status_code=400, detail="check constraint failed")
    if isinstance(orig, pgerr.NotNullViolation):
        return HTTPException(status_code=400, detail="missing required field")
    # по умолчанию
    return HTTPException(status_code=400, detail="integrity error")