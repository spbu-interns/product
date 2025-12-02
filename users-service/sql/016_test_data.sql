-- 016_test_data.sql
-- Полноценные тестовые данные для разработки
-- Создает клиники, докторов, клиентов, админа с заполненными профилями

BEGIN;

-- Очистка старых тестовых данных (опционально)
-- DELETE FROM doctor_specializations;
-- DELETE FROM doctors WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@test.com');
-- DELETE FROM clients WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@test.com');
-- DELETE FROM admins WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@test.com');
-- DELETE FROM users WHERE email LIKE '%@test.com';
-- DELETE FROM clinics WHERE name LIKE 'Клиника%';

-- 1. КЛИНИКИ
INSERT INTO clinics (name, description, address, city, region, phone, email, site, metro, working_hours)
VALUES
  ('Клиника "Здоровье+"', 
   'Многопрофильный медицинский центр с современным оборудованием',
   'ул. Ленина, д. 15',
   'Санкт-Петербург',
   'Ленинградская область',
   '+7 (812) 555-0001',
   'info@zdorovie-plus.ru',
   'https://zdorovie-plus.ru',
   'Площадь Восстания',
   '{"пн-пт": "08:00-20:00", "сб": "09:00-18:00", "вс": "выходной"}'::jsonb
  ),
  ('Клиника "МедСервис"',
   'Семейная клиника с опытными специалистами',
   'пр. Невский, д. 88',
   'Санкт-Петербург',
   'Ленинградская область',
   '+7 (812) 555-0002',
   'contact@medservice.ru',
   'https://medservice.ru',
   'Маяковская',
   '{"пн-сб": "09:00-21:00", "вс": "10:00-16:00"}'::jsonb
  ),
  ('Клиника "ПроМед"',
   'Специализированный центр диагностики и лечения',
   'ул. Московская, д. 33',
   'Санкт-Петербург',
   'Ленинградская область',
   '+7 (812) 555-0003',
   'reception@promed.ru',
   'https://promed.ru',
   'Технологический институт',
   '{"круглосуточно": "24/7"}'::jsonb
  )
ON CONFLICT DO NOTHING;

-- Пароль для всех тестовых пользователей: "password123"
-- Хэш: $2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm

-- 2. АДМИН
DO $$
DECLARE
  v_clinic_id BIGINT;
  v_user_id BIGINT;
BEGIN
  -- Получаем ID первой клиники
  SELECT id INTO v_clinic_id FROM clinics WHERE name = 'Клиника "Здоровье+"' LIMIT 1;
  
  -- Создаем пользователя-админа
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'admin@test.com',
    'admin_ivanov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'ADMIN',
    'Иван',
    'Иванов',
    'Петрович',
    '+7 (911) 000-0001',
    'MALE',
    v_clinic_id,
    TRUE,
    
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  -- Создаем запись в admins
  IF v_user_id IS NOT NULL THEN
    INSERT INTO admins (user_id, clinic_id, position)
    VALUES (v_user_id, v_clinic_id, 'Главный администратор')
    ON CONFLICT (user_id) DO NOTHING;
  END IF;
END $$;

-- 3. ДОКТОРА
DO $$
DECLARE
  v_clinic1_id BIGINT;
  v_clinic2_id BIGINT;
  v_clinic3_id BIGINT;
  v_user_id BIGINT;
  v_doctor_id BIGINT;
  v_spec_id INT;
