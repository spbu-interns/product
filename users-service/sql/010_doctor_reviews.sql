-- 010_doctor_reviews.sql

CREATE TABLE IF NOT EXISTS doctor_reviews (
  id         bigserial PRIMARY KEY,
  doctor_id  bigint REFERENCES doctors(id) ON DELETE CASCADE,
  client_id  bigint REFERENCES clients(id) ON DELETE SET NULL,
  rating     NUMERIC(2,1) CHECK (rating BETWEEN 1 AND 5),
  comment    TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE (doctor_id, client_id)
);

CREATE INDEX IF NOT EXISTS ix_reviews_doctor_id ON doctor_reviews(doctor_id);

DROP TRIGGER IF EXISTS trg_reviews_updated ON doctor_reviews;
CREATE TRIGGER trg_reviews_updated
BEFORE UPDATE ON doctor_reviews
FOR EACH ROW EXECUTE PROCEDURE set_updated_at();