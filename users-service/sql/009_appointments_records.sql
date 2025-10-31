-- 009_appointments_records.sql

-- 1) Слоты врача
CREATE TABLE IF NOT EXISTS appointment_slots (
  id          bigserial PRIMARY KEY,
  doctor_id   bigint REFERENCES doctors(id) ON DELETE CASCADE,
  start_time  TIMESTAMP NOT NULL,
  end_time    TIMESTAMP NOT NULL,
  duration    INT GENERATED ALWAYS AS (EXTRACT(EPOCH FROM (end_time - start_time)) / 60) STORED,
  is_booked   BOOLEAN DEFAULT FALSE,
  created_at  TIMESTAMP NOT NULL DEFAULT now(),
  updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_slots_doctor_id ON appointment_slots(doctor_id);
CREATE INDEX IF NOT EXISTS ix_slots_start_time ON appointment_slots(start_time);

DROP TRIGGER IF EXISTS trg_slots_updated ON appointment_slots;
CREATE TRIGGER trg_slots_updated
BEFORE UPDATE ON appointment_slots
FOR EACH ROW EXECUTE PROCEDURE set_updated_at();

-- 2) Запись на приём (1:1 со слотом)
CREATE TABLE IF NOT EXISTS appointments (
  id           bigserial PRIMARY KEY,
  slot_id      bigint UNIQUE REFERENCES appointment_slots(id) ON DELETE CASCADE,
  client_id    bigint REFERENCES clients(id) ON DELETE CASCADE,
  status       VARCHAR(30) NOT NULL DEFAULT 'BOOKED' CHECK (status IN ('BOOKED','CANCELED','COMPLETED','NO_SHOW')),
  comments     TEXT,
  created_at   TIMESTAMP NOT NULL DEFAULT now(),
  updated_at   TIMESTAMP NOT NULL DEFAULT now(),
  canceled_at  TIMESTAMP,
  completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_appointments_client_id ON appointments(client_id);

DROP TRIGGER IF EXISTS trg_appointments_updated ON appointments;
CREATE TRIGGER trg_appointments_updated
BEFORE UPDATE ON appointments
FOR EACH ROW EXECUTE PROCEDURE set_updated_at();

-- 3) Медицинские записи
CREATE TABLE IF NOT EXISTS medical_records (
  id              bigserial PRIMARY KEY,
  client_id       bigint REFERENCES clients(id) ON DELETE CASCADE,
  doctor_id       bigint REFERENCES doctors(id) ON DELETE SET NULL,
  appointment_id  bigint REFERENCES appointments(id) ON DELETE SET NULL,
  diagnosis       TEXT,
  symptoms        TEXT,
  treatment       TEXT,
  recommendations TEXT,
  created_at      TIMESTAMP NOT NULL DEFAULT now(),
  updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_medrec_client_id ON medical_records(client_id);
CREATE INDEX IF NOT EXISTS ix_medrec_doctor_id ON medical_records(doctor_id);

DROP TRIGGER IF EXISTS trg_medrec_updated ON medical_records;
CREATE TRIGGER trg_medrec_updated
BEFORE UPDATE ON medical_records
FOR EACH ROW EXECUTE PROCEDURE set_updated_at();

-- 4) Документы к мед.записям
CREATE TABLE IF NOT EXISTS medical_documents (
  id          bigserial PRIMARY KEY,
  record_id   bigint REFERENCES medical_records(id) ON DELETE CASCADE,
  client_id   bigint REFERENCES clients(id) ON DELETE CASCADE,
  filename    VARCHAR(255),
  file_url    TEXT,
  file_type   VARCHAR(50),
  encrypted   BOOLEAN DEFAULT TRUE,
  uploaded_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_meddoc_record_id ON medical_documents(record_id);
CREATE INDEX IF NOT EXISTS ix_meddoc_client_id ON medical_documents(client_id);