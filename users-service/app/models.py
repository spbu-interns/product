from pydantic import BaseModel, EmailStr, Field

# Входная модель при создании пользователя
class UserIn(BaseModel):
    email: EmailStr
    login: str = Field(min_length=3, max_length=128)
    password: str = Field(min_length=6)
    role: str  # CLIENT | DOCTOR | ADMIN

# Выходная модель без password_hash
class UserOut(BaseModel):
    id: int
    email: EmailStr
    login: str
    role: str