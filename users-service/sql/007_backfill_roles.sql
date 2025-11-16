-- 007_backfill_roles.sql

-- Создаём clients для всех users.role = 'CLIENT', если нет
INSERT INTO clients (user_id)
SELECT u.id
FROM users u
LEFT JOIN clients c ON c.user_id = u.id
WHERE u.role = 'CLIENT' AND c.user_id IS NULL;

-- Создаём doctors для всех users.role = 'DOCTOR', переносим clinic_id (если был)
INSERT INTO doctors (user_id, clinic_id, profession, is_confirmed)
SELECT u.id, u.clinic_id, COALESCE(u.surname, 'Doctor') || ' ' || COALESCE(u.name, ''), FALSE
FROM users u
LEFT JOIN doctors d ON d.user_id = u.id
WHERE u.role = 'DOCTOR' AND d.user_id IS NULL;

-- Создаём admins для всех users.role = 'ADMIN', переносим clinic_id (если был)
INSERT INTO admins (user_id, clinic_id, position)
SELECT u.id, u.clinic_id, 'Admin'
FROM users u
LEFT JOIN admins a ON a.user_id = u.id
WHERE u.role = 'ADMIN' AND a.user_id IS NULL;

-- (Опционально) users.clinic_id больше не используется напрямую — НЕ удаляем, чтобы не ломать старый код.