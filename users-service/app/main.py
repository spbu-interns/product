from fastapi import FastAPI, HTTPException, Query
from sqlalchemy.exc import IntegrityError
from typing import List, Optional
from sqlalchemy import text
from .db import get_session
from .models import UserIn, UserOut
from . import repository as repo
from typing import List
app = FastAPI(title="Users DB API")


@app.get("/health")
def health():
    return {"status": "ok"}

@app.get("/users/by-email/{email}", response_model=UserOut | None)
def find_by_email(email: str):
    s = get_session()
    try:
        u = repo.find_by_email(s, email)
        return u
    finally:
        s.close()

@app.get("/users/by-login/{login}", response_model=UserOut | None)
def find_by_login(login: str):
    s = get_session()
    try:
        u = repo.find_by_login(s, login)
        return u
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
        
        
@app.get("/users", response_model=List[UserOut])
def get_users(role: Optional[str] = Query(None)):
    s = get_session()
    try:
        if role:
            rows = s.execute(
                text("select id,email,login,role from users where role=:r"),
                {"r": role}
            ).mappings().all()
        else:
            rows = s.execute(
                text("select id,email,login,role from users")
            ).mappings().all()
        return [dict(r) for r in rows]
    finally:
        s.close()