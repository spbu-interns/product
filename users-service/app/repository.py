from typing import Optional, List, Dict
from sqlalchemy import text
from sqlalchemy.orm import Session
from passlib.hash import bcrypt
from .models import UserIn, RegistrationIn
from sqlalchemy import or_
from datetime import datetime, timedelta
import secrets, hashlib


EMAIL_TOKEN_TTL_MIN = 30
RESET_TOKEN_TTL_MIN = 30

# В НАЧАЛЕ ФАЙЛА (после импортов) — общий список колонок, чтобы не дублировать:
_USER_COLS = """
id,email,login,role,
first_name,last_name,patronymic,phone_number,clinic_id,
name,surname,date_of_birth,avatar,gender,
is_active,email_verified_at,password_changed_at,created_at,updated_at
"""

def _rowmap(r) -> Dict:
    return dict(r) if r else None

def find_by_email(s: Session, email: str) -> Optional[Dict]:
    r = s.execute(text(f"""
        select {_USER_COLS}
        from users where email=:e
    """), {"e": email}).mappings().first()
    return _rowmap(r)

def find_by_login(s: Session, login: str) -> Optional[Dict]:
    r = s.execute(text(f"""
        select {_USER_COLS}
        from users where login=:l
    """), {"l": login}).mappings().first()
    return _rowmap(r)

def exists_by_email(s: Session, email: str) -> bool:
    return bool(s.execute(text("select 1 from users where email=:e limit 1"),
                          {"e": email}).first())

def exists_by_login(s: Session, login: str) -> bool:
    return bool(s.execute(text("select 1 from users where login=:l limit 1"),
                          {"l": login}).first())

def insert_user(s: Session, user) -> Dict:
    if user.role not in ("CLIENT", "DOCTOR", "ADMIN"):
        raise ValueError("invalid role")
    pwd_hash = bcrypt.hash(user.password)
    r = s.execute(text(f"""
        insert into users (email,login,password_hash,role,
                           first_name,last_name,patronymic,phone_number,
                           clinic_id,is_active)
        values (:e,:l,:p,:r,:fn,:ln,:pn,:ph,:cid,:ia)
        returning {_USER_COLS}
    """), {
        "e": user.email, "l": user.login, "p": pwd_hash, "r": user.role,
        "fn": user.first_name, "ln": user.last_name, "pn": user.patronymic,
        "ph": user.phone_number, "cid": user.clinic_id,
        "ia": True if user.is_active is None else user.is_active,
    }).mappings().first()
    s.commit()
    return dict(r)

def register_user(s: Session, reg: RegistrationIn) -> Dict:
    pwd_hash = bcrypt.hash(reg.password)
    r = s.execute(text(f"""
        insert into users (email,login,password_hash,is_active)
        values (:e,:l,:p,:ia)
        returning {_USER_COLS}
    """), {
        "e": reg.email,
        "l": reg.username,
        "p": pwd_hash,
        "ia": True if reg.is_active is None else reg.is_active,
    }).mappings().first()
    s.commit()
    return dict(r)

def list_users(s: Session, role: Optional[str]=None) -> List[Dict]:
    if role:
        rows = s.execute(text(f"""
            select {_USER_COLS}
            from users where role=:r order by id
        """), {"r": role}).mappings().all()
    else:
        rows = s.execute(text(f"""
            select {_USER_COLS}
            from users order by id
        """)).mappings().all()
    return [dict(r) for r in rows]


def update_user_profile(s: Session, user_id: int, p) -> Optional[Dict]:
    sets = []
    params = {"id": user_id}

    # совместимостьные поля
    if p.first_name is not None:
        sets.append("first_name = :first_name"); params["first_name"] = p.first_name
    if p.last_name is not None:
        sets.append("last_name = :last_name"); params["last_name"] = p.last_name
    if p.patronymic is not None:
        sets.append("patronymic = :patronymic"); params["patronymic"] = p.patronymic
    if p.phone_number is not None:
        sets.append("phone_number = :phone_number"); params["phone_number"] = p.phone_number
    if p.clinic_id is not None:
        sets.append("clinic_id = :clinic_id"); params["clinic_id"] = p.clinic_id

    # НОВЫЕ поля профиля
    if p.name is not None:
        sets.append("name = :name"); params["name"] = p.name
    if p.surname is not None:
        sets.append("surname = :surname"); params["surname"] = p.surname
    if p.date_of_birth is not None:
        sets.append("date_of_birth = :dob"); params["dob"] = p.date_of_birth
    if p.avatar is not None:
        sets.append("avatar = :avatar"); params["avatar"] = p.avatar
    if p.gender is not None:
        sets.append("gender = :gender"); params["gender"] = p.gender

    if not sets:
        return None

    sql = f"""
        update users
        set {', '.join(sets)}
        where id = :id
        returning {_USER_COLS}
    """
    r = s.execute(text(sql), params).mappings().first()
    s.commit()
    return dict(r) if r else None

