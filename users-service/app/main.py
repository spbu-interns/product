from typing import List, Optional
from fastapi import FastAPI, HTTPException, Query, BackgroundTasks
from sqlalchemy.exc import IntegrityError
from psycopg2 import errors as pgerr
from .db import get_session
from .models import (
    UserIn, UserOut,
    EmailStartVerificationIn, EmailVerifyIn,
    PasswordForgotIn, PasswordResetIn, RegistrationIn,
    LoginIn, ApiLoginResponse,
    ComplaintIn, ComplaintOut, ComplaintPatch,
    NoteIn, NoteOut, NotePatch,
    UserProfilePatch,
    SpecializationOut, DoctorSearchOut, Gender,
)
from . import repository as repo
from .repository import RESET_TOKEN_TTL_MIN
from passlib.hash import bcrypt
from datetime import date

app = FastAPI(title="Users DB API")

@app.get("/health")
def health():
    return {"status": "ok"}

@app.get("/specializations", response_model=List[SpecializationOut])
def api_list_specializations(popular_only: bool = Query(False)):
    """
    popular_only=true -> только популярные,
    popular_only=false -> все, популярные первыми.
    """
    s = get_session()
    try:
        return repo.list_specializations(s, popular_only=popular_only)
    finally:
        s.close()

@app.get("/users", response_model=List[UserOut])
def get_users(role: Optional[str] = Query(None)):
    s = get_session()
    try:
        return repo.list_users(s, role)
    finally:
        s.close()

@app.patch("/users/{user_id}/profile", response_model=UserOut)
def patch_user_profile(user_id: int, p: UserProfilePatch):
    s = get_session()
    try:
        try:
            r = repo.update_user_profile(s, user_id, p)
            if not r:
                # либо не найден, либо нечего обновлять
                raise HTTPException(404, "user not found or nothing to update")
            return r
        except IntegrityError as e:
            s.rollback()
            raise _map_integrity(e)
    finally:
        s.close()
        
# --- User profile (read) ---
@app.get("/users/{user_id}/profile", response_model=UserOut)
def api_get_user_profile(user_id: int):
    s = get_session()
    try:
        u = repo.get_user_profile(s, user_id)
        if not u:
            raise HTTPException(404, "user not found")
        return u
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
        except IntegrityError as e:
            s.rollback()
            raise _map_integrity(e)
        except ValueError as ve:
            s.rollback()
            raise HTTPException(status_code=400, detail=str(ve))
    finally:
        s.close()

@app.post("/register", response_model=UserOut, status_code=201)
def register(reg: RegistrationIn):
    s = get_session()
    try:
        try:
            return repo.register_user_with_role(s, reg)
        except IntegrityError as e:
            s.rollback()
            raise _map_integrity(e)
        except ValueError as ve:
            s.rollback()
            raise HTTPException(status_code=400, detail=str(ve))
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

@app.post("/auth/email/start")
def start_email_verification(body: EmailStartVerificationIn, bt: BackgroundTasks):
    s = get_session()
    try:
        uid = repo.find_user_id_by_email(s, body.email)
        if not uid:
            raise HTTPException(404, "user not found")
        raw_token = repo.start_email_verification(s, uid)

        return {"token": raw_token, "message": "verification link sent to email"}
    finally:
        s.close()

@app.post("/auth/email/verify")
def verify_email(body: EmailVerifyIn):
    s = get_session()
    try:
        ok = repo.verify_email_token(s, body.token)
        if not ok:
            raise HTTPException(400, "invalid or expired token")
        return {"status": "verified"}
    finally:
        s.close()

@app.get("/auth/email/verify")
def verify_email_by_link(token: str):
    s = get_session()
    try:
        ok = repo.verify_email_token(s, token)
        if not ok:
            raise HTTPException(400, "invalid or expired token")
        return {"status": "verified"}
    finally:
        s.close()

