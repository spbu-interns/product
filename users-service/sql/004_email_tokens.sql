ALTER TABLE users
  ADD COLUMN IF NOT EXISTS email_verified_at   timestamp NULL,
  ADD COLUMN IF NOT EXISTS password_changed_at timestamp NOT NULL DEFAULT now();

-- для уже существующих записей выставим password_changed_at = created_at (если создано раньше)
UPDATE users
SET password_changed_at = COALESCE(created_at, now())
WHERE password_changed_at IS NULL;


-- Одноразовые токены — подтверждение email
CREATE TABLE IF NOT EXISTS email_verifications (
  id          bigserial PRIMARY KEY,
  user_id     bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  char(64) NOT NULL,                         -- SHA-256 хэш токена (сам токен не храним)
  expires_at  timestamp NOT NULL,                        -- срок жизни
  consumed_at timestamp NULL,                            -- когда был использован; до использования NULL
  created_at  timestamp NOT NULL DEFAULT now()
);

-- уникальность самого «секретного» значения
CREATE UNIQUE INDEX IF NOT EXISTS ux_email_verifications_token_hash
  ON email_verifications(token_hash);

-- быстрые выборки
CREATE INDEX IF NOT EXISTS ix_email_verifications_user_id
  ON email_verifications(user_id);

CREATE INDEX IF NOT EXISTS ix_email_verifications_expires_at
  ON email_verifications(expires_at);

-- (не строго обязательно) гарантируем, что у пользователя не более одного «непотраченного» токена
-- NB: partial unique index с динамическим NOW() нельзя; делаем по consumed_at
CREATE UNIQUE INDEX IF NOT EXISTS ux_email_verifications_one_unconsumed
  ON email_verifications(user_id)
  WHERE consumed_at IS NULL;

-- Одноразовые токены — сброс пароля (структура аналогична)
CREATE TABLE IF NOT EXISTS password_reset_tokens (
  id          bigserial PRIMARY KEY,
  user_id     bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  char(64) NOT NULL,                         -- SHA-256 хэш
  expires_at  timestamp NOT NULL,
  consumed_at timestamp NULL,
  created_at  timestamp NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_password_reset_tokens_token_hash
  ON password_reset_tokens(token_hash);

CREATE INDEX IF NOT EXISTS ix_password_reset_tokens_user_id
  ON password_reset_tokens(user_id);

CREATE INDEX IF NOT EXISTS ix_password_reset_tokens_expires_at
  ON password_reset_tokens(expires_at);

CREATE UNIQUE INDEX IF NOT EXISTS ux_password_reset_tokens_one_unconsumed
  ON password_reset_tokens(user_id)
  WHERE consumed_at IS NULL;