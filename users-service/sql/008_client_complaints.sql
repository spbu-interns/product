-- 008_client_complaints.sql

CREATE TABLE IF NOT EXISTS client_complaints (
  id          bigserial PRIMARY KEY,
  client_id   bigint REFERENCES clients(id) ON DELETE CASCADE,
  title       VARCHAR(255) NOT NULL,
  description TEXT,
  created_at  TIMESTAMP NOT NULL DEFAULT now(),
  updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_client_complaints_client_id ON client_complaints(client_id);

DROP TRIGGER IF EXISTS trg_client_complaints_updated ON client_complaints;
CREATE TRIGGER trg_client_complaints_updated
BEFORE UPDATE ON client_complaints
FOR EACH ROW EXECUTE PROCEDURE set_updated_at();

-- Перенос данных из patient_complaints (если было)
-- patient_complaints.patient_id -> это users.id. Нужно замапить на clients.id
INSERT INTO client_complaints (client_id, title, description, created_at, updated_at)
SELECT c.id AS client_id,
       pc.title,
       pc.body AS description,
       pc.created_at,
       pc.updated_at
FROM patient_complaints pc
JOIN clients c ON c.user_id = pc.patient_id
ON CONFLICT DO NOTHING;  -- на случай повторного прогона

-- Старую таблицу patient_complaints сейчас НЕ удаляем.