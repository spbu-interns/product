-- 017_appointment_reviews.sql
-- Table for per-appointment patient reviews and notification tracking

CREATE TABLE IF NOT EXISTS appointment_reviews (
                                                   id bigserial PRIMARY KEY,
                                                   appointment_id bigint UNIQUE REFERENCES appointments(id) ON DELETE CASCADE,
    doctor_id bigint NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    client_id bigint NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_reviews_client_id ON appointment_reviews(client_id);
CREATE INDEX IF NOT EXISTS ix_reviews_doctor_id ON appointment_reviews(doctor_id);

DROP TRIGGER IF EXISTS trg_reviews_updated ON appointment_reviews;
CREATE TRIGGER trg_reviews_updated
    BEFORE UPDATE ON appointment_reviews
    FOR EACH ROW EXECUTE PROCEDURE set_updated_at();

-- Track review invitations so we can notify patients right after completion
CREATE TABLE IF NOT EXISTS appointment_review_requests (
                                                           id bigserial PRIMARY KEY,
                                                           appointment_id bigint UNIQUE REFERENCES appointments(id) ON DELETE CASCADE,
    client_id bigint NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    doctor_id bigint NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    sent_at TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_review_requests_client_id ON appointment_review_requests(client_id);