# == Password reset ==
@app.post("/auth/password/forgot")
def password_forgot(body: PasswordForgotIn, bt: BackgroundTasks):
    s = get_session()
    try:
        uid = repo.find_user_id_by_email(s, body.email)
        if not uid:
            # чтобы не палить существование пользователя, можно всегда возвращать 200
            raise HTTPException(404, "user not found")

        raw = repo.start_password_reset(s, uid)

        return {"token": raw, "message": "reset link sent to email"}
    finally:
        s.close()

@app.post("/auth/password/reset")
def password_reset(body: PasswordResetIn):
    s = get_session()
    try:
        ok = repo.consume_password_reset(s, body.token, body.new_password)
        if not ok:
            raise HTTPException(400, "invalid or expired token")
        return {"status": "password_changed"}
    finally:
        s.close()

def _map_integrity(e: IntegrityError) -> HTTPException:
    orig = getattr(e, "orig", None)
    if isinstance(orig, pgerr.UniqueViolation):
        return HTTPException(status_code=409, detail="email or login already exists")
    if isinstance(orig, pgerr.ForeignKeyViolation):
        return HTTPException(status_code=400, detail="clinic_id does not exist")
    if isinstance(orig, pgerr.CheckViolation):
        return HTTPException(status_code=400, detail="check constraint failed")
    if isinstance(orig, pgerr.NotNullViolation):
        return HTTPException(status_code=400, detail="missing required field")
    # по умолчанию
    return HTTPException(status_code=400, detail="integrity error")

# ===== Complaints =====

@app.post("/patients/{patient_id}/complaints", response_model=ComplaintOut, status_code=201)
def create_complaint(patient_id: int, c: ComplaintIn):
    s = get_session()
    try:
        # (опционально) можно проверить, что patient_id существует и это CLIENT
        r = repo.create_complaint(s, patient_id, c)
        return r
    finally:
        s.close()

@app.get("/patients/{patient_id}/complaints", response_model=List[ComplaintOut])
def list_complaints(patient_id: int, status: Optional[str] = Query(None)):
    s = get_session()
    try:
        return repo.list_complaints(s, patient_id, status)
    finally:
        s.close()

@app.patch("/complaints/{complaint_id}", response_model=ComplaintOut)
def patch_complaint(complaint_id: int, p: ComplaintPatch):
    s = get_session()
    try:
        r = repo.patch_complaint(s, complaint_id, p)
        if not r:
            raise HTTPException(404, "complaint not found or nothing to update")
        return r
    finally:
        s.close()

@app.delete("/complaints/{complaint_id}", status_code=204)
def delete_complaint(complaint_id: int):
    s = get_session()
    try:
        ok = repo.delete_complaint(s, complaint_id)
        if not ok:
            raise HTTPException(404, "complaint not found")
        return
    finally:
        s.close()

# ===== Doctor Notes =====

@app.post("/patients/{patient_id}/notes", response_model=NoteOut, status_code=201)
def create_note(patient_id: int, n: NoteIn):
    s = get_session()
    try:
        # (опционально) проверить, что doctor_id существует и role == DOCTOR
        r = repo.create_note(s, patient_id, n)
        return r
    finally:
        s.close()

@app.get("/patients/{patient_id}/notes", response_model=List[NoteOut])
def list_notes(patient_id: int, include_internal: bool = Query(True)):
    s = get_session()
    try:
        return repo.list_notes(s, patient_id, include_internal)
    finally:
        s.close()

@app.patch("/notes/{note_id}", response_model=NoteOut)
def patch_note(note_id: int, p: NotePatch):
    s = get_session()
    try:
        r = repo.patch_note(s, note_id, p)
        if not r:
            raise HTTPException(404, "note not found or nothing to update")
        return r
    finally:
        s.close()

@app.delete("/notes/{note_id}", status_code=204)
def delete_note(note_id: int):
    s = get_session()
    try:
        ok = repo.delete_note(s, note_id)
        if not ok:
            raise HTTPException(404, "note not found")
        return
    finally:
        s.close()
        
        
