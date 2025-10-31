-- 011_hotfix_notes_fk.sql
ALTER TABLE doctor_notes
  ALTER COLUMN doctor_id DROP NOT NULL;

ALTER TABLE doctor_notes
  DROP CONSTRAINT IF EXISTS doctor_notes_doctor_id_fkey;

ALTER TABLE doctor_notes
  ADD CONSTRAINT doctor_notes_doctor_id_fkey
  FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE SET NULL;