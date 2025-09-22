from typing import Optional
from sqlalchemy import text
from sqlalchemy.orm import Session
from passlib.hash import bcrypt
from .models import UserIn

# findByEmail(email: String): User?
def find_by_email(s: Session, email: str) -> Optional[dict]:
    r = s.execute(
        text("select id,email,login,role from users where email=:e"),
        {"e": email}
    ).mappings().first()
    return dict(r) if r else None

# findByLogin(login: String): User?
def find_by_login(s: Session, login: str) -> Optional[dict]:
    r = s.execute(
        text("select id,email,login,role from users where login=:l"),
        {"l": login}
    ).mappings().first()
    return dict(r) if r else None

# existsByEmail(email: String): Boolean
def exists_by_email(s: Session, email: str) -> bool:
    r = s.execute(
        text("select 1 from users where email=:e limit 1"),
        {"e": email}
    ).first()
    return bool(r)

# existsByLogin(login: String): Boolean
def exists_by_login(s: Session, login: str) -> bool:
    r = s.execute(
        text("select 1 from users where login=:l limit 1"),
        {"l": login}
    ).first()
    return bool(r)

# insertUser(user: User)
def insert_user(s: Session, user: UserIn) -> dict:
    if user.role not in ("CLIENT", "DOCTOR", "ADMIN"):
        raise ValueError("invalid role")
    pwd_hash = bcrypt.hash(user.password)
    r = s.execute(
        text("""
            insert into users(email,login,password_hash,role)
            values (:e,:l,:p,:r)
            returning id,email,login,role
        """),
        {"e": user.email, "l": user.login, "p": pwd_hash, "r": user.role}
    ).mappings().first()
    s.commit()
    return dict(r)