def find_auth_by_login_or_email(s: Session, v: str):
    r = s.execute(text("""
        select id, role, password_hash, is_active
        from users
        where login = :v or email = :v
        limit 1
    """), {"v": v}).mappings().first()
    return dict(r) if r else None

def _gen_token_and_hash() -> tuple[str, str]:
    raw = secrets.token_urlsafe(32)   # отдать пользователю
    h = hashlib.sha256(raw.encode("utf-8")).hexdigest()  # хранить в БД
    return raw, h

def start_email_verification(s: Session, user_id: int) -> str:
    raw, h = _gen_token_and_hash()
    expires = datetime.utcnow() + timedelta(minutes=EMAIL_TOKEN_TTL_MIN)

    # позволяем 1 активный (непогашенный) токен на пользователя
    s.execute(text("delete from email_verifications where user_id=:u and consumed_at is null"),
              {"u": user_id})
    s.execute(text("""
        insert into email_verifications(user_id, token_hash, expires_at)
        values (:u, :h, :e)
    """), {"u": user_id, "h": h, "e": expires})
    s.commit()
    return raw  # вернуть сырой токен (его будешь высылать по почте)

def verify_email_token(s: Session, raw_token: str) -> bool:
    h = hashlib.sha256(raw_token.encode("utf-8")).hexdigest()
    row = s.execute(text("""
        select id, user_id, expires_at, consumed_at
        from email_verifications
        where token_hash=:h
        limit 1
    """), {"h": h}).mappings().first()
    if not row:
        return False
    if row["consumed_at"] is not None or row["expires_at"] <= datetime.utcnow():
        return False

    # помечаем токен и ставим флаг пользователю
    s.execute(text("update email_verifications set consumed_at=now() where id=:id"),
              {"id": row["id"]})
    s.execute(text("update users set email_verified_at=now() where id=:u"),
              {"u": row["user_id"]})
    s.commit()
    return True

# --- password reset tokens ---

def start_password_reset(s: Session, user_id: int) -> str:
    raw, h = _gen_token_and_hash()
    expires = datetime.utcnow() + timedelta(minutes=RESET_TOKEN_TTL_MIN)
    s.execute(text("delete from password_reset_tokens where user_id=:u and consumed_at is null"),
              {"u": user_id})
    s.execute(text("""
        insert into password_reset_tokens(user_id, token_hash, expires_at)
        values (:u, :h, :e)
    """), {"u": user_id, "h": h, "e": expires})
    s.commit()
    return raw

def consume_password_reset(s: Session, raw_token: str, new_password: str) -> bool:
    h = hashlib.sha256(raw_token.encode("utf-8")).hexdigest()
    row = s.execute(text("""
        select id, user_id, expires_at, consumed_at
        from password_reset_tokens
        where token_hash=:h
        limit 1
    """), {"h": h}).mappings().first()
    if not row:
        return False
    if row["consumed_at"] is not None or row["expires_at"] <= datetime.utcnow():
        return False

    new_hash = bcrypt.hash(new_password)
    s.execute(text("update password_reset_tokens set consumed_at=now() where id=:id"),
              {"id": row["id"]})
    s.execute(text("""
        update users
        set password_hash=:ph, password_changed_at=now()
        where id=:u
    """), {"ph": new_hash, "u": row["user_id"]})
    s.commit()
    return True

def find_user_id_by_email(s: Session, email: str) -> Optional[int]:
    r = s.execute(text("select id from users where email=:e"), {"e": email}).first()
    return r[0] if r else None


# ---------- Complaints ----------

def create_complaint(s: Session, patient_id: int, c) -> Dict:
    r = s.execute(text("""
        insert into patient_complaints (patient_id, title, body)
        values (:pid, :t, :b)
        returning id, patient_id, title, body, status, created_at, updated_at
    """), {"pid": patient_id, "t": c.title, "b": c.body}).mappings().first()
    s.commit()
    return dict(r)

def list_complaints(s: Session, patient_id: int, status: Optional[str] = None) -> List[Dict]:
    if status:
        rows = s.execute(text("""
            select id, patient_id, title, body, status, created_at, updated_at
            from patient_complaints
            where patient_id = :pid and status = :st
            order by id desc
        """), {"pid": patient_id, "st": status}).mappings().all()
    else:
        rows = s.execute(text("""
            select id, patient_id, title, body, status, created_at, updated_at
            from patient_complaints
            where patient_id = :pid
            order by id desc
        """), {"pid": patient_id}).mappings().all()
    return [dict(r) for r in rows]

