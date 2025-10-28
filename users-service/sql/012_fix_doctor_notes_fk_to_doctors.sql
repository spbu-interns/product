BEGIN;

-- 1) Сносим неправильный FK (указывает на users)
ALTER TABLE doctor_notes
  DROP CONSTRAINT IF EXISTS doctor_notes_doctor_id_fkey;

-- 2) Конвертируем значения: раньше в doctor_notes.doctor_id лежал users.id врача,
--    теперь должно лежать doctors.id
UPDATE doctor_notes dn
SET doctor_id = d.id
FROM doctors d
WHERE dn.doctor_id = d.user_id;

-- 3) На всякий случай занулим то, что не конвертировалось
UPDATE doctor_notes dn
SET doctor_id = NULL
WHERE doctor_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM doctors d WHERE d.id = dn.doctor_id);

-- 4) Ставим правильный FK на doctors(id)
ALTER TABLE doctor_notes
  ADD CONSTRAINT doctor_notes_doctor_id_fkey
  FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE SET NULL;

COMMIT;