BEGIN
  -- ID клиник
  SELECT id INTO v_clinic1_id FROM clinics WHERE name = 'Клиника "Здоровье+"' LIMIT 1;
  SELECT id INTO v_clinic2_id FROM clinics WHERE name = 'Клиника "МедСервис"' LIMIT 1;
  SELECT id INTO v_clinic3_id FROM clinics WHERE name = 'Клиника "ПроМед"' LIMIT 1;
  
  -- Доктор 1: Терапевт
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'petrov.therapist@test.com',
    'dr_petrov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'DOCTOR',
    'Петр',
    'Петров',
    'Сергеевич',
    '1980-05-15',
    '+7 (911) 000-0101',
    'MALE',
    v_clinic1_id,
    TRUE,
    
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO doctors (user_id, clinic_id, profession, info, is_confirmed, rating, experience, price, online_available)
    VALUES (
      v_user_id,
      v_clinic1_id,
      'Врач-терапевт высшей категории',
      'Специализируется на диагностике и лечении заболеваний внутренних органов. Стаж работы 15 лет.',
      TRUE,
      4.8,
      15,
      2500.00,
      TRUE
    )
    ON CONFLICT (user_id) DO NOTHING
    RETURNING id INTO v_doctor_id;
    
    -- Добавляем специализацию
    IF v_doctor_id IS NOT NULL THEN
      SELECT id INTO v_spec_id FROM specializations WHERE name = 'Терапевт' LIMIT 1;
      IF v_spec_id IS NOT NULL THEN
        INSERT INTO doctor_specializations (doctor_id, specialization_id)
        VALUES (v_doctor_id, v_spec_id)
        ON CONFLICT DO NOTHING;
      END IF;
    END IF;
  END IF;

  -- Доктор 2: Кардиолог
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'sidorova.cardio@test.com',
    'dr_sidorova',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'DOCTOR',
    'Анна',
    'Сидорова',
    'Викторовна',
    '1985-08-22',
    '+7 (911) 000-0102',
    'FEMALE',
    v_clinic1_id,
    TRUE,
    
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO doctors (user_id, clinic_id, profession, info, is_confirmed, rating, experience, price, online_available)
    VALUES (
      v_user_id,
      v_clinic1_id,
      'Врач-кардиолог',
      'Диагностика и лечение заболеваний сердечно-сосудистой системы. Кандидат медицинских наук.',
      TRUE,
      4.9,
      12,
      3500.00,
      TRUE
    )
    ON CONFLICT (user_id) DO NOTHING
    RETURNING id INTO v_doctor_id;
    
    IF v_doctor_id IS NOT NULL THEN
      SELECT id INTO v_spec_id FROM specializations WHERE name = 'Кардиолог' LIMIT 1;
      IF v_spec_id IS NOT NULL THEN
        INSERT INTO doctor_specializations (doctor_id, specialization_id)
        VALUES (v_doctor_id, v_spec_id)
        ON CONFLICT DO NOTHING;
      END IF;
    END IF;
  END IF;

  -- Доктор 3: Невролог
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'kuznetsov.neuro@test.com',
    'dr_kuznetsov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'DOCTOR',
    'Дмитрий',
    'Кузнецов',
    'Александрович',
    '1978-11-30',
    '+7 (911) 000-0103',
    'MALE',
    v_clinic2_id,
    TRUE,
    
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO doctors (user_id, clinic_id, profession, info, is_confirmed, rating, experience, price, online_available)
    VALUES (
      v_user_id,
      v_clinic2_id,
      'Врач-невролог высшей категории',
      'Лечение заболеваний нервной системы, головных болей, неврозов.',
      TRUE,
      4.7,
      18,
      3000.00,
      FALSE
    )
    ON CONFLICT (user_id) DO NOTHING
    RETURNING id INTO v_doctor_id;
    
    IF v_doctor_id IS NOT NULL THEN
      SELECT id INTO v_spec_id FROM specializations WHERE name = 'Невролог' LIMIT 1;
      IF v_spec_id IS NOT NULL THEN
        INSERT INTO doctor_specializations (doctor_id, specialization_id)
        VALUES (v_doctor_id, v_spec_id)
        ON CONFLICT DO NOTHING;
      END IF;
    END IF;
  END IF;

  -- Доктор 4: Педиатр
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'volkova.pediatr@test.com',
    'dr_volkova',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'DOCTOR',
    'Елена',
    'Волкова',
    'Игоревна',
    '1990-03-12',
    '+7 (911) 000-0104',
    'FEMALE',
    v_clinic2_id,
    TRUE,
    
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO doctors (user_id, clinic_id, profession, info, is_confirmed, rating, experience, price, online_available)
    VALUES (
      v_user_id,
      v_clinic2_id,
      'Врач-педиатр',
      'Детский врач. Наблюдение детей от рождения до 18 лет.',
      TRUE,
      5.0,
      8,
      2200.00,
      TRUE
    )
    ON CONFLICT (user_id) DO NOTHING
    RETURNING id INTO v_doctor_id;
    
    IF v_doctor_id IS NOT NULL THEN
      SELECT id INTO v_spec_id FROM specializations WHERE name = 'Педиатр' LIMIT 1;
      IF v_spec_id IS NOT NULL THEN
        INSERT INTO doctor_specializations (doctor_id, specialization_id)
        VALUES (v_doctor_id, v_spec_id)
        ON CONFLICT DO NOTHING;
      END IF;
    END IF;
  END IF;

  -- Доктор 5: Стоматолог
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'sokolov.dent@test.com',
    'dr_sokolov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'DOCTOR',
    'Михаил',
    'Соколов',
    'Владимирович',
    '1983-07-18',
    '+7 (911) 000-0105',
    'MALE',
    v_clinic3_id,
    TRUE,
    
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO doctors (user_id, clinic_id, profession, info, is_confirmed, rating, experience, price, online_available)
    VALUES (
      v_user_id,
      v_clinic3_id,
      'Врач-стоматолог-терапевт',
      'Лечение кариеса, пульпита, периодонтита. Эстетическая реставрация зубов.',
      TRUE,
      4.6,
      10,
      4000.00,
      FALSE
    )
    ON CONFLICT (user_id) DO NOTHING
    RETURNING id INTO v_doctor_id;
    
    IF v_doctor_id IS NOT NULL THEN
      SELECT id INTO v_spec_id FROM specializations WHERE name = 'Стоматолог' LIMIT 1;
      IF v_spec_id IS NOT NULL THEN
        INSERT INTO doctor_specializations (doctor_id, specialization_id)
        VALUES (v_doctor_id, v_spec_id)
        ON CONFLICT DO NOTHING;
      END IF;
    END IF;
  END IF;