def patch_complaint(s: Session, complaint_id: int, p) -> Optional[Dict]:
    sets = []
    params = {"id": complaint_id}
    if p.title is not None:
        sets.append("title = :t")
        params["t"] = p.title
    if p.body is not None:
        sets.append("body = :b")
        params["b"] = p.body
    if p.status is not None:
        sets.append("status = :st")
        params["st"] = p.status
    if not sets:
        return None
    sql = f"update patient_complaints set {', '.join(sets)} where id = :id returning id, patient_id, title, body, status, created_at, updated_at"
    r = s.execute(text(sql), params).mappings().first()
    s.commit()
    return dict(r) if r else None

def delete_complaint(s: Session, complaint_id: int) -> bool:
    r = s.execute(text("delete from patient_complaints where id = :id"), {"id": complaint_id})
    s.commit()
    return r.rowcount > 0

# ---------- Doctor Notes ----------

def create_note(s: Session, patient_id: int, n) -> Dict:
    r = s.execute(text("""
        insert into doctor_notes (patient_id, doctor_id, note, visibility)
        values (:pid, :did, :nt, :vis)
        returning id, patient_id, doctor_id, note, visibility, created_at, updated_at
    """), {"pid": patient_id, "did": n.doctor_id, "nt": n.note, "vis": n.visibility}).mappings().first()
    s.commit()
    return dict(r)

def list_notes(s: Session, patient_id: int, include_internal: bool = True) -> List[Dict]:
    if include_internal:
        rows = s.execute(text("""
            select id, patient_id, doctor_id, note, visibility, created_at, updated_at
            from doctor_notes
            where patient_id = :pid
            order by id desc
        """), {"pid": patient_id}).mappings().all()
    else:
        rows = s.execute(text("""
            select id, patient_id, doctor_id, note, visibility, created_at, updated_at
            from doctor_notes
            where patient_id = :pid and visibility = 'PATIENT'
            order by id desc
        """), {"pid": patient_id}).mappings().all()
    return [dict(r) for r in rows]

def patch_note(s: Session, note_id: int, p) -> Optional[Dict]:
    sets = []
    params = {"id": note_id}
    if p.note is not None:
        sets.append("note = :nt")
        params["nt"] = p.note
    if p.visibility is not None:
        sets.append("visibility = :vis")
        params["vis"] = p.visibility
    if not sets:
        return None
    sql = f"update doctor_notes set {', '.join(sets)} where id = :id returning id, patient_id, doctor_id, note, visibility, created_at, updated_at"
    r = s.execute(text(sql), params).mappings().first()
    s.commit()
    return dict(r) if r else None

def delete_note(s: Session, note_id: int) -> bool:
    r = s.execute(text("delete from doctor_notes where id = :id"), {"id": note_id})
    s.commit()
    return r.rowcount > 0


# ===== Helpers: map user_id -> client_id =====
def _client_id_by_user_id(s: Session, user_id: int) -> Optional[int]:
    r = s.execute(text("select id from clients where user_id=:u"), {"u": user_id}).first()
    return r[0] if r else None

# ===== Clients =====
def create_client(s: Session, body) -> Dict:
    r = s.execute(text("""
        insert into clients(user_id, blood_type, height, weight,
                            emergency_contact_name, emergency_contact_number,
                            address, snils, passport, dms_oms)
        values (:uid,:bt,:h,:w,:ecn,:ecn2,:addr,:snils,:pass,:dms)
        returning *
    """), {
        "uid": body.user_id, "bt": body.blood_type, "h": body.height, "w": body.weight,
        "ecn": body.emergency_contact_name, "ecn2": body.emergency_contact_number,
        "addr": body.address, "snils": body.snils, "pass": body.passport, "dms": body.dms_oms
    }).mappings().first()
    s.commit()
    return dict(r)

def get_client_by_user_id(s: Session, user_id: int) -> Optional[Dict]:
    r = s.execute(text("select * from clients where user_id=:u"), {"u": user_id}).mappings().first()
    return dict(r) if r else None

# ===== Doctors =====
def create_doctor(s: Session, body) -> Dict:
    r = s.execute(text("""
        insert into doctors(user_id, clinic_id, profession, info, is_confirmed, rating, experience, price)
        values (:uid,:cid,:prof,:info,:conf,:rt,:exp,:price)
        returning *
    """), {
        "uid": body.user_id, "cid": body.clinic_id, "prof": body.profession, "info": body.info,
        "conf": body.is_confirmed, "rt": body.rating, "exp": body.experience, "price": body.price
    }).mappings().first()
    s.commit()
    return dict(r)

