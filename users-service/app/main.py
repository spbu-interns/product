from fastapi.middleware.cors import CORSMiddleware
from typing import List, Optional
from fastapi import FastAPI, HTTPException, Query, BackgroundTasks, UploadFile, File
from fastapi.responses import FileResponse, JSONResponse
from sqlalchemy.exc import IntegrityError
from psycopg2 import errors as pgerr
from .db import get_session
import os
import uuid
from pathlib import Path
from PIL import Image
import io
from . import models as m
from .models import (
    UserIn, UserOut,
    EmailStartVerificationIn, EmailVerifyIn,
    PasswordForgotIn, PasswordResetIn, RegistrationIn,
    LoginIn, ApiLoginResponse,
    ComplaintIn, ComplaintOut, ComplaintPatch,
    NoteIn, NoteOut, NotePatch,
    UserProfilePatch,
    SpecializationOut, DoctorSearchOut, Gender,
    DoctorPatientOut,
)
from . import repository as repo
from .repository import RESET_TOKEN_TTL_MIN
from passlib.hash import bcrypt
from datetime import date

app = FastAPI(title="Users DB API")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Настройки для аватарок
AVATARS_DIR = Path(__file__).parent.parent / "avatars"
AVATARS_DIR.mkdir(exist_ok=True)
ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".gif"}
MAX_FILE_SIZE = 5 * 1024 * 1024  # 5 MB
MAX_IMAGE_DIMENSION = 2048  # максимальный размер стороны

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

        # нет юзера, отключен, или неверный пароль
        if (not u) or (not u["is_active"]) or (not bcrypt.verify(req.password, u["password_hash"])):
            return ApiLoginResponse(
                success=False,
                error="invalid login or password",
            )

        # пароль ок, но email не подтверждён
        if u.get("email_verified_at") is None:
            return ApiLoginResponse(
                success=False,
                error="EMAIL_NOT_VERIFIED",
            )

        # всё ок
        return ApiLoginResponse(
            success=True,
            role=u["role"],
        )
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
def api_list_slots(
    doctor_id: int,
    date_filter: Optional[date] = Query(None, alias="date"),
):
    """
    Все слоты врача. Если передать ?date=YYYY-MM-DD — вернёт слоты только за этот день.
    """
    s = get_session()
    try:
        return repo.list_slots_for_doctor(s, doctor_id, date_filter)
    finally:
        s.close()
        
@app.delete("/doctors/{doctor_id}/slots/{slot_id}", status_code=204)
def api_delete_slot(doctor_id: int, slot_id: int):
    """
    Удалить слот врача. Нельзя удалить занятый слот.
    """
    s = get_session()
    try:
        ok = repo.delete_slot_for_doctor(s, doctor_id, slot_id)
        if not ok:
            # чтобы не палить детали, даём общее сообщение
            raise HTTPException(
                400,
                "slot not found, belongs to another doctor or is already booked",
            )
        return
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
        
@app.post("/appointments/{appointment_id}/cancel", status_code=204)
def api_cancel_appointment(appointment_id: int):
    """
    Клиент отменяет запись:
    - слот становится свободным
    - запись помечена как CANCELED
    """
    s = get_session()
    try:
        ok = repo.cancel_appointment(s, appointment_id)
        if not ok:
            raise HTTPException(404, "appointment not found")
        return
    finally:
        s.close()

@app.post("/appointments/{appointment_id}/complete", status_code=204)
def api_complete_appointment(appointment_id: int):
    """
    Завершить приём (инициировано врачом).
    """
    s = get_session()
    try:
        ok = repo.complete_appointment(s, appointment_id)
        if not ok:
            raise HTTPException(404, "appointment not found")
        return
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
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
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
            limit=limit,
            offset=offset,
        )
    finally:
        s.close()


@app.get("/clients/{client_id}/medical-records", response_model=List[MedicalRecordOut])
def api_list_medical_records_for_client(client_id: int):
    s = get_session()
    try:
        return repo.list_medical_records_for_client(s, client_id)
    finally:
        s.close()


@app.get("/doctors/{doctor_id}/appointments", response_model=List[AppointmentOut])
def api_list_appointments_for_doctor(doctor_id: int):
    s = get_session()
    try:
        return repo.list_appointments_for_doctor(s, doctor_id)
    finally:
        s.close()


@app.get("/doctors/{doctor_id}/patients", response_model=List[DoctorPatientOut])
def api_list_patients_for_doctor(doctor_id: int):
    s = get_session()
    try:
        return repo.list_patients_for_doctor(s, doctor_id)
    finally:
        s.close()


# ========== AVATAR ENDPOINTS ==========

