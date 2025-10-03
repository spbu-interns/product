from typing import List, Optional
from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.exc import IntegrityError
from sqlalchemy import text
from .db import get_session
from .models import UserIn, UserOut, RegistrationIn
from . import repository as repo

app = FastAPI(title="Users DB API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://127.0.0.1:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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