END $$;

-- 4. КЛИЕНТЫ
DO $$
DECLARE
  v_clinic1_id BIGINT;
  v_user_id BIGINT;
BEGIN
  SELECT id INTO v_clinic1_id FROM clinics WHERE name = 'Клиника "Здоровье+"' LIMIT 1;
  
  -- Клиент 1
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'ivanova.maria@test.com',
    'maria_ivanova',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Мария',
    'Иванова',
    'Андреевна',
    '1992-04-25',
    '+7 (911) 000-0201',
    'FEMALE',
    v_clinic1_id,
    TRUE,
    
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, emergency_contact_name, emergency_contact_number, address, snils, passport, dms_oms)
    VALUES (
      v_user_id,
      'A+',
      165.0,
      58.0,
      'Иванов Андрей (муж)',
      '+7 (911) 000-0211',
      'г. Санкт-Петербург, ул. Пушкина, д. 10, кв. 5',
      '123-456-789 01',
      '4012 345678',
      'ОМС 1234567890123456'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 2
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'smirnov.alex@test.com',
    'alex_smirnov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Александр',
    'Смирнов',
    'Дмитриевич',
    '1988-09-10',
    '+7 (911) 000-0202',
    'MALE',
    v_clinic1_id,
    TRUE,
    
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, emergency_contact_name, emergency_contact_number, address, snils, passport, dms_oms)
    VALUES (
      v_user_id,
      'B+',
      180.0,
      82.0,
      'Смирнова Ольга (жена)',
      '+7 (911) 000-0212',
      'г. Санкт-Петербург, пр. Энгельса, д. 45, кв. 120',
      '987-654-321 09',
      '4018 654321',
      'ДМС АльфаСтрахование'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 3
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'popova.olga@test.com',
    'olga_popova',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Ольга',
    'Попова',
    'Сергеевна',
    '1995-12-03',
    '+7 (911) 000-0203',
    'FEMALE',
    v_clinic1_id,
    TRUE,
    
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, emergency_contact_name, emergency_contact_number, address)
    VALUES (
      v_user_id,
      'O+',
      170.0,
      65.0,
      'Попов Сергей (отец)',
      '+7 (911) 000-0213',
      'г. Санкт-Петербург, ул. Ломоносова, д. 23, кв. 78'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 4
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'novikov.dmitry@test.com',
    'dmitry_novikov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Дмитрий',
    'Новиков',
    'Михайлович',
    '1990-06-20',
    '+7 (911) 000-0204',
    'MALE',
    v_clinic1_id,
    TRUE,
    
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, address)
    VALUES (
      v_user_id,
      'AB+',
      175.0,
      78.0,
      'г. Санкт-Петербург, ул. Маяковского, д. 12, кв. 34'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 5
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'fedorova.elena@test.com',
    'elena_fedorova',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Елена',
    'Федорова',
    'Владимировна',
    '1987-02-14',
    '+7 (911) 000-0205',
    'FEMALE',
    v_clinic1_id,
    TRUE,
    
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, emergency_contact_name, emergency_contact_number, address, snils, passport, dms_oms)
    VALUES (
      v_user_id,
      'A-',
      162.0,
      55.0,
      'Федоров Владимир (отец)',
      '+7 (911) 000-0215',
      'г. Санкт-Петербург, пр. Ветеранов, д. 78, кв. 90',
      '111-222-333 44',
      '4015 111222',
      'ОМС 9876543210987654'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 6
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'kozlov.sergey@test.com',
    'sergey_kozlov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Сергей',
    'Козлов',
    'Иванович',
    '1993-08-08',
    '+7 (911) 000-0206',
    'MALE',
    v_clinic1_id,
    TRUE,
    
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, address)
    VALUES (
      v_user_id,
      'B-',
      183.0,
      90.0,
      'г. Санкт-Петербург, ул. Рубинштейна, д. 5, кв. 12'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

END $$;

COMMIT;

-- Информация для разработчиков:
-- 
-- Созданы:
-- - 3 клиники (Здоровье+, МедСервис, ПроМед)
-- - 1 админ (admin@test.com / admin_ivanov)
-- - 5 докторов:
--   * dr_petrov - Терапевт (Здоровье+)
--   * dr_sidorova - Кардиолог (Здоровье+)
--   * dr_kuznetsov - Невролог (МедСервис)
--   * dr_volkova - Педиатр (МедСервис)
--   * dr_sokolov - Стоматолог (ПроМед)
-- - 6 клиентов (maria_ivanova, alex_smirnov, olga_popova, dmitry_novikov, elena_fedorova, sergey_kozlov)
--
-- Пароль для всех: password123
--
-- Аватарки указаны как пути в поле avatar, но нужно физически загрузить файлы через скрипт upload_test_avatars.py
