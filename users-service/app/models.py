from typing import Optional
from pydantic import BaseModel, EmailStr, Field

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
