BEGIN;

-- Индексы под фильтрацию врачей
CREATE INDEX IF NOT EXISTS idx_doctors_rating ON doctors(rating);
CREATE INDEX IF NOT EXISTS idx_users_gender ON users(gender);
CREATE INDEX IF NOT EXISTS idx_users_birthdate ON users(date_of_birth);
CREATE INDEX IF NOT EXISTS idx_slots_time ON appointment_slots(start_time);
CREATE INDEX IF NOT EXISTS idx_specialization_link ON doctor_specializations(doctor_id, specialization_id);

COMMIT;