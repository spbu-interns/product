from typing import Optional
from pydantic import BaseModel, EmailStr, Field
from typing import Literal
from datetime import datetime, date

Gender = Literal["MALE", "FEMALE"]

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

    # старые поля (совместимость)
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    patronymic: Optional[str] = None
    phone_number: Optional[str] = None
    clinic_id: Optional[int] = None

    # НОВЫЕ поля профиля
    name: Optional[str] = None
    surname: Optional[str] = None
    date_of_birth: Optional[date] = None
    avatar: Optional[str] = None
    gender: Optional[Gender] = None

    is_active: bool
    email_verified_at: Optional[datetime] = None
    password_changed_at: datetime
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None
    
#частичного обновления профиля (PATCH):
class UserProfilePatch(BaseModel):
    # старые полям для совместимости — если нужно поддерживать
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    patronymic: Optional[str] = None
    phone_number: Optional[str] = None
    clinic_id: Optional[int] = None

    # НОВЫЕ поля профиля по ТЗ
    name: Optional[str] = None
    surname: Optional[str] = None
    date_of_birth: Optional[date] = None
    avatar: Optional[str] = None
    gender: Optional[Gender] = None

class LoginIn(BaseModel):
    login_or_email: str = Field(min_length=3, max_length=100)
    password: str = Field(min_length=6)

class ApiLoginResponse(BaseModel):
    success: bool
    role: Optional[str] = None
    error: Optional[str] = None
    
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
    
# ====== NEW: Domain v2 models ======
from datetime import datetime

# --- Clients / Doctors / Admins ---
class ClientIn(BaseModel):
    user_id: int
    blood_type: Optional[str] = None
    height: Optional[float] = None
    weight: Optional[float] = None
    emergency_contact_name: Optional[str] = None
    emergency_contact_number: Optional[str] = None
    address: Optional[str] = None
    snils: Optional[str] = None
    passport: Optional[str] = None
    dms_oms: Optional[str] = None

class ClientOut(ClientIn):
    id: int
    created_at: datetime
    updated_at: datetime

class DoctorIn(BaseModel):
    user_id: int
    clinic_id: Optional[int] = None
    profession: str
    info: Optional[str] = None
    is_confirmed: Optional[bool] = False
    rating: Optional[float] = 0.0
    experience: Optional[int] = None
    price: Optional[float] = None

class DoctorOut(DoctorIn):
    id: int
    created_at: datetime
    updated_at: datetime

class AdminIn(BaseModel):
    user_id: int
    clinic_id: int
    position: Optional[str] = None

class AdminOut(AdminIn):
    id: int
    created_at: datetime
    updated_at: datetime

# --- Appointment slots / appointments ---
AppointmentStatus = Literal['BOOKED','CANCELED','COMPLETED','NO_SHOW']

class SlotIn(BaseModel):
    doctor_id: int
    start_time: datetime
    end_time: datetime

class SlotOut(BaseModel):
    id: int
    doctor_id: int
    start_time: datetime
    end_time: datetime
    duration: int
    is_booked: bool
    created_at: datetime
    updated_at: datetime

class AppointmentIn(BaseModel):
    slot_id: int
    client_id: int
    comments: Optional[str] = None

class AppointmentOut(BaseModel):
    id: int
    slot_id: int
    client_id: int
    status: AppointmentStatus
    comments: Optional[str] = None
    created_at: datetime
    updated_at: datetime
    canceled_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None

# --- Medical records / documents ---
class MedicalRecordIn(BaseModel):
    client_id: int
    doctor_id: Optional[int] = None
    appointment_id: Optional[int] = None
    diagnosis: Optional[str] = None
    symptoms: Optional[str] = None
    treatment: Optional[str] = None
    recommendations: Optional[str] = None

class MedicalRecordOut(MedicalRecordIn):
    id: int
    created_at: datetime
    updated_at: datetime

class MedicalDocumentIn(BaseModel):
    record_id: int
    client_id: int
    filename: Optional[str] = None
    file_url: Optional[str] = None
    file_type: Optional[str] = None
    encrypted: Optional[bool] = True

class MedicalDocumentOut(MedicalDocumentIn):
    id: int
    uploaded_at: datetime

# --- Doctor reviews ---
class DoctorReviewIn(BaseModel):
    doctor_id: int
    client_id: int
    rating: float
    comment: Optional[str] = None

class DoctorReviewOut(DoctorReviewIn):
    id: int
    created_at: datetime
    updated_at: datetime
    