def get_doctor_by_user_id(s: Session, user_id: int) -> Optional[Dict]:
    r = s.execute(text("select * from doctors where user_id=:u"), {"u": user_id}).mappings().first()
    return dict(r) if r else None

# ===== Complaints (новая таблица: client_complaints) =====
def create_client_complaint_by_user(s: Session, patient_user_id: int, c) -> Optional[Dict]:
    cid = _client_id_by_user_id(s, patient_user_id)
    if cid is None:
        return None
    r = s.execute(text("""
        insert into client_complaints (client_id, title, description)
        values (:cid, :t, :d)
        returning *
    """), {"cid": cid, "t": c.title, "d": c.body}).mappings().first()
    s.commit()
    return dict(r)

def list_client_complaints_by_user(s: Session, patient_user_id: int) -> List[Dict]:
    cid = _client_id_by_user_id(s, patient_user_id)
    if cid is None:
        return []
    rows = s.execute(text("""
        select * from client_complaints
        where client_id=:cid
        order by id desc
    """), {"cid": cid}).mappings().all()
    return [dict(r) for r in rows]

# ===== Slots =====
def create_slot(s: Session, body) -> Dict:
    r = s.execute(text("""
        insert into appointment_slots(doctor_id, start_time, end_time)
        values (:did,:st,:et)
        returning *
    """), {"did": body.doctor_id, "st": body.start_time, "et": body.end_time}).mappings().first()
    s.commit()
    return dict(r)

def list_slots_for_doctor(s: Session, doctor_id: int) -> List[Dict]:
    rows = s.execute(text("""
        select * from appointment_slots where doctor_id=:d order by start_time
    """), {"d": doctor_id}).mappings().all()
    return [dict(r) for r in rows]

# ===== Appointments =====
def book_appointment(s: Session, body) -> Optional[Dict]:
    # простая защита: слот свободен?
    slot = s.execute(text("select is_booked from appointment_slots where id=:id"), {"id": body.slot_id}).first()
    if not slot or slot[0]:
        return None
    r = s.execute(text("""
        insert into appointments(slot_id, client_id, comments)
        values (:sid,:cid,:com)
        returning *
    """), {"sid": body.slot_id, "cid": body.client_id, "com": body.comments}).mappings().first()
    s.execute(text("update appointment_slots set is_booked=true where id=:id"), {"id": body.slot_id})
    s.commit()
    return dict(r)

def list_appointments_for_client(s: Session, client_id: int) -> List[Dict]:
    rows = s.execute(text("select * from appointments where client_id=:c order by id desc"),
                     {"c": client_id}).mappings().all()
    return [dict(r) for r in rows]

# ===== Medical records / documents =====
def create_medical_record(s: Session, body) -> Dict:
    r = s.execute(text("""
        insert into medical_records(client_id, doctor_id, appointment_id, diagnosis, symptoms, treatment, recommendations)
        values (:cid,:did,:aid,:dg,:sym,:tr,:rec)
        returning *
    """), {
        "cid": body.client_id, "did": body.doctor_id, "aid": body.appointment_id,
        "dg": body.diagnosis, "sym": body.symptoms, "tr": body.treatment, "rec": body.recommendations
    }).mappings().first()
    s.commit()
    return dict(r)

def add_medical_document(s: Session, body) -> Dict:
    r = s.execute(text("""
        insert into medical_documents(record_id, client_id, filename, file_url, file_type, encrypted)
        values (:rid,:cid,:fn,:url,:ft,:enc)
        returning *
    """), {
        "rid": body.record_id, "cid": body.client_id, "fn": body.filename,
        "url": body.file_url, "ft": body.file_type, "enc": body.encrypted
    }).mappings().first()
    s.commit()
    return dict(r)

# ===== Reviews =====
def create_doctor_review(s: Session, body) -> Optional[Dict]:
    try:
        r = s.execute(text("""
            insert into doctor_reviews(doctor_id, client_id, rating, comment)
            values (:did,:cid,:rt,:cmt)
            returning *
        """), {"did": body.doctor_id, "cid": body.client_id, "rt": body.rating, "cmt": body.comment}).mappings().first()
        s.commit()
        return dict(r)
    except Exception:
        s.rollback()
        return None

def list_doctor_reviews(s: Session, doctor_id: int) -> List[Dict]:
    rows = s.execute(text("select * from doctor_reviews where doctor_id=:d order by id desc"),
                     {"d": doctor_id}).mappings().all()
    return [dict(r) for r in rows]