# ===== NEW: Domain v2 routes =====
from .models import (
    ClientIn, ClientOut,
    DoctorIn, DoctorOut,
    AdminIn, AdminOut,
    SlotIn, SlotOut,
    AppointmentIn, AppointmentOut,
    MedicalRecordIn, MedicalRecordOut,
    MedicalDocumentIn, MedicalDocumentOut,
    DoctorReviewIn, DoctorReviewOut,
    ClientPatch, DoctorPatch, AdminPatch,
)

# --- Clients ---
@app.post("/clients", response_model=ClientOut, status_code=201)
def api_create_client(body: ClientIn):
    s = get_session()
    try:
        return repo.create_client(s, body)
    finally:
        s.close()

@app.get("/clients/by-user/{user_id}", response_model=ClientOut | None)
def api_get_client_by_user(user_id: int):
    s = get_session()
    try:
        return repo.get_client_by_user_id(s, user_id)
    finally:
        s.close()

# --- Doctors ---
@app.post("/doctors", response_model=DoctorOut, status_code=201)
def api_create_doctor(body: DoctorIn):
    s = get_session()
    try:
        return repo.create_doctor(s, body)
    finally:
        s.close()

@app.get("/doctors/by-user/{user_id}", response_model=DoctorOut | None)
def api_get_doctor_by_user(user_id: int):
    s = get_session()
    try:
        return repo.get_doctor_by_user_id(s, user_id)
    finally:
        s.close()
        
@app.get("/doctors/{doctor_id}/available-dates", response_model=List[date])
def api_get_doctor_available_dates(doctor_id: int):
    """
    Доступные даты для календаря врача:
    только те дни, когда есть хотя бы один свободный слот.
    """
    s = get_session()
    try:
        return repo.list_available_dates_for_doctor(s, doctor_id)
    finally:
        s.close()

# --- Client complaints (совместимость: принимаем patient_user_id) ---
@app.post("/v2/patients/{patient_user_id}/complaints", status_code=201)
def api_create_client_complaint(patient_user_id: int, c: ComplaintIn):
    s = get_session()
    try:
        r = repo.create_client_complaint_by_user(s, patient_user_id, c)
        if not r:
            raise HTTPException(404, "client not found for given user")
        return r
    finally:
        s.close()

@app.get("/v2/patients/{patient_user_id}/complaints")
def api_list_client_complaints(patient_user_id: int):
    s = get_session()
    try:
        return repo.list_client_complaints_by_user(s, patient_user_id)
    finally:
        s.close()

# --- Slots ---
@app.post("/doctors/{doctor_id}/slots", response_model=SlotOut, status_code=201)
def api_create_slot(doctor_id: int, body: SlotIn):
    if body.doctor_id != doctor_id:
        raise HTTPException(400, "doctor_id mismatch")
    s = get_session()
    try:
        return repo.create_slot(s, body)
    finally:
        s.close()

@app.get("/doctors/{doctor_id}/slots", response_model=List[SlotOut])
def api_list_slots(doctor_id: int):
    s = get_session()
    try:
        return repo.list_slots_for_doctor(s, doctor_id)
    finally:
        s.close()

# --- Appointments ---
@app.post("/appointments", response_model=AppointmentOut, status_code=201)
def api_book_appointment(body: AppointmentIn):
    s = get_session()
    try:
        r = repo.book_appointment(s, body)
        if not r:
            raise HTTPException(400, "slot not available")
        return r
    finally:
        s.close()

@app.get("/clients/{client_id}/appointments", response_model=List[AppointmentOut])
def api_list_appointments_for_client(client_id: int):
    s = get_session()
    try:
        return repo.list_appointments_for_client(s, client_id)
    finally:
        s.close()

# --- Medical records / documents ---
@app.post("/records", response_model=MedicalRecordOut, status_code=201)
def api_create_medical_record(body: MedicalRecordIn):
    s = get_session()
    try:
        return repo.create_medical_record(s, body)
    finally:
        s.close()

@app.post("/records/{record_id}/documents", response_model=MedicalDocumentOut, status_code=201)
def api_add_medical_document(record_id: int, body: MedicalDocumentIn):
    if body.record_id != record_id:
        raise HTTPException(400, "record_id mismatch")
    s = get_session()
    try:
        return repo.add_medical_document(s, body)
    finally:
        s.close()

