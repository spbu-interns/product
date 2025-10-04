from typing import List, Optional
from fastapi import FastAPI, HTTPException, Query
from sqlalchemy.exc import IntegrityError
from sqlalchemy import text
from .db import get_session
from .models import UserIn, UserOut, RegistrationIn, LoginIn, ApiLoginResponse
from . import repository as repo
from passlib.hash import bcrypt

app = FastAPI(title="Users DB API")

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
        except IntegrityError:
            s.rollback()
            raise HTTPException(status_code=409, detail="email or login already exists")
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

@app.post("/auth/login", response_model=ApiLoginResponse)
def auth_login(req: LoginIn):
    s = get_session()
    try:
        u = repo.find_auth_by_login_or_email(s, req.login_or_email)
        if (not u) or (not u["is_active"]) or (not bcrypt.verify(req.password, u["password_hash"])):
            raise HTTPException(status_code=401, detail="invalid login or password")
        return ApiLoginResponse(success=True, role=u["role"])
    finally:
        s.close()