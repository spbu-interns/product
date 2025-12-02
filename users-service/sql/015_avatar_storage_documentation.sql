-- 015_avatar_storage_documentation.sql
-- Документация к системе хранения аватарок

-- Комментарий к полю avatar в таблице users
COMMENT ON COLUMN users.avatar IS 'Относительный путь к файлу аватарки, например: avatars/user_1_a1b2c3d4.jpg. NULL если аватарка не установлена.';

-- Индекс для быстрого поиска пользователей с аватарками (опционально)
CREATE INDEX IF NOT EXISTS idx_users_avatar_exists ON users(id) WHERE avatar IS NOT NULL;
