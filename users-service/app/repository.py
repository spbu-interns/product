from typing import Optional, List, Dict
from sqlalchemy import text
from sqlalchemy.orm import Session
from passlib.hash import bcrypt
from .models import UserIn, RegistrationIn

def _rowmap(r) -> Dict:
    return dict(r) if r else None

def find_by_email(s: Session, email: str) -> Optional[Dict]:
    r = s.execute(text("""
        select id,email,login,role,first_name,last_name,patronymic,
               phone_number,clinic_id,is_active,created_at,updated_at
        from users where email=:e
    """), {"e": email}).mappings().first()
    return _rowmap(r)

def find_by_login(s: Session, login: str) -> Optional[Dict]:
    r = s.execute(text("""
        select id,email,login,role,first_name,last_name,patronymic,
               phone_number,clinic_id,is_active,created_at,updated_at
        from users where login=:l
    """), {"l": login}).mappings().first()
    return _rowmap(r)

def exists_by_email(s: Session, email: str) -> bool:
    return bool(s.execute(text("select 1 from users where email=:e limit 1"),
                          {"e": email}).first())

def exists_by_login(s: Session, login: str) -> bool:
    return bool(s.execute(text("select 1 from users where login=:l limit 1"),
                          {"l": login}).first())

def insert_user(s: Session, user: UserIn) -> Dict:
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
                  phone_number,clinic_id,is_active,created_at,updated_at
    """), {
        "e": user.email,
        "l": user.login,
        "p": pwd_hash,
        "r": user.role,
        "fn": user.first_name,
        "ln": user.last_name,
        "pn": user.patronymic,
        "ph": user.phone_number,
        "cid": user.clinic_id,
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
                   phone_number,clinic_id,is_active,created_at,updated_at
            from users where role=:r
            order by id
        """), {"r": role}).mappings().all()
    else:
        rows = s.execute(text("""
            select id,email,login,role,first_name,last_name,patronymic,
                   phone_number,clinic_id,is_active,created_at,updated_at
            from users
            order by id
        """)).mappings().all()
    return [dict(r) for r in rows]