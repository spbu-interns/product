-- 006_domain_core.sql

-- 2) clinics: расширим по ТЗ
ALTER TABLE clinics
  ADD COLUMN IF NOT EXISTS description  TEXT,
  ADD COLUMN IF NOT EXISTS address      TEXT,
  ADD COLUMN IF NOT EXISTS city         VARCHAR(100),
  ADD COLUMN IF NOT EXISTS region       VARCHAR(100),
  ADD COLUMN IF NOT EXISTS phone        VARCHAR(50),
  ADD COLUMN IF NOT EXISTS email        VARCHAR(255),
  ADD COLUMN IF NOT EXISTS site         TEXT,
  ADD COLUMN IF NOT EXISTS working_hours JSONB;

-- 3) базовая функция updated_at (idempotent)
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$;

-- 4) clients (1:1 к users)
CREATE TABLE IF NOT EXISTS clients (
  id                       bigserial PRIMARY KEY,
  user_id                  bigint UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  blood_type               VARCHAR(5),
  height                   NUMERIC(5,2),
  weight                   NUMERIC(5,2),
  emergency_contact_name   VARCHAR(255),
  emergency_contact_number VARCHAR(50),
  address                  TEXT,
  snils                    VARCHAR(20),
  passport                 VARCHAR(20),
  dms_oms                  VARCHAR(50),
  created_at               TIMESTAMP NOT NULL DEFAULT now(),
  updated_at               TIMESTAMP NOT NULL DEFAULT now()
);

DROP TRIGGER IF EXISTS trg_clients_updated ON clients;
CREATE TRIGGER trg_clients_updated
BEFORE UPDATE ON clients
FOR EACH ROW EXECUTE PROCEDURE set_updated_at();

-- 5) doctors (1:1 к users)
CREATE TABLE IF NOT EXISTS doctors (
  id           bigserial PRIMARY KEY,
  user_id      bigint UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  clinic_id    bigint REFERENCES clinics(id) ON DELETE SET NULL,
  profession   VARCHAR(255) NOT NULL,
  info         TEXT,
  is_confirmed BOOLEAN DEFAULT FALSE,
  rating       NUMERIC(3,2) DEFAULT 0.0,
  experience   INT,
  price        NUMERIC(10,2),
  created_at   TIMESTAMP NOT NULL DEFAULT now(),
  updated_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_doctors_clinic_id ON doctors(clinic_id);

DROP TRIGGER IF EXISTS trg_doctors_updated ON doctors;
CREATE TRIGGER trg_doctors_updated
BEFORE UPDATE ON doctors
FOR EACH ROW EXECUTE PROCEDURE set_updated_at();

-- 6) admins (1:1 к users)
CREATE TABLE IF NOT EXISTS admins (
  id         bigserial PRIMARY KEY,
  user_id    bigint UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  clinic_id  bigint REFERENCES clinics(id) ON DELETE CASCADE,
  position   VARCHAR(100),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_admins_clinic_id ON admins(clinic_id);

DROP TRIGGER IF EXISTS trg_admins_updated ON admins;
CREATE TRIGGER trg_admins_updated
BEFORE UPDATE ON admins
FOR EACH ROW EXECUTE PROCEDURE set_updated_at();