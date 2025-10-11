-- Жалобы пациентов и заметки врачей

-- 1) Таблица жалоб
CREATE TABLE IF NOT EXISTS patient_complaints (
  id           bigserial PRIMARY KEY,
  patient_id   bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title        varchar(200) NOT NULL,
  body         text NOT NULL,
  status       varchar(20) NOT NULL DEFAULT 'OPEN', -- OPEN, IN_PROGRESS, CLOSED
  created_at   timestamp NOT NULL DEFAULT now(),
  updated_at   timestamp NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_patient_complaints_patient_id
  ON patient_complaints(patient_id);

CREATE INDEX IF NOT EXISTS ix_patient_complaints_status
  ON patient_complaints(status);

-- 2) Таблица заметок врача по пациенту
CREATE TABLE IF NOT EXISTS doctor_notes (
  id           bigserial PRIMARY KEY,
  patient_id   bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  doctor_id    bigint NOT NULL REFERENCES users(id) ON DELETE SET NULL,
  note         text NOT NULL,
  visibility   varchar(20) NOT NULL DEFAULT 'INTERNAL', -- INTERNAL, PATIENT
  created_at   timestamp NOT NULL DEFAULT now(),
  updated_at   timestamp NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_doctor_notes_patient_id
  ON doctor_notes(patient_id);

CREATE INDEX IF NOT EXISTS ix_doctor_notes_doctor_id
  ON doctor_notes(doctor_id);

-- 3) Триггер для updated_at (если ещё нет)
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_patient_complaints_updated ON patient_complaints;
CREATE TRIGGER trg_patient_complaints_updated
BEFORE UPDATE ON patient_complaints
FOR EACH ROW EXECUTE PROCEDURE set_updated_at();

DROP TRIGGER IF EXISTS trg_doctor_notes_updated ON doctor_notes;
CREATE TRIGGER trg_doctor_notes_updated
BEFORE UPDATE ON doctor_notes
FOR EACH ROW EXECUTE PROCEDURE set_updated_at();