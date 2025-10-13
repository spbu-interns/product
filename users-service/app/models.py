from typing import Optional
from pydantic import BaseModel, EmailStr, Field
from typing import Literal

class UserIn(BaseModel):
    email: EmailStr
    login: str = Field(min_length=3, max_length=100)
    password: str = Field(min_length=6)
    role: str = "CLIENT"  # CLIENT | DOCTOR | ADMIN
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    patronymic: Optional[str] = None
    phone_number: Optional[str] = None
    clinic_id: Optional[int] = None
    is_active: Optional[bool] = True

class RegistrationIn(BaseModel):
    # соответствие «исчерпывающим полям регистрации»
    # id приходит/возвращается с сервера, в запросе не нужен
    username: str = Field(min_length=3, max_length=100)  # -> users.login
    password: str = Field(min_length=6)                  # -> bcrypt hash
    email: EmailStr
    is_active: Optional[bool] = True

from typing import Optional
from datetime import datetime
from pydantic import BaseModel, EmailStr, Field

class UserOut(BaseModel):
    id: int
    email: EmailStr
    login: str
    role: str
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    patronymic: Optional[str] = None
    phone_number: Optional[str] = None
    clinic_id: Optional[int] = None
    is_active: bool
    email_verified_at: Optional[datetime] = None
    password_changed_at: datetime 
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

class LoginIn(BaseModel):
    login_or_email: str = Field(min_length=3, max_length=100)
    password: str = Field(min_length=6)

class ApiLoginResponse(BaseModel):
    success: bool
    role: Optional[str] = None
    error: Optional[str] = None
    token: Optional[str] = None
    
class EmailStartVerificationIn(BaseModel):
    email: EmailStr

class EmailVerifyIn(BaseModel):
    token: str  # сырой токен из письма

class PasswordForgotIn(BaseModel):
    email: EmailStr

class PasswordResetIn(BaseModel):
    token: str
    new_password: str = Field(min_length=6)

# ---------- Complaints & Notes ----------

ComplaintStatus = Literal["OPEN", "IN_PROGRESS", "CLOSED"]
NoteVisibility = Literal["INTERNAL", "PATIENT"]

class ComplaintIn(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    body: str = Field(min_length=1)

class ComplaintOut(BaseModel):
    id: int
    patient_id: int
    title: str
    body: str
    status: ComplaintStatus
    created_at: datetime
    updated_at: datetime

class ComplaintPatch(BaseModel):
    title: Optional[str] = Field(default=None, min_length=1, max_length=200)
    body: Optional[str] = Field(default=None, min_length=1)
    status: Optional[ComplaintStatus] = None

class NoteIn(BaseModel):
    doctor_id: int
    note: str = Field(min_length=1)
    visibility: NoteVisibility = "INTERNAL"

class NoteOut(BaseModel):
    id: int
    patient_id: int
    doctor_id: int
    note: str
    visibility: NoteVisibility
    created_at: datetime
    updated_at: datetime

class NotePatch(BaseModel):
    note: Optional[str] = Field(default=None, min_length=1)
    visibility: Optional[NoteVisibility] = None