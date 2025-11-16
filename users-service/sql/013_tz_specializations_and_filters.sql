BEGIN;

-- 1) Специализации (справочник)
CREATE TABLE IF NOT EXISTS specializations (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255) UNIQUE NOT NULL,
    is_popular  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Индексы для быстрых выборок популярных/по имени
CREATE INDEX IF NOT EXISTS idx_specializations_popular_name
    ON specializations (is_popular DESC, name);

-- 2) Связка врач—специализация (многие-ко-многим)
CREATE TABLE IF NOT EXISTS doctor_specializations (
    id                 SERIAL PRIMARY KEY,
    doctor_id          INT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    specialization_id  INT NOT NULL REFERENCES specializations(id) ON DELETE CASCADE,
    UNIQUE (doctor_id, specialization_id)
);

CREATE INDEX IF NOT EXISTS idx_doctor_specializations_doctor
    ON doctor_specializations(doctor_id);
CREATE INDEX IF NOT EXISTS idx_doctor_specializations_spec
    ON doctor_specializations(specialization_id);

-- 3) Добавить метро в клиники
ALTER TABLE clinics
    ADD COLUMN IF NOT EXISTS metro VARCHAR(255);

-- Индексы для фильтров по географии
CREATE INDEX IF NOT EXISTS idx_clinics_city   ON clinics(city);
CREATE INDEX IF NOT EXISTS idx_clinics_region ON clinics(region);
CREATE INDEX IF NOT EXISTS idx_clinics_metro  ON clinics(metro);

-- 4) Флаг онлайн-консультаций у врача
ALTER TABLE doctors
    ADD COLUMN IF NOT EXISTS online_available BOOLEAN NOT NULL DEFAULT FALSE;

-- Индексы под фильтры поиска (стоимость, стаж, онлайн)
CREATE INDEX IF NOT EXISTS idx_doctors_price        ON doctors(price);
CREATE INDEX IF NOT EXISTS idx_doctors_experience   ON doctors(experience);
CREATE INDEX IF NOT EXISTS idx_doctors_online       ON doctors(online_available);
CREATE INDEX IF NOT EXISTS idx_doctors_clinic       ON doctors(clinic_id);

-- 5) Индексы для проверки доступности по времени (свободные слоты)
CREATE INDEX IF NOT EXISTS idx_appointment_slots_doctor_time_booked
    ON appointment_slots(doctor_id, start_time, is_booked);

-- 6) Сид данных по специализациям (из ТЗ)
-- Первые ~10 помечаем как популярные.
WITH seed(name, is_popular) AS (
    VALUES
      ('Терапевт', TRUE),
      ('Педиатр', TRUE),
      ('Стоматолог', TRUE),
      ('Гинеколог', TRUE),
      ('Уролог', TRUE),
      ('Кардиолог', TRUE),
      ('Невролог', TRUE),
      ('Эндокринолог', TRUE),
      ('Дерматолог', TRUE),
      ('Офтальмолог', TRUE),

      ('Отоларинголог', FALSE),
      ('Психолог', FALSE),
      ('Психотерапевт', FALSE),
      ('Психиатр', FALSE),
      ('Травматолог-ортопед', FALSE),
      ('Гастроэнтеролог', FALSE),
      ('Аллерголог-иммунолог', FALSE),
      ('Диетолог', FALSE),
      ('Онколог', FALSE),
      ('Хирург', FALSE),
      ('Ревматолог', FALSE)
)
INSERT INTO specializations(name, is_popular)
SELECT name, is_popular FROM seed
ON CONFLICT (name) DO UPDATE
SET is_popular = EXCLUDED.is_popular;

-- 7) (Необязательно, но полезно) автоматическая синхронизация рейтинга врача
-- по таблице отзывов doctor_reviews (среднее значение).
-- Если у вас уже есть другая логика — этот блок можно пропустить.
CREATE OR REPLACE FUNCTION trg_refresh_doctor_rating() RETURNS TRIGGER AS $$
DECLARE
    v_doctor_id INT := COALESCE(NEW.doctor_id, OLD.doctor_id);
    v_avg FLOAT;
BEGIN
    SELECT AVG(rating)::FLOAT INTO v_avg
    FROM doctor_reviews
    WHERE doctor_id = v_doctor_id;

    UPDATE doctors
       SET rating = COALESCE(v_avg, 0),
           updated_at = NOW()
     WHERE id = v_doctor_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Индекс на отзывы (если его нет)
CREATE INDEX IF NOT EXISTS idx_doctor_reviews_doctor
    ON doctor_reviews(doctor_id);

DROP TRIGGER IF EXISTS trg_reviews_refresh_insert ON doctor_reviews;
DROP TRIGGER IF EXISTS trg_reviews_refresh_update ON doctor_reviews;
DROP TRIGGER IF EXISTS trg_reviews_refresh_delete ON doctor_reviews;

CREATE TRIGGER trg_reviews_refresh_insert
AFTER INSERT ON doctor_reviews
FOR EACH ROW EXECUTE FUNCTION trg_refresh_doctor_rating();

CREATE TRIGGER trg_reviews_refresh_update
AFTER UPDATE ON doctor_reviews
FOR EACH ROW EXECUTE FUNCTION trg_refresh_doctor_rating();

CREATE TRIGGER trg_reviews_refresh_delete
AFTER DELETE ON doctor_reviews
FOR EACH ROW EXECUTE FUNCTION trg_refresh_doctor_rating();

COMMIT;