@app.post("/users/{user_id}/avatar", status_code=201)
async def upload_avatar(user_id: int, file: UploadFile = File(...)):
    """
    Загрузить аватарку пользователя.
    Поддерживаемые форматы: JPG, JPEG, PNG, WebP, GIF
    Максимальный размер: 5 MB
    """
    # Проверка существования пользователя
    s = get_session()
    try:
        user = repo.get_user_profile(s, user_id)
        if not user:
            raise HTTPException(404, "user not found")
        
        # Проверка расширения файла
        file_ext = Path(file.filename or "").suffix.lower()
        if file_ext not in ALLOWED_EXTENSIONS:
            raise HTTPException(
                400, 
                f"Invalid file type. Allowed: {', '.join(ALLOWED_EXTENSIONS)}"
            )
        
        # Чтение файла
        contents = await file.read()
        
        # Проверка размера
        if len(contents) > MAX_FILE_SIZE:
            raise HTTPException(
                400, 
                f"File too large. Maximum size: {MAX_FILE_SIZE // 1024 // 1024} MB"
            )
        
        # Проверка, что это действительно изображение + оптимизация
        try:
            image = Image.open(io.BytesIO(contents))
            
            # Конвертация в RGB если нужно (для JPEG)
            if image.mode in ("RGBA", "LA", "P") and file_ext in [".jpg", ".jpeg"]:
                background = Image.new("RGB", image.size, (255, 255, 255))
                if image.mode == "P":
                    image = image.convert("RGBA")
                background.paste(image, mask=image.split()[-1] if image.mode == "RGBA" else None)
                image = background
            
            # Изменение размера если слишком большое
            if image.width > MAX_IMAGE_DIMENSION or image.height > MAX_IMAGE_DIMENSION:
                image.thumbnail((MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION), Image.Resampling.LANCZOS)
            
            # Сохранение оптимизированного изображения
            optimized = io.BytesIO()
            save_format = "JPEG" if file_ext in [".jpg", ".jpeg"] else image.format or "PNG"
            image.save(optimized, format=save_format, quality=85, optimize=True)
            contents = optimized.getvalue()
            
        except Exception as e:
            raise HTTPException(400, f"Invalid image file: {str(e)}")
        
        # Удаление старой аватарки
        old_avatar = user.get("avatar")
        if old_avatar and old_avatar.startswith("avatars/"):
            old_path = AVATARS_DIR.parent / old_avatar
            if old_path.exists():
                old_path.unlink()
        
        # Генерация уникального имени файла
        filename = f"user_{user_id}_{uuid.uuid4().hex[:8]}{file_ext}"
        file_path = AVATARS_DIR / filename
        
        # Сохранение файла
        with open(file_path, "wb") as f:
            f.write(contents)
        
        # Обновление пути в БД
        avatar_path = f"avatars/{filename}"
        patch = m.UserProfilePatch(avatar=avatar_path)
        repo.update_user_profile(s, user_id, patch)
        s.commit()
        
        return JSONResponse(
            status_code=201,
            content={
                "message": "avatar uploaded successfully",
                "avatar_url": f"/users/{user_id}/avatar",
                "filename": filename
            }
        )
    finally:
        s.close()


@app.get("/users/{user_id}/avatar")
def get_avatar(user_id: int):
    """
    Получить аватарку пользователя.
    Возвращает файл изображения или 404 если аватарка не установлена.
    """
    s = get_session()
    try:
        user = repo.get_user_profile(s, user_id)
        if not user:
            raise HTTPException(404, "user not found")
        
        avatar_path = user.get("avatar")
        if not avatar_path:
            raise HTTPException(404, "avatar not set")
        
        # Поддержка как относительных, так и полных путей
        if avatar_path.startswith("avatars/"):
            file_path = AVATARS_DIR.parent / avatar_path
        else:
            file_path = Path(avatar_path)
        
        if not file_path.exists():
            raise HTTPException(404, "avatar file not found")
        
        # Определение MIME-типа по расширению
        ext = file_path.suffix.lower()
        media_types = {
            ".jpg": "image/jpeg",
            ".jpeg": "image/jpeg",
            ".png": "image/png",
            ".webp": "image/webp",
            ".gif": "image/gif"
        }
        media_type = media_types.get(ext, "image/jpeg")
        
        return FileResponse(
            file_path,
            media_type=media_type,
            filename=file_path.name
        )
    finally:
        s.close()


@app.delete("/users/{user_id}/avatar", status_code=204)
def delete_avatar(user_id: int):
    """
    Удалить аватарку пользователя.
    """
    s = get_session()
    try:
        user = repo.get_user_profile(s, user_id)
        if not user:
            raise HTTPException(404, "user not found")
        
        avatar_path = user.get("avatar")
        if not avatar_path:
            return  # Уже нет аватарки
        
        # Удаление файла
        if avatar_path.startswith("avatars/"):
            file_path = AVATARS_DIR.parent / avatar_path
            if file_path.exists():
                file_path.unlink()
        
        # Очистка поля в БД
        patch = m.UserProfilePatch(avatar=None)
        repo.update_user_profile(s, user_id, patch)
        s.commit()
        
        return
    finally:
        s.close()