# --- Doctor reviews ---
@app.post("/doctors/{doctor_id}/reviews", response_model=DoctorReviewOut, status_code=201)
def api_create_review(doctor_id: int, body: DoctorReviewIn):
    if body.doctor_id != doctor_id:
        raise HTTPException(400, "doctor_id mismatch")
    s = get_session()
    try:
        r = repo.create_doctor_review(s, body)
        if not r:
            raise HTTPException(400, "duplicate review or invalid ref")
        return r
    finally:
        s.close()

@app.get("/doctors/{doctor_id}/reviews", response_model=List[DoctorReviewOut])
def api_list_reviews(doctor_id: int):
    s = get_session()
    try:
        return repo.list_doctor_reviews(s, doctor_id)
    finally:
        s.close()


@app.get("/admins/by-user/{user_id}", response_model=AdminOut | None)
def api_get_admin_by_user(user_id: int):
    s = get_session()
    try:
        return repo.get_admin_by_user_id(s, user_id)
    finally:
        s.close()
        
# --- PATCH Clients by user_id ---
@app.patch("/clients/by-user/{user_id}", response_model=ClientOut)
def api_patch_client_by_user(user_id: int, p: ClientPatch):
    s = get_session()
    try:
        r = repo.patch_client_by_user_id(s, user_id, p)
        if not r:
            raise HTTPException(404, "client not found or nothing to update")
        return r
    finally:
        s.close()

# --- PATCH Doctors by user_id ---
@app.patch("/doctors/by-user/{user_id}", response_model=DoctorOut)
def api_patch_doctor_by_user(user_id: int, p: DoctorPatch):
    s = get_session()
    try:
        r = repo.patch_doctor_by_user_id(s, user_id, p)
        if not r:
            raise HTTPException(404, "doctor not found or nothing to update")
        return r
    finally:
        s.close()

# --- PATCH Admins by user_id ---
@app.patch("/admins/by-user/{user_id}", response_model=AdminOut)
def api_patch_admin_by_user(user_id: int, p: AdminPatch):
    s = get_session()
    try:
        r = repo.patch_admin_by_user_id(s, user_id, p)
        if not r:
            raise HTTPException(404, "admin not found or nothing to update")
        return r
    finally:
        s.close()
        
@app.get("/doctors/search", response_model=List[DoctorSearchOut])
def api_search_doctors(
    specialization_ids: Optional[List[int]] = Query(None),
    city: Optional[str] = Query(None),
    region: Optional[str] = Query(None),
    metro: Optional[str] = Query(None),
    online_only: bool = Query(False),

    min_price: Optional[float] = Query(None),
    max_price: Optional[float] = Query(None),
    min_rating: Optional[float] = Query(None),

    gender: Optional[Gender] = Query(None),
    min_age: Optional[int] = Query(None),
    max_age: Optional[int] = Query(None),

    min_experience: Optional[int] = Query(None),
    max_experience: Optional[int] = Query(None),

    date_filter: Optional[date] = Query(None, alias="date"),
):
    """
    Поиск врачей с фильтрами из ТЗ.
    - specialization_ids: список id специализаций
    - city/region/metro: локация клиники
    - online_only: только онлайн-консультации
    - min/max_price: диапазон цены
    - min_rating: минимальный рейтинг
    - gender: пол врача
    - min/max_age: возраст врача
    - min/max_experience: стаж
    - date: наличие свободного слота в указанный день
    """
    s = get_session()
    try:
        return repo.search_doctors(
            s,
            specialization_ids=specialization_ids,
            city=city,
            region=region,
            metro=metro,
            online_only=online_only,
            min_price=min_price,
            max_price=max_price,
            min_rating=min_rating,
            gender=gender,
            min_age=min_age,
            max_age=max_age,
            min_experience=min_experience,
            max_experience=max_experience,
            date_filter=date_filter,
        )
    finally:
        s.close()