from typing import Optional, List, Dict
from sqlalchemy import text
from sqlalchemy.orm import Session
from passlib.hash import bcrypt
from .models import UserIn, RegistrationIn
from datetime import datetime, timedelta, date
import secrets, hashlib


EMAIL_TOKEN_TTL_MIN = 30
RESET_TOKEN_TTL_MIN = 30

# В НАЧАЛЕ ФАЙЛА (после импортов) — общий список колонок, чтобы не дублировать:
_USER_COLS = """
id,email,login,role,
patronymic,phone_number,clinic_id,
name,surname,date_of_birth,avatar,gender,
is_active,email_verified_at,password_changed_at,created_at,updated_at
"""

_DOCTOR_RATING_EXPR_TMPL = """
    coalesce((
        select avg(rating)::float
        from (
            select rating from appointment_reviews where doctor_id = {doctor_ref}
            union all
            select rating from doctor_reviews where doctor_id = {doctor_ref}
        ) r
    ), 0)
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
                           name,surname,patronymic,phone_number,
                           clinic_id,is_active)
        values (:e,:l,:p,:r,:name,:surname,:pn,:ph,:cid,:ia)
        returning {_USER_COLS}
    """), {
        "e": user.email, "l": user.login, "p": pwd_hash, "r": user.role,
        "name": user.name, "surname": user.surname, "pn": user.patronymic,
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
    incoming = p.model_dump(exclude_unset=True)

    # поля, реально существующие в users
    if "phone_number" in incoming:
        sets.append("phone_number = :phone_number"); params["phone_number"] = p.phone_number
    if "clinic_id" in incoming:
        sets.append("clinic_id = :clinic_id"); params["clinic_id"] = p.clinic_id

    if "name" in incoming:
        sets.append("name = :name"); params["name"] = p.name
    if "surname" in incoming:
        sets.append("surname = :surname"); params["surname"] = p.surname
    if "patronymic" in incoming:
        sets.append("patronymic = :patronymic"); params["patronymic"] = p.patronymic
    if "date_of_birth" in incoming:
        sets.append("date_of_birth = :dob"); params["dob"] = p.date_of_birth
    if "avatar" in incoming:
        sets.append("avatar = :avatar"); params["avatar"] = p.avatar
    if "gender" in incoming:
        sets.append("gender = :gender"); params["gender"] = p.gender

    if not sets:
        return None

    sets.append("updated_at = now()")

    # роль для зеркалирования clinic_id
    role_row = s.execute(text("select role from users where id = :id"), {"id": user_id}).first()
    role = role_row[0] if role_row else None

    r = s.execute(text(f"""
        update users
        set {', '.join(sets)}
        where id = :id
        returning {_USER_COLS}
    """), params).mappings().first()

    # --- синк clinic_id в doctors/admins (если прислали clinic_id) ---
    if p.clinic_id is not None:
        if role == "DOCTOR":
            s.execute(text("""
                update doctors
                set clinic_id = :cid, updated_at = now()
                where user_id = :uid
            """), {"cid": p.clinic_id, "uid": user_id})
        elif role == "ADMIN":
            s.execute(text("""
                update admins
                set clinic_id = :cid, updated_at = now()
                where user_id = :uid
            """), {"cid": p.clinic_id, "uid": user_id})

    s.commit()
    return dict(r) if r else None

# --- User profile by id ---
def get_user_profile(s: Session, user_id: int) -> Optional[Dict]:
    r = s.execute(text(f"""
        select {_USER_COLS}
        from users
        where id = :id
        limit 1
    """), {"id": user_id}).mappings().first()
    return dict(r) if r else None

def find_auth_by_login_or_email(s: Session, v: str):
    r = s.execute(text("""
        select id, role, password_hash, is_active, email_verified_at
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
def set_doctor_specializations(s: Session, doctor_id: int, specialization_ids: List[int]) -> None:
    """
    Перезаписывает список специализаций врача.
    """
    # удаляем старые связи
    s.execute(text("delete from doctor_specializations where doctor_id=:d"), {"d": doctor_id})
    # вставляем новые
    if specialization_ids:
        s.execute(
            text("""
                insert into doctor_specializations(doctor_id, specialization_id)
                values (:did, :sid)
            """),
            [{"did": doctor_id, "sid": sid} for sid in specialization_ids],
        )
        
def _get_doctor_with_specs_by_id(s: Session, doctor_id: int) -> Optional[Dict]:
    rating_expr = _DOCTOR_RATING_EXPR_TMPL.format(doctor_ref="d.id")
    r = s.execute(text(f"""
        select
            d.*,
            {rating_expr} as rating,
            coalesce(
                array(
                    select ds.specialization_id
                    from doctor_specializations ds
                    where ds.doctor_id = d.id
                    order by ds.specialization_id
                ),
                array[]::int[]
            ) as specialization_ids
        from doctors d
        where d.id = :id
        limit 1
    """), {"id": doctor_id}).mappings().first()
    return dict(r) if r else None


def _get_doctor_with_specs_by_user_id(s: Session, user_id: int) -> Optional[Dict]:
    rating_expr = _DOCTOR_RATING_EXPR_TMPL.format(doctor_ref="d.id")
    r = s.execute(text(f"""
        select
            d.*,
            {rating_expr} as rating,
            coalesce(
                array(
                    select ds.specialization_id
                    from doctor_specializations ds
                    where ds.doctor_id = d.id
                    order by ds.specialization_id
                ),
                array[]::int[]
            ) as specialization_ids
        from doctors d
        where d.user_id = :uid
        limit 1
    """), {"uid": user_id}).mappings().first()
    return dict(r) if r else None
        
def list_specializations(s: Session, popular_only: Optional[bool] = None) -> List[Dict]:
    if popular_only:
        rows = s.execute(text("""
            select id, name, is_popular, created_at
            from specializations
            where is_popular = true
            order by name
        """)).mappings().all()
    else:
        rows = s.execute(text("""
            select id, name, is_popular, created_at
            from specializations
            order by is_popular desc, name
        """)).mappings().all()
    return [dict(r) for r in rows]



def create_doctor(s: Session, body) -> Dict:
    base_rating = body.rating if body.rating is not None else 0.0
    r = s.execute(text("""
        insert into doctors(user_id, clinic_id, profession, info,
                            is_confirmed, rating, experience, price,
                            online_available)
        values (:uid,:cid,:prof,:info,:conf,:rt,:exp,:price,:online)
        returning id
    """), {
        "uid": body.user_id,
        "cid": body.clinic_id,
        "prof": body.profession,
        "info": body.info,
        "conf": body.is_confirmed,
        "rt": base_rating,
        "exp": body.experience,
        "price": body.price,
        "online": body.online_available if body.online_available is not None else False,
    }).mappings().first()

    doctor_id = r["id"]

    # специализации, если переданы
    spec_ids = getattr(body, "specialization_ids", None)
    if spec_ids:
        set_doctor_specializations(s, doctor_id, spec_ids)

    # теперь достаем врача с specialization_ids
    doctor = _get_doctor_with_specs_by_id(s, doctor_id)

    s.commit()
    return doctor

def get_doctor_by_user_id(s: Session, user_id: int) -> Optional[Dict]:
    return _get_doctor_with_specs_by_user_id(s, user_id)

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

def list_slots_for_doctor(
    s: Session,
    doctor_id: int,
    slot_date: Optional[date] = None,
) -> List[Dict]:
    """
    Все слоты врача, опционально отфильтрованные по дате (DATE(start_time)).
    """
    if slot_date is None:
        rows = s.execute(
            text("""
                select *
                from appointment_slots
                where doctor_id = :d
                order by start_time
            """),
            {"d": doctor_id},
        ).mappings().all()
    else:
        rows = s.execute(
            text("""
                select *
                from appointment_slots
                where doctor_id = :d
                  and date(start_time) = :dt
                order by start_time
            """),
            {"d": doctor_id, "dt": slot_date},
        ).mappings().all()

    return [dict(r) for r in rows]

# ===== Appointments =====
def book_appointment(s: Session, body) -> Optional[Dict]:
    # простая защита: слот свободен?
    slot = s.execute(
        text("select is_booked from appointment_slots where id=:id"),
        {"id": body.slot_id},
    ).first()
    if not slot or slot[0]:
        # нет такого слота или уже занят
        return None

    canceled = s.execute(
        text(
            """
            select id from appointments
            where slot_id = :sid and status = 'CANCELED'
            limit 1
            """
        ),
        {"sid": body.slot_id},
    ).mappings().first()

    if canceled:
        r = s.execute(
            text(
                """
                update appointments
                set client_id = :cid,
                    comments = :com,
                    appointment_type_id = :atype,
                    status = 'BOOKED',
                    canceled_at = null,
                    completed_at = null,
                    updated_at = now()
                where id = :aid
                returning *
                """
            ),
            {
                "aid": canceled["id"],
                "cid": body.client_id,
                "com": body.comments,
                "atype": getattr(body, "appointment_type_id", None),
            },
        ).mappings().first()
    else:
        r = s.execute(
            text("""
                insert into appointments(
                    slot_id,
                    client_id,
                    comments,
                    appointment_type_id
                )
                values (:sid, :cid, :com, :atype)
                returning *
            """),
            {
                "sid": body.slot_id,
                "cid": body.client_id,
                "com": body.comments,
                "atype": getattr(body, "appointment_type_id", None),
            },
        ).mappings().first()

    s.execute(
        text("update appointment_slots set is_booked=true where id=:id"),
        {"id": body.slot_id},
    )

    s.commit()
    return dict(r)

def list_appointments_for_client(s: Session, client_id: int) -> List[Dict]:
    rows = s.execute(text("select * from appointments where client_id=:c order by id desc"),
                     {"c": client_id}).mappings().all()
    return [dict(r) for r in rows]

def list_available_dates_for_doctor(s: Session, doctor_id: int) -> List[date]:
    """
    Список дат, в которые у врача есть хотя бы один свободный слот.
    """
    rows = s.execute(
        text("""
            select distinct date(start_time) as day
            from appointment_slots
            where doctor_id = :d
              and is_booked = false
            order by day
        """),
        {"d": doctor_id},
    ).all()
    return [r[0] for r in rows]

def cancel_appointment(s: Session, appointment_id: int) -> bool:
    """
    Отменить запись:
    - appointment.status -> 'CANCELED'
    - выставить canceled_at, updated_at
    - освободить слот (appointment_slots.is_booked = false)
    """
    row = s.execute(
        text("select slot_id from appointments where id = :id"),
        {"id": appointment_id},
    ).first()

    if not row:
        return False

    slot_id = row[0]

    s.execute(
        text("""
            update appointments
            set status = 'CANCELED',
                canceled_at = now(),
                updated_at = now()
            where id = :id
        """),
        {"id": appointment_id},
    )

    s.execute(
        text("""
            update appointment_slots
            set is_booked = false,
                updated_at = now()
            where id = :sid
        """),
        {"sid": slot_id},
    )

    s.commit()
    return True


def complete_appointment(s: Session, appointment_id: int) -> bool:
    """
    Пометить запись завершённой и зафиксировать время завершения.
    """
    row = s.execute(
        text("select id from appointments where id = :id"),
        {"id": appointment_id},
    ).first()

    if not row:
        return False

    s.execute(
        text(
            """
            update appointments
            set status = 'COMPLETED',
                completed_at = now(),
                updated_at = now()
            where id = :id
            """
        ),
        {"id": appointment_id},
    )

    s.commit()

    # Immediately record that we should ask the patient for a review
    try:
        create_review_invitation(s, appointment_id)
    except Exception:
        # уведомление не должно блокировать завершение приёма
        s.rollback()
        s.commit()
    return True


def create_review_invitation(s: Session, appointment_id: int):
    info = s.execute(
        text(
            """
            select a.id as appointment_id,
                   a.client_id,
                   slots.doctor_id,
                   u.email
            from appointments a
            join appointment_slots slots on slots.id = a.slot_id
            join clients c on c.id = a.client_id
            join users u on u.id = c.user_id
            where a.id = :aid
            """
        ),
        {"aid": appointment_id},
    ).mappings().first()

    if not info:
        return None

    s.execute(
        text(
            """
            insert into appointment_review_requests(appointment_id, client_id, doctor_id)
            values (:aid, :cid, :did)
            on conflict (appointment_id) do nothing
            """
        ),
        {
            "aid": info["appointment_id"],
            "cid": info["client_id"],
            "did": info["doctor_id"],
        },
    )
    s.commit()

    # эмуляция отправки письма со ссылкой на отзыв
    email = info.get("email")
    if email:
        print(f"[reviews] Отправляем письмо на {email} c ссылкой на отзыв по приёму {appointment_id}")
    return info


def upsert_appointment_review(s: Session, appointment_id: int, body) -> Optional[Dict]:
    appointment = s.execute(
        text(
            """
            select a.id, a.client_id, slots.doctor_id
            from appointments a
            join appointment_slots slots on slots.id = a.slot_id
            where a.id = :aid
            """
        ),
        {"aid": appointment_id},
    ).mappings().first()

    if not appointment:
        return None

    existing = s.execute(
        text("select id from appointment_reviews where appointment_id = :aid"),
        {"aid": appointment_id},
    ).first()

    if existing:
        r = s.execute(
            text(
                """
                update appointment_reviews
                set rating = :rt,
                    comment = :cmt,
                    updated_at = now()
                where appointment_id = :aid
                returning *
                """
            ),
            {"rt": body.rating, "cmt": body.comment, "aid": appointment_id},
        ).mappings().first()
    else:
        r = s.execute(
            text(
                """
                insert into appointment_reviews(appointment_id, doctor_id, client_id, rating, comment)
                values (:aid, :did, :cid, :rt, :cmt)
                returning *
                """
            ),
            {
                "aid": appointment_id,
                "did": appointment["doctor_id"],
                "cid": appointment["client_id"],
                "rt": body.rating,
                "cmt": body.comment,
            },
        ).mappings().first()

    _refresh_doctor_rating(s, appointment["doctor_id"])
    s.commit()
    return dict(r)


def get_appointment_review(s: Session, appointment_id: int) -> Optional[Dict]:
    r = s.execute(
        text("select * from appointment_reviews where appointment_id = :aid"),
        {"aid": appointment_id},
    ).mappings().first()
    return dict(r) if r else None


def list_appointments_with_reviews(s: Session, client_id: int) -> List[Dict]:
    rows = s.execute(
        text(
            """
            select a.id as appointment_id,
                   a.status,
                   slots.start_time as slot_start,
                   slots.end_time as slot_end,
                   slots.doctor_id,
                   u.name as doctor_name,
                   u.surname as doctor_surname,
                   u.patronymic as doctor_patronymic,
                   d.profession as doctor_profession,
                   r.id as review_id,
                   r.rating,
                   r.comment,
                   r.created_at as review_created_at,
                   r.updated_at as review_updated_at,
                   r.appointment_id as review_appointment_id,
                   r.doctor_id as review_doctor_id,
                   r.client_id as review_client_id
            from appointments a
            join appointment_slots slots on slots.id = a.slot_id
            left join doctors d on d.id = slots.doctor_id
            left join users u on u.id = d.user_id
            left join appointment_reviews r on r.appointment_id = a.id
            where a.client_id = :cid and a.status in ('BOOKED','COMPLETED','CANCELED','NO_SHOW')
            order by slots.start_time desc
            """
        ),
        {"cid": client_id},
    ).mappings().all()

    return [dict(r) for r in rows]


def list_pending_reviews(s: Session, client_id: int) -> List[Dict]:
    rows = s.execute(
        text(
            """
            select a.id as appointment_id,
                   a.status,
                   slots.start_time as slot_start,
                   slots.end_time as slot_end,
                   slots.doctor_id,
                   u.name as doctor_name,
                   u.surname as doctor_surname,
                   u.patronymic as doctor_patronymic,
                   d.profession as doctor_profession
            from appointments a
            join appointment_slots slots on slots.id = a.slot_id
            left join doctors d on d.id = slots.doctor_id
            left join users u on u.id = d.user_id
            left join appointment_reviews r on r.appointment_id = a.id
            where a.client_id = :cid and a.status = 'COMPLETED' and r.id is null
            order by slots.start_time desc
            """
        ),
        {"cid": client_id},
    ).mappings().all()

    return [dict(r) for r in rows]


def get_next_appointment_for_client(s: Session, client_id: int) -> Optional[Dict]:
    row = s.execute(
        text(
            """
            select a.id as appointment_id,
                   slots.start_time as slot_start,
                   slots.doctor_id,
                   u.name as doctor_name,
                   u.surname as doctor_surname,
                   u.patronymic as doctor_patronymic,
                   d.profession as doctor_profession
            from appointments a
            join appointment_slots slots on slots.id = a.slot_id
            left join doctors d on d.id = slots.doctor_id
            left join users u on u.id = d.user_id
            where a.client_id = :cid
              and a.status = 'BOOKED'
              and slots.start_time > now()
            order by slots.start_time asc
            limit 1
            """
        ),
        {"cid": client_id},
    ).mappings().first()

    return dict(row) if row else None


def delete_slot_for_doctor(s: Session, doctor_id: int, slot_id: int) -> bool:
    """
    Удалить слот врача, только если он:
    - существует
    - принадлежит этому doctor_id
    - не забронирован (is_booked = false)
    """
    row = s.execute(
        text("""
            select doctor_id, is_booked
            from appointment_slots
            where id = :id
        """),
        {"id": slot_id},
    ).first()

    if not row:
        # нет такого слота
        return False

    row_doctor_id, is_booked = row[0], row[1]

    if row_doctor_id != doctor_id:
        # слот другого врача
        return False

    if is_booked:
        # по ТЗ нельзя удалять занятый слот
        return False

    res = s.execute(
        text("delete from appointment_slots where id = :id"),
        {"id": slot_id},
    )
    s.commit()
    return res.rowcount > 0

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
def _refresh_doctor_rating(s: Session, doctor_id: int) -> float:
    """
    Пересчитывает рейтинг врача на основе всех доступных отзывов.
    Используем как отзывы после приёмов, так и прямые отзывы о врачах.
    """
    rating_expr = _DOCTOR_RATING_EXPR_TMPL.format(doctor_ref=":did")
    avg_row = s.execute(
        text(f"select {rating_expr} as avg_rating"),
        {"did": doctor_id},
    ).mappings().first()

    new_rating = (avg_row or {}).get("avg_rating")
    safe_rating = new_rating if new_rating is not None else 0

    s.execute(
        text("update doctors set rating = :rt, updated_at = now() where id = :did"),
        {"rt": safe_rating, "did": doctor_id},
    )

    return safe_rating


def create_doctor_review(s: Session, body) -> Optional[Dict]:
    try:
        r = s.execute(text("""
            insert into doctor_reviews(doctor_id, client_id, rating, comment)
            values (:did,:cid,:rt,:cmt)
            returning *
        """), {"did": body.doctor_id, "cid": body.client_id, "rt": body.rating, "cmt": body.comment}).mappings().first()
        _refresh_doctor_rating(s, body.doctor_id)
        s.commit()
        return dict(r)
    except Exception:
        s.rollback()
        return None

def list_doctor_reviews(s: Session, doctor_id: int) -> List[Dict]:
    rows = s.execute(text("select * from doctor_reviews where doctor_id=:d order by id desc"),
                     {"d": doctor_id}).mappings().all()
    return [dict(r) for r in rows]


# ===== New Registration =====
def create_admin(s: Session, body) -> Dict:
    r = s.execute(text("""
        insert into admins(user_id, clinic_id, position)
        values (:uid,:cid,:pos)
        returning *
    """), {"uid": body["user_id"], "cid": body["clinic_id"], "pos": body.get("position")}).mappings().first()
    return dict(r)

def register_user_with_role(s: Session, reg) -> Dict:
    # одна транзакция
    try:
        pwd_hash = bcrypt.hash(reg.password)
        user = s.execute(text(f"""
            insert into users (email,login,password_hash,is_active,role)
            values (:e,:l,:p,:ia,:role)
            returning {_USER_COLS}
        """), {
            "e": reg.email,
            "l": reg.username,
            "p": pwd_hash,
            "ia": True if reg.is_active is None else reg.is_active,
            "role": reg.role
        }).mappings().first()

        uid = user["id"]

        if reg.role == "CLIENT":
            data = (reg.client or type("X", (), {})())
            s.execute(text("""
              insert into clients(user_id, blood_type, height, weight,
                                  emergency_contact_name, emergency_contact_number,
                                  address, snils, passport, dms_oms)
              values (:uid,:bt,:h,:w,:ecn,:ecn2,:addr,:snils,:pass,:dms)
            """), {
              "uid": uid,
              "bt": getattr(data, "blood_type", None),
              "h": getattr(data, "height", None),
              "w": getattr(data, "weight", None),
              "ecn": getattr(data, "emergency_contact_name", None),
              "ecn2": getattr(data, "emergency_contact_number", None),
              "addr": getattr(data, "address", None),
              "snils": getattr(data, "snils", None),
              "pass": getattr(data, "passport", None),
              "dms": getattr(data, "dms_oms", None),
            })

        elif reg.role == "DOCTOR":
            if not reg.doctor or not reg.doctor.profession:
                raise ValueError("doctor.profession is required")
            doc = s.execute(text("""
              insert into doctors(user_id, clinic_id, profession, info,
                                  is_confirmed, rating, experience, price,
                                  online_available)
              values (:uid,:cid,:prof,:info,:conf,:rt,:exp,:price,:online)
              returning id
            """), {
              "uid": uid,
              "cid": reg.doctor.clinic_id,
              "prof": reg.doctor.profession,
              "info": reg.doctor.info,
              "conf": reg.doctor.is_confirmed or False,
              "rt": reg.doctor.rating or 0.0,
              "exp": reg.doctor.experience,
              "price": reg.doctor.price,
              "online": reg.doctor.online_available or False,
            }).mappings().first()

            # специализации при регистрации
            if reg.doctor.specialization_ids:
                set_doctor_specializations(s, doc["id"], reg.doctor.specialization_ids)

        elif reg.role == "ADMIN":
            if not reg.admin or not reg.admin.clinic_id:
                raise ValueError("admin.clinic_id is required")
            s.execute(text("""
              insert into admins(user_id, clinic_id, position)
              values (:uid,:cid,:pos)
            """), {
              "uid": uid,
              "cid": reg.admin.clinic_id,
              "pos": reg.admin.position
            })

        s.commit()
        return dict(user)

    except Exception:
        s.rollback()
        raise
    
def get_admin_by_user_id(s: Session, user_id: int) -> Optional[Dict]:
    r = s.execute(text("select * from admins where user_id=:u"), {"u": user_id}).mappings().first()
    return dict(r) if r else None

def patch_client_by_user_id(s: Session, user_id: int, p) -> Optional[Dict]:
    sets = []
    params = {"uid": user_id}

    if p.blood_type is not None:
        sets.append("blood_type = :blood_type"); params["blood_type"] = p.blood_type
    if p.height is not None:
        sets.append("height = :height"); params["height"] = p.height
    if p.weight is not None:
        sets.append("weight = :weight"); params["weight"] = p.weight
    if p.emergency_contact_name is not None:
        sets.append("emergency_contact_name = :ecn"); params["ecn"] = p.emergency_contact_name
    if p.emergency_contact_number is not None:
        sets.append("emergency_contact_number = :ecn2"); params["ecn2"] = p.emergency_contact_number
    if p.address is not None:
        sets.append("address = :addr"); params["addr"] = p.address
    if p.snils is not None:
        sets.append("snils = :snils"); params["snils"] = p.snils
    if p.passport is not None:
        sets.append("passport = :pass"); params["pass"] = p.passport
    if p.dms_oms is not None:
        sets.append("dms_oms = :dms"); params["dms"] = p.dms_oms

    if not sets:
        return None

    sets.append("updated_at = now()")
    sql = f"""
        update clients
        set {', '.join(sets)}
        where user_id = :uid
        returning *
    """
    r = s.execute(text(sql), params).mappings().first()
    s.commit()
    return dict(r) if r else None


def patch_doctor_by_user_id(s: Session, user_id: int, p) -> Optional[Dict]:
    sets = []
    params = {"uid": user_id}

    if p.clinic_id is not None:
        sets.append("clinic_id = :cid"); params["cid"] = p.clinic_id
    if p.profession is not None:
        sets.append("profession = :prof"); params["prof"] = p.profession
    if p.info is not None:
        sets.append("info = :info"); params["info"] = p.info
    if p.is_confirmed is not None:
        sets.append("is_confirmed = :conf"); params["conf"] = p.is_confirmed
    if p.rating is not None:
        sets.append("rating = :rt"); params["rt"] = p.rating
    if p.experience is not None:
        sets.append("experience = :exp"); params["exp"] = p.experience
    if p.price is not None:
        sets.append("price = :price"); params["price"] = p.price
    if p.online_available is not None:
        sets.append("online_available = :online")
        params["online"] = p.online_available

    if not sets:
        return None

    sets.append("updated_at = now()")
    sql = f"""
        update doctors
        set {', '.join(sets)}
        where user_id = :uid
        returning id
    """
    r = s.execute(text(sql), params).mappings().first()

    if not r:
        s.commit()
        return None

    doctor_id = r["id"]

    # обновляем специализации, если прислали
    if p.specialization_ids is not None:
        set_doctor_specializations(s, doctor_id, p.specialization_ids)

    # зеркалим clinic_id в users, если прислали
    if p.clinic_id is not None:
        s.execute(
            text("update users set clinic_id=:cid, updated_at=now() where id=:uid"),
            {"cid": p.clinic_id, "uid": user_id},
        )

    # достаем полную запись врача с specialization_ids
    doctor = _get_doctor_with_specs_by_id(s, doctor_id)

    s.commit()
    return doctor


def patch_admin_by_user_id(s: Session, user_id: int, p) -> Optional[Dict]:
    sets = []
    params = {"uid": user_id}

    if p.clinic_id is not None:
        sets.append("clinic_id = :cid"); params["cid"] = p.clinic_id
    if p.position is not None:
        sets.append("position = :pos"); params["pos"] = p.position

    if not sets:
        return None

    sets.append("updated_at = now()")
    sql = f"""
        update admins
        set {', '.join(sets)}
        where user_id = :uid
        returning *
    """
    r = s.execute(text(sql), params).mappings().first()

    # зеркалим clinic_id в users, если прислали
    if r and p.clinic_id is not None:
        s.execute(text("update users set clinic_id=:cid, updated_at=now() where id=:uid"),
                  {"cid": p.clinic_id, "uid": user_id})

    s.commit()
    return dict(r) if r else None

def search_doctors(
    s: Session,
    specialization_ids: Optional[List[int]] = None,
    city: Optional[str] = None,
    region: Optional[str] = None,
    metro: Optional[str] = None,
    online_only: bool = False,
    min_price: Optional[float] = None,
    max_price: Optional[float] = None,
    min_rating: Optional[float] = None,
    gender: Optional[str] = None,
    min_age: Optional[int] = None,
    max_age: Optional[int] = None,
    min_experience: Optional[int] = None,
    max_experience: Optional[int] = None,
    date_filter: Optional[date] = None,
    limit: int = 50,
    offset: int = 0,
) -> List[Dict]:
    safe_limit = 50 if limit is None or limit <= 0 else limit
    safe_offset = 0 if offset is None or offset < 0 else offset
    rating_expr = _DOCTOR_RATING_EXPR_TMPL.format(doctor_ref="d.id")
    sql = """
        select
            d.*,
            {rating_expr} as rating,
            u.gender,
            u.date_of_birth,
            c.city,
            c.region,
            c.metro,
            coalesce(
                array(
                    select s2.name
                    from doctor_specializations ds2
                    join specializations s2 on s2.id = ds2.specialization_id
                    where ds2.doctor_id = d.id
                    order by s2.name
                ),
                array[]::varchar[]
            ) as specialization_names
        from doctors d
        join users u on u.id = d.user_id
        left join clinics c on c.id = d.clinic_id
        where 1=1
    """.format(rating_expr=rating_expr)
    params = {"limit": safe_limit, "offset": safe_offset}

    if city is not None:
        sql += " and c.city = :city"
        params["city"] = city

    if region is not None:
        sql += " and c.region = :region"
        params["region"] = region

    if metro is not None:
        sql += " and c.metro = :metro"
        params["metro"] = metro

    if online_only:
        sql += " and d.online_available = true"

    if min_price is not None:
        sql += " and d.price is not null and d.price >= :min_price"
        params["min_price"] = min_price

    if max_price is not None:
        sql += " and d.price is not null and d.price <= :max_price"
        params["max_price"] = max_price

    if min_rating is not None:
        sql += f" and {rating_expr} >= :min_rating"
        params["min_rating"] = min_rating

    if gender is not None:
        sql += " and u.gender = :gender"
        params["gender"] = gender

    if min_age is not None:
        sql += """
            and u.date_of_birth is not null
            and extract(year from age(now(), u.date_of_birth)) >= :min_age
        """
        params["min_age"] = min_age

    if max_age is not None:
        sql += """
            and u.date_of_birth is not null
            and extract(year from age(now(), u.date_of_birth)) <= :max_age
        """
        params["max_age"] = max_age

    if min_experience is not None:
        sql += " and d.experience is not null and d.experience >= :min_exp"
        params["min_exp"] = min_experience

    if max_experience is not None:
        sql += " and d.experience is not null and d.experience <= :max_exp"
        params["max_exp"] = max_experience

    if specialization_ids:
        sql += """
            and exists (
                select 1 from doctor_specializations ds
                where ds.doctor_id = d.id
                  and ds.specialization_id = any(:spec_ids)
            )
        """
        params["spec_ids"] = specialization_ids

    if date_filter is not None:
        sql += """
            and exists (
                select 1
                from appointment_slots s2
                where s2.doctor_id = d.id
                  and s2.is_booked = false
                  and date(s2.start_time) = :slot_date
            )
        """
        params["slot_date"] = date_filter

    sql += f"""
            order by {rating_expr} desc, d.price asc nulls last, d.id
            limit :limit offset :offset
        """

    rows = s.execute(text(sql), params).mappings().all()
    return [dict(r) for r in rows]


def list_medical_records_for_client(s: Session, client_id: int) -> List[Dict]:
    rows = s.execute(
        text(
            """
            select *
            from medical_records
            where client_id = :c
            order by id desc
            """
        ),
        {"c": client_id},
    ).mappings().all()
    return [dict(r) for r in rows]


def list_appointments_for_doctor(s: Session, doctor_id: int) -> List[Dict]:
    rows = s.execute(
        text(
            """
            select a.*
            from appointments a
            join appointment_slots s2 on a.slot_id = s2.id
            where s2.doctor_id = :d
            order by a.id desc
            """
        ),
        {"d": doctor_id},
    ).mappings().all()
    return [dict(r) for r in rows]


def list_patients_for_doctor(s: Session, doctor_id: int) -> List[Dict]:
    rows = s.execute(
        text(
            """
            select distinct
                c.id as client_id,
                c.user_id as user_id,
                u.name,
                u.surname,
                u.patronymic,
                u.phone_number,
                u.date_of_birth,
                u.avatar,
                u.gender
            from appointments a
            join appointment_slots s2 on a.slot_id = s2.id
            join clients c on a.client_id = c.id
            join users u on c.user_id = u.id
            where s2.doctor_id = :d
            """
        ),
        {"d": doctor_id},
    ).mappings().all()
    return [dict(r) for r in rows]


# ============================================================================
# Chat Sessions Repository
# ============================================================================

def get_or_create_chat_session(s, user_id: int, session_id: Optional[str] = None):
    """
    Get existing chat session or create new one
    
    Args:
        s: SQLAlchemy session
        user_id: User ID
        session_id: Optional UUID to resume existing session
    
    Returns:
        dict with session data (id, user_id, session_id, messages, created_at, updated_at)
    """
    if session_id:
        # Try to find existing session
        row = s.execute(
            text("""
                select id, user_id, session_id, messages, created_at, updated_at
                from chat_sessions
                where session_id = :sid and user_id = :uid
            """),
            {"sid": session_id, "uid": user_id}
        ).mappings().first()
        
        if row:
            return dict(row)
    
    # Create new session
    row = s.execute(
        text("""
            insert into chat_sessions(user_id, messages)
            values (:uid, '[]'::jsonb)
            returning id, user_id, session_id, messages, created_at, updated_at
        """),
        {"uid": user_id}
    ).mappings().first()
    
    s.commit()
    return dict(row)


def update_chat_session_messages(s, session_id: str, messages: list):
    """
    Update messages in chat session
    
    Args:
        s: SQLAlchemy session
        session_id: UUID of session
        messages: List of message dicts to store in JSONB
    """
    import json
    
    s.execute(
        text("""
            update chat_sessions
            set messages = cast(:msgs as jsonb)
            where session_id = :sid
        """),
        {"sid": str(session_id), "msgs": json.dumps(messages)}
    )
    s.commit()


def get_chat_history(s, user_id: int, limit: int = 50):
    """
    Get chat history for user
    
    Args:
        s: SQLAlchemy session
        user_id: User ID
        limit: Maximum number of sessions to return
    
    Returns:
        List of session dicts ordered by updated_at DESC
    """
    rows = s.execute(
        text("""
            select id, user_id, session_id, messages, created_at, updated_at
            from chat_sessions
            where user_id = :uid
            order by updated_at desc
            limit :lim
        """),
        {"uid": user_id, "lim": limit}
    ).mappings().all()
    
    # Convert UUID to string for JSON serialization
    result = []
    for r in rows:
        session_dict = dict(r)
        session_dict["session_id"] = str(session_dict["session_id"])
        result.append(session_dict)
    
    return result


def delete_chat_session(s, session_id: str):
    """Delete chat session by session_id"""
    s.execute(
        text("delete from chat_sessions where session_id = :sid"),
        {"sid": session_id}
    )
    s.commit()
