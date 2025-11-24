BEGIN;

-- 1. Типы приёма
--------------------------------------------------

CREATE TABLE IF NOT EXISTS appointment_types (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL   -- "Первичный", "Повторный" и т.п.
);

-- 2. Связь appointments -> appointment_types
--------------------------------------------------

ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS appointment_type_id INT
        REFERENCES appointment_types(id);

-- 3. Индекс для быстрых запросов свободных слотов врача
--------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_slots_availability
    ON appointment_slots (doctor_id, is_booked, start_time);


-- 4. Функция: создать слот
--------------------------------------------------
-- ВАЖНО: доктор в таблице doctors с id BIGINT,
-- поэтому параметры беру BIGINT.

CREATE OR REPLACE FUNCTION create_slot(
    p_doctor_id BIGINT,
    p_start     TIMESTAMP,
    p_end       TIMESTAMP
)
RETURNS appointment_slots AS $$
DECLARE
    new_slot appointment_slots;
BEGIN
    INSERT INTO appointment_slots (doctor_id, start_time, end_time, is_booked)
    VALUES (p_doctor_id, p_start, p_end, FALSE)
    RETURNING * INTO new_slot;

    RETURN new_slot;
END;
$$ LANGUAGE plpgsql;


-- 5. Функция: забронировать слот (создать appointments + пометить слот)
--------------------------------------------------

CREATE OR REPLACE FUNCTION book_slot(
    p_slot_id             BIGINT,
    p_client_id           BIGINT,
    p_comments            TEXT DEFAULT NULL,
    p_appointment_type_id INT  DEFAULT NULL
)
RETURNS appointments AS $$
DECLARE
    slot_record    appointment_slots;
    new_appointment appointments;
BEGIN
    SELECT * INTO slot_record
    FROM appointment_slots
    WHERE id = p_slot_id
    FOR UPDATE;

    IF slot_record IS NULL THEN
        RAISE EXCEPTION 'Slot % not found', p_slot_id;
    END IF;

    IF slot_record.is_booked THEN
        RAISE EXCEPTION 'Slot % already booked', p_slot_id;
    END IF;

    UPDATE appointment_slots
    SET is_booked = TRUE,
        updated_at = NOW()
    WHERE id = p_slot_id;

    INSERT INTO appointments (slot_id, client_id, comments, appointment_type_id)
    VALUES (p_slot_id, p_client_id, p_comments, p_appointment_type_id)
    RETURNING * INTO new_appointment;

    RETURN new_appointment;
END;
$$ LANGUAGE plpgsql;


-- 6. Функция: отменить запись (освободить слот, статус CANCELED)
--------------------------------------------------

CREATE OR REPLACE FUNCTION cancel_appointment(
    p_appointment_id BIGINT
)
RETURNS VOID AS $$
DECLARE
    slot_id_local BIGINT;
BEGIN
    SELECT slot_id
    INTO slot_id_local
    FROM appointments
    WHERE id = p_appointment_id;

    IF slot_id_local IS NULL THEN
        RAISE EXCEPTION 'Appointment % not found', p_appointment_id;
    END IF;

    UPDATE appointments
    SET status      = 'CANCELED',
        canceled_at = NOW(),
        updated_at  = NOW()
    WHERE id = p_appointment_id;

    UPDATE appointment_slots
    SET is_booked = FALSE,
        updated_at = NOW()
    WHERE id = slot_id_local;
END;
$$ LANGUAGE plpgsql;


-- 7. Функция: удалить слот (только если он свободен)
--------------------------------------------------

CREATE OR REPLACE FUNCTION delete_slot(
    p_slot_id BIGINT
)
RETURNS VOID AS $$
DECLARE
    booked BOOLEAN;
BEGIN
    SELECT is_booked
    INTO booked
    FROM appointment_slots
    WHERE id = p_slot_id;

    IF booked IS NULL THEN
        RAISE EXCEPTION 'Slot % not found', p_slot_id;
    END IF;

    IF booked THEN
        RAISE EXCEPTION 'Cannot delete booked slot %', p_slot_id;
    END IF;

    DELETE FROM appointment_slots
    WHERE id = p_slot_id;
END;
$$ LANGUAGE plpgsql;


-- 8. Функция: получить слоты врача (с опциональным фильтром по дате)
--------------------------------------------------

CREATE OR REPLACE FUNCTION get_doctor_slots(
    p_doctor_id BIGINT,
    p_date      DATE DEFAULT NULL
)
RETURNS TABLE (
    id         BIGINT,
    start_time TIMESTAMP,
    end_time   TIMESTAMP,
    duration   INT,
    is_booked  BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        s.id,
        s.start_time,
        s.end_time,
        s.duration,
        s.is_booked
    FROM appointment_slots s
    WHERE s.doctor_id = p_doctor_id
      AND (p_date IS NULL OR DATE(s.start_time) = p_date)
    ORDER BY s.start_time;
END;
$$ LANGUAGE plpgsql;


-- 9. (опционально) Функция для календаря: доступные даты врача

CREATE OR REPLACE FUNCTION get_doctor_available_dates(
    p_doctor_id BIGINT
)
RETURNS TABLE (
    available_date DATE
) AS $$
BEGIN
    RETURN QUERY
    SELECT DISTINCT DATE(start_time) AS available_date
    FROM appointment_slots
    WHERE doctor_id = p_doctor_id
      AND is_booked = FALSE
    ORDER BY available_date;
END;
$$ LANGUAGE plpgsql;


INSERT INTO appointment_types (name) VALUES
  ('Первичный'),
  ('Повторный'),
  ('Осмотр')
ON CONFLICT (name) DO NOTHING;

COMMIT;