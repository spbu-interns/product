from typing import Optional, List, Dict
from sqlalchemy import text
from sqlalchemy.orm import Session
from passlib.hash import bcrypt
from .models import UserIn, RegistrationIn
from sqlalchemy import or_
from datetime import datetime, timedelta
import secrets, hashlib


EMAIL_TOKEN_TTL_MIN = 30
RESET_TOKEN_TTL_MIN = 30


def _rowmap(r) -> Dict:
    return dict(r) if r else None

def find_by_email(s: Session, email: str) -> Optional[Dict]:
    r = s.execute(text("""
        select id,email,login,role,first_name,last_name,patronymic,
               phone_number,clinic_id,is_active,
               email_verified_at, password_changed_at,
               created_at, updated_at
        from users where email=:e
    """), {"e": email}).mappings().first()
    return _rowmap(r)

def find_by_login(s: Session, login: str) -> Optional[Dict]:
    r = s.execute(text("""
        select id,email,login,role,first_name,last_name,patronymic,
               phone_number,clinic_id,is_active,
               email_verified_at, password_changed_at,
               created_at, updated_at
        from users where login=:l
    """), {"l": login}).mappings().first()
    return _rowmap(r)

def exists_by_email(s: Session, email: str) -> bool:
    return bool(s.execute(text("select 1 from users where email=:e limit 1"),
                          {"e": email}).first())

def exists_by_login(s: Session, login: str) -> bool:
    return bool(s.execute(text("select 1 from users where login=:l limit 1"),
                          {"l": login}).first())

def insert_user(s: Session, user) -> Dict:
    if user.role not in ("CLIENT", "DOCTOR", "ADMIN"):
        raise ValueError("invalid role")
    if user.role == "DOCTOR" and (not user.first_name or not user.last_name):
        raise ValueError("doctor must have first_name and last_name")

    pwd_hash = bcrypt.hash(user.password)
    r = s.execute(text("""
        insert into users (email,login,password_hash,role,
                           first_name,last_name,patronymic,phone_number,
                           clinic_id,is_active)
        values (:e,:l,:p,:r,:fn,:ln,:pn,:ph,:cid,:ia)
        returning id,email,login,role,first_name,last_name,patronymic,
                  phone_number,clinic_id,is_active,
                  email_verified_at, password_changed_at,
                  created_at, updated_at
    """), {
        "e": user.email, "l": user.login, "p": pwd_hash, "r": user.role,
        "fn": user.first_name, "ln": user.last_name, "pn": user.patronymic,
        "ph": user.phone_number, "cid": user.clinic_id,
        "ia": True if user.is_active is None else user.is_active,
    }).mappings().first()
    s.commit()
    return dict(r)

def register_user(s: Session, reg: RegistrationIn) -> Dict:
    pwd_hash = bcrypt.hash(reg.password)
    r = s.execute(text("""
        insert into users (email,login,password_hash,is_active)
        values (:e,:l,:p,:ia)
        returning id,email,login,role,first_name,last_name,patronymic,
                  phone_number,clinic_id,is_active,created_at,updated_at
    """), {
        "e": reg.email,
        "l": reg.username,
        "p": pwd_hash,
        "ia": True if reg.is_active is None else reg.is_active,
    }).mappings().first()
    s.commit()
    return dict(r)

def list_users(s: Session, role: Optional[str]=None) -> List[Dict]:
    if role:
        rows = s.execute(text("""
            select id,email,login,role,first_name,last_name,patronymic,
                   phone_number,clinic_id,is_active,
                   email_verified_at, password_changed_at,
                   created_at, updated_at
            from users where role=:r order by id
        """), {"r": role}).mappings().all()
    else:
        rows = s.execute(text("""
            select id,email,login,role,first_name,last_name,patronymic,
                   phone_number,clinic_id,is_active,
                   email_verified_at, password_changed_at,
                   created_at, updated_at
            from users order by id
        """)).mappings().all()
    return [dict(r) for r in rows]

def find_auth_by_login_or_email(s: Session, v: str):
    r = s.execute(text("""
        select id, role, password_hash, is_active
        from users
        where login = :v or email = :v
        limit 1
    """), {"v": v}).mappings().first()
    return dict(r) if r else None

def _gen_token_and_hash() -> tuple[str, str]:
    raw = secrets.token_urlsafe(32)   # отдать пользователю
    h = hashlib.sha256(raw.encode("utf-8")).hexdigest()  # хранить в БД
    return raw, h

def start_email_verification(s: Session, user_id: int) -> str:
    raw, h = _gen_token_and_hash()
    expires = datetime.utcnow() + timedelta(minutes=EMAIL_TOKEN_TTL_MIN)

    # позволяем 1 активный (непогашенный) токен на пользователя
    s.execute(text("delete from email_verifications where user_id=:u and consumed_at is null"),
              {"u": user_id})
    s.execute(text("""
        insert into email_verifications(user_id, token_hash, expires_at)
        values (:u, :h, :e)
    """), {"u": user_id, "h": h, "e": expires})
    s.commit()
    return raw  # вернуть сырой токен (его будешь высылать по почте)

def verify_email_token(s: Session, raw_token: str) -> bool:
    h = hashlib.sha256(raw_token.encode("utf-8")).hexdigest()
    row = s.execute(text("""
        select id, user_id, expires_at, consumed_at
        from email_verifications
        where token_hash=:h
        limit 1
    """), {"h": h}).mappings().first()
    if not row:
        return False
    if row["consumed_at"] is not None or row["expires_at"] <= datetime.utcnow():
        return False

    # помечаем токен и ставим флаг пользователю
    s.execute(text("update email_verifications set consumed_at=now() where id=:id"),
              {"id": row["id"]})
    s.execute(text("update users set email_verified_at=now() where id=:u"),
              {"u": row["user_id"]})
    s.commit()
    return True

# --- password reset tokens ---

def start_password_reset(s: Session, user_id: int) -> str:
    raw, h = _gen_token_and_hash()
    expires = datetime.utcnow() + timedelta(minutes=RESET_TOKEN_TTL_MIN)
    s.execute(text("delete from password_reset_tokens where user_id=:u and consumed_at is null"),
              {"u": user_id})
    s.execute(text("""
        insert into password_reset_tokens(user_id, token_hash, expires_at)
        values (:u, :h, :e)
    """), {"u": user_id, "h": h, "e": expires})
    s.commit()
    return raw

def consume_password_reset(s: Session, raw_token: str, new_password: str) -> bool:
    h = hashlib.sha256(raw_token.encode("utf-8")).hexdigest()
    row = s.execute(text("""
        select id, user_id, expires_at, consumed_at
        from password_reset_tokens
        where token_hash=:h
        limit 1
    """), {"h": h}).mappings().first()
    if not row:
        return False
    if row["consumed_at"] is not None or row["expires_at"] <= datetime.utcnow():
        return False

    new_hash = bcrypt.hash(new_password)
    s.execute(text("update password_reset_tokens set consumed_at=now() where id=:id"),
              {"id": row["id"]})
    s.execute(text("""
        update users
        set password_hash=:ph, password_changed_at=now()
        where id=:u
    """), {"ph": new_hash, "u": row["user_id"]})
    s.commit()
    return True

def find_user_id_by_email(s: Session, email: str) -> Optional[int]:
    r = s.execute(text("select id from users where email=:e"), {"e": email}).first()
    return r[0] if r else None
