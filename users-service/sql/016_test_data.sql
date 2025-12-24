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

-- 5. ДОПОЛНИТЕЛЬНЫЕ ДОКТОРА
DO $$
DECLARE
  v_clinic1_id BIGINT;
  v_clinic2_id BIGINT;
  v_clinic3_id BIGINT;
  v_user_id BIGINT;
  v_doctor_id BIGINT;
  v_spec_id INT;
BEGIN
  SELECT id INTO v_clinic1_id FROM clinics WHERE name = 'Клиника "Здоровье+"' LIMIT 1;
  SELECT id INTO v_clinic2_id FROM clinics WHERE name = 'Клиника "МедСервис"' LIMIT 1;
  SELECT id INTO v_clinic3_id FROM clinics WHERE name = 'Клиника "ПроМед"' LIMIT 1;
  
  -- Доктор 6: Хирург
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'morozov.surgeon@test.com',
    'dr_morozov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'DOCTOR',
    'Алексей',
    'Морозов',
    'Павлович',
    '1975-01-28',
    '+7 (911) 000-0106',
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
      'Врач-хирург высшей категории',
      'Абдоминальная хирургия, лапароскопические операции. Доктор медицинских наук.',
      TRUE,
      4.9,
      22,
      5000.00,
      FALSE
    )
    ON CONFLICT (user_id) DO NOTHING
    RETURNING id INTO v_doctor_id;
    
    IF v_doctor_id IS NOT NULL THEN
      SELECT id INTO v_spec_id FROM specializations WHERE name = 'Хирург' LIMIT 1;
      IF v_spec_id IS NOT NULL THEN
        INSERT INTO doctor_specializations (doctor_id, specialization_id)
        VALUES (v_doctor_id, v_spec_id)
        ON CONFLICT DO NOTHING;
      END IF;
    END IF;
  END IF;

  -- Доктор 7: Офтальмолог
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'lebedeva.ophth@test.com',
    'dr_lebedeva',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'DOCTOR',
    'Наталья',
    'Лебедева',
    'Олеговна',
    '1989-11-05',
    '+7 (911) 000-0107',
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
      'Врач-офтальмолог',
      'Диагностика и лечение заболеваний глаз. Подбор очков и контактных линз.',
      TRUE,
      4.8,
      9,
      2800.00,
      TRUE
    )
    ON CONFLICT (user_id) DO NOTHING
    RETURNING id INTO v_doctor_id;
    
    IF v_doctor_id IS NOT NULL THEN
      SELECT id INTO v_spec_id FROM specializations WHERE name = 'Офтальмолог' LIMIT 1;
      IF v_spec_id IS NOT NULL THEN
        INSERT INTO doctor_specializations (doctor_id, specialization_id)
        VALUES (v_doctor_id, v_spec_id)
        ON CONFLICT DO NOTHING;
      END IF;
    END IF;
  END IF;

END $$;

-- 6. ДОПОЛНИТЕЛЬНЫЕ КЛИЕНТЫ
DO $$
DECLARE
  v_clinic1_id BIGINT;
  v_clinic2_id BIGINT;
  v_user_id BIGINT;
BEGIN
  SELECT id INTO v_clinic1_id FROM clinics WHERE name = 'Клиника "Здоровье+"' LIMIT 1;
  SELECT id INTO v_clinic2_id FROM clinics WHERE name = 'Клиника "МедСервис"' LIMIT 1;
  
  -- Клиент 7
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'sokolova.anna@test.com',
    'anna_sokolova',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Анна',
    'Соколова',
    'Петровна',
    '1991-07-17',
    '+7 (911) 000-0207',
    'FEMALE',
    v_clinic1_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, emergency_contact_name, emergency_contact_number, address, snils, passport)
    VALUES (
      v_user_id,
      'O-',
      168.0,
      62.0,
      'Соколов Петр (отец)',
      '+7 (911) 000-0217',
      'г. Санкт-Петербург, ул. Гороховая, д. 56, кв. 23',
      '555-666-777 88',
      '4019 888999'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 8
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'vasiliev.igor@test.com',
    'igor_vasiliev',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Игорь',
    'Васильев',
    'Андреевич',
    '1984-03-22',
    '+7 (911) 000-0208',
    'MALE',
    v_clinic2_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, address, dms_oms)
    VALUES (
      v_user_id,
      'A+',
      177.0,
      85.0,
      'г. Санкт-Петербург, пр. Стачек, д. 102, кв. 67',
      'ДМС Ресо-Гарантия'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 9
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'petrova.vera@test.com',
    'vera_petrova',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Вера',
    'Петрова',
    'Николаевна',
    '1996-05-30',
    '+7 (911) 000-0209',
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
      'AB-',
      163.0,
      54.0,
      'Петрова Наталья (мать)',
      '+7 (911) 000-0219',
      'г. Санкт-Петербург, ул. Декабристов, д. 7, кв. 89'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 10
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'karpov.maxim@test.com',
    'maxim_karpov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Максим',
    'Карпов',
    'Юрьевич',
    '1989-12-11',
    '+7 (911) 000-0210',
    'MALE',
    v_clinic2_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, address, snils, passport, dms_oms)
    VALUES (
      v_user_id,
      'B+',
      181.0,
      88.0,
      'г. Санкт-Петербург, ул. Некрасова, д. 44, кв. 15',
      '444-555-666 77',
      '4020 123456',
      'ОМС 5555666677778888'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 11
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'mikhailova.yulia@test.com',
    'yulia_mikhailova',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Юлия',
    'Михайлова',
    'Викторовна',
    '1994-09-08',
    '+7 (911) 000-0211',
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
      'A-',
      166.0,
      59.0,
      'Михайлов Виктор (отец)',
      '+7 (911) 000-0221',
      'г. Санкт-Петербург, пр. Просвещения, д. 90, кв. 120'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 12
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'orlov.roman@test.com',
    'roman_orlov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Роман',
    'Орлов',
    'Станиславович',
    '1986-04-16',
    '+7 (911) 000-0212',
    'MALE',
    v_clinic1_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, address, dms_oms)
    VALUES (
      v_user_id,
      'O+',
      179.0,
      92.0,
      'г. Санкт-Петербург, ул. Чайковского, д. 28, кв. 3',
      'ДМС ВТБ Страхование'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 13
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'belova.oksana@test.com',
    'oksana_belova',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Оксана',
    'Белова',
    'Дмитриевна',
    '1991-01-25',
    '+7 (911) 000-0213',
    'FEMALE',
    v_clinic2_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, emergency_contact_name, emergency_contact_number, address, snils, passport)
    VALUES (
      v_user_id,
      'B-',
      171.0,
      68.0,
      'Белов Дмитрий (брат)',
      '+7 (911) 000-0223',
      'г. Санкт-Петербург, ул. Восстания, д. 33, кв. 77',
      '222-333-444 55',
      '4021 567890'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 14
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'nikitin.pavel@test.com',
    'pavel_nikitin',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Павел',
    'Никитин',
    'Артемович',
    '1997-10-29',
    '+7 (911) 000-0214',
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
      'A+',
      174.0,
      71.0,
      'г. Санкт-Петербург, пр. Культуры, д. 15, кв. 205'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

END $$;

-- 7. ДОПОЛНИТЕЛЬНЫЕ КЛИНИКИ
INSERT INTO clinics (name, description, address, city, region, phone, email, site, metro, working_hours)
VALUES
  ('Клиника "ВитаЛайф"', 
   'Современный медицинский центр широкого профиля',
   'ул. Ленина, д. 45, корп. 2',
   'Санкт-Петербург',
   'Санкт-Петербург',
   '+7 (812) 333-22-11',
   'info@vitalife.ru',
   'https://vitalife.ru',
   'Невский проспект',
   '{"пн-пт": "07:30-21:00", "сб-вс": "09:00-18:00"}'::jsonb
  ),
  ('Клиника "Медлюкс"',
   'Премиум клиника с европейским стандартом обслуживания',
   'Красный проспект, д. 82',
   'Новосибирск',
   'Новосибирская область',
   '+7 (383) 777-88-99',
   'contact@medlux.ru',
   'https://medlux.ru',
   'Площадь Ленина',
   '{"круглосуточно": "24/7"}'::jsonb
  ),
  ('Клиника "ДокторПлюс"',
   'Семейная клиника с индивидуальным подходом',
   'ул. Гагарина, д. 15',
   'Екатеринбург',
   'Свердловская область',
   '+7 (343) 444-55-66',
   'hello@doctorplus.ru',
   'https://doctorplus.ru',
   'Площадь 1905 года',
   '{"пн-сб": "08:00-20:00", "вс": "10:00-16:00"}'::jsonb
  )
ON CONFLICT DO NOTHING;

-- 8. ДОПОЛНИТЕЛЬНЫЕ АДМИНЫ
DO $$
DECLARE
  v_clinic_id BIGINT;
  v_user_id BIGINT;
BEGIN
  -- Админ 2 (ВитаЛайф)
  SELECT id INTO v_clinic_id FROM clinics WHERE name = 'Клиника "ВитаЛайф"' LIMIT 1;
  
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'admin2@test.com',
    'admin_sidorov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'ADMIN',
    'Сергей',
    'Сидоров',
    'Петрович',
    '+7 (812) 999-11-22',
    'MALE',
    v_clinic_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO admins (user_id, clinic_id, position)
    VALUES (v_user_id, v_clinic_id, 'Главный администратор')
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Админ 3 (Медлюкс)
  SELECT id INTO v_clinic_id FROM clinics WHERE name = 'Клиника "Медлюкс"' LIMIT 1;
  
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'admin3@test.com',
    'admin_nikolaeva',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'ADMIN',
    'Татьяна',
    'Николаева',
    'Владимировна',
    '+7 (383) 888-77-66',
    'FEMALE',
    v_clinic_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO admins (user_id, clinic_id, position)
    VALUES (v_user_id, v_clinic_id, 'Директор клиники')
    ON CONFLICT (user_id) DO NOTHING;
  END IF;
END $$;

-- 9. ДОПОЛНИТЕЛЬНЫЕ ДОКТОРА (7 специалистов)
DO $$
DECLARE
  v_clinic1_id BIGINT;
  v_clinic2_id BIGINT;
  v_clinic3_id BIGINT;
  v_user_id BIGINT;
  v_doctor_id BIGINT;
  v_spec_id INT;
BEGIN
  SELECT id INTO v_clinic1_id FROM clinics WHERE name = 'Клиника "ВитаЛайф"' LIMIT 1;
  SELECT id INTO v_clinic2_id FROM clinics WHERE name = 'Клиника "Медлюкс"' LIMIT 1;
  SELECT id INTO v_clinic3_id FROM clinics WHERE name = 'Клиника "ДокторПлюс"' LIMIT 1;
  
  -- Доктор 8: Дерматолог (мужчина)
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'ivanov.derm@test.com',
    'dr_ivanov_derm',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'DOCTOR',
    'Владимир',
    'Иванов',
    'Алексеевич',
    '1981-07-14',
    '+7 (812) 111-22-33',
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
      'Врач-дерматолог',
      'Лечение кожных заболеваний, акне, экземы. Косметология и дерматовенерология.',
      TRUE,
      4.7,
      12,
      3500.00,
      TRUE
    )
    ON CONFLICT (user_id) DO NOTHING
    RETURNING id INTO v_doctor_id;
    
    IF v_doctor_id IS NOT NULL THEN
      SELECT id INTO v_spec_id FROM specializations WHERE name = 'Дерматолог' LIMIT 1;
      IF v_spec_id IS NOT NULL THEN
        INSERT INTO doctor_specializations (doctor_id, specialization_id) VALUES (v_doctor_id, v_spec_id) ON CONFLICT DO NOTHING;
      END IF;
    END IF;
  END IF;

  -- Доктор 9: Эндокринолог (женщина)
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'gromova.endo@test.com',
    'dr_gromova',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'DOCTOR',
    'Людмила',
    'Громова',
    'Игоревна',
    '1985-03-22',
    '+7 (812) 222-33-44',
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
      'Врач-эндокринолог',
      'Диагностика и лечение диабета, заболеваний щитовидной железы и гормональных нарушений.',
      TRUE,
      4.9,
      9,
      4000.00,
      TRUE
    )
    ON CONFLICT (user_id) DO NOTHING
    RETURNING id INTO v_doctor_id;
    
    IF v_doctor_id IS NOT NULL THEN
      SELECT id INTO v_spec_id FROM specializations WHERE name = 'Эндокринолог' LIMIT 1;
      IF v_spec_id IS NOT NULL THEN
        INSERT INTO doctor_specializations (doctor_id, specialization_id) VALUES (v_doctor_id, v_spec_id) ON CONFLICT DO NOTHING;
      END IF;
    END IF;
  END IF;

  -- Доктор 10: Уролог (мужчина)
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'belov.uro@test.com',
    'dr_belov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'DOCTOR',
    'Павел',
    'Белов',
    'Сергеевич',
    '1979-11-08',
    '+7 (383) 333-44-55',
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
      'Врач-уролог',
      'Диагностика и лечение заболеваний мочеполовой системы. Консультации по урологическим вопросам.',
      TRUE,
      4.8,
      15,
      3800.00,
      FALSE
    )
    ON CONFLICT (user_id) DO NOTHING
    RETURNING id INTO v_doctor_id;
    
    IF v_doctor_id IS NOT NULL THEN
      SELECT id INTO v_spec_id FROM specializations WHERE name = 'Уролог' LIMIT 1;
      IF v_spec_id IS NOT NULL THEN
        INSERT INTO doctor_specializations (doctor_id, specialization_id) VALUES (v_doctor_id, v_spec_id) ON CONFLICT DO NOTHING;
      END IF;
    END IF;
  END IF;

  -- Доктор 11: Гинеколог (женщина)
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'romanova.gyn@test.com',
    'dr_romanova',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'DOCTOR',
    'Ирина',
    'Романова',
    'Дмитриевна',
    '1987-05-17',
    '+7 (383) 444-55-66',
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
      'Врач-гинеколог',
      'Ведение беременности, женское здоровье, эндоскопическая хирургия. Консультации и диагностика.',
      TRUE,
      4.9,
      8,
      4200.00,
      TRUE
    )
    ON CONFLICT (user_id) DO NOTHING
    RETURNING id INTO v_doctor_id;
    
    IF v_doctor_id IS NOT NULL THEN
      SELECT id INTO v_spec_id FROM specializations WHERE name = 'Гинеколог' LIMIT 1;
      IF v_spec_id IS NOT NULL THEN
        INSERT INTO doctor_specializations (doctor_id, specialization_id) VALUES (v_doctor_id, v_spec_id) ON CONFLICT DO NOTHING;
      END IF;
    END IF;
  END IF;

  -- Доктор 12: ЛОР (мужчина)
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'zaitsev.lor@test.com',
    'dr_zaitsev',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'DOCTOR',
    'Егор',
    'Зайцев',
    'Николаевич',
    '1983-09-25',
    '+7 (343) 555-66-77',
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
      'Врач-оториноларинголог (ЛОР)',
      'Диагностика и лечение заболеваний уха, горла и носа. Микрохирургия ЛОР-органов.',
      TRUE,
      4.6,
      11,
      3300.00,
      TRUE
    )
    ON CONFLICT (user_id) DO NOTHING
    RETURNING id INTO v_doctor_id;
    
    IF v_doctor_id IS NOT NULL THEN
      SELECT id INTO v_spec_id FROM specializations WHERE name = 'ЛОР' LIMIT 1;
      IF v_spec_id IS NOT NULL THEN
        INSERT INTO doctor_specializations (doctor_id, specialization_id) VALUES (v_doctor_id, v_spec_id) ON CONFLICT DO NOTHING;
      END IF;
    END IF;
  END IF;

  -- Доктор 13: Психиатр (женщина)
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'krasnova.psych@test.com',
    'dr_krasnova',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'DOCTOR',
    'Виктория',
    'Краснова',
    'Андреевна',
    '1986-12-03',
    '+7 (343) 666-77-88',
    'FEMALE',
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
      'Врач-психиатр',
      'Лечение депрессии, тревожных расстройств. Психотерапия и консультирование по психическому здоровью.',
      TRUE,
      4.8,
      10,
      5000.00,
      TRUE
    )
    ON CONFLICT (user_id) DO NOTHING
    RETURNING id INTO v_doctor_id;
    
    IF v_doctor_id IS NOT NULL THEN
      SELECT id INTO v_spec_id FROM specializations WHERE name = 'Психиатр' LIMIT 1;
      IF v_spec_id IS NOT NULL THEN
        INSERT INTO doctor_specializations (doctor_id, specialization_id) VALUES (v_doctor_id, v_spec_id) ON CONFLICT DO NOTHING;
      END IF;
    END IF;
  END IF;

  -- Доктор 14: Ортопед (мужчина)
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'titov.ortho@test.com',
    'dr_titov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'DOCTOR',
    'Артем',
    'Титов',
    'Владимирович',
    '1980-04-30',
    '+7 (343) 777-88-99',
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
      'Врач-ортопед',
      'Травматология и ортопедия. Эндопротезирование суставов, лечение заболеваний опорно-двигательного аппарата.',
      TRUE,
      4.9,
      14,
      4500.00,
      FALSE
    )
    ON CONFLICT (user_id) DO NOTHING
    RETURNING id INTO v_doctor_id;
    
    IF v_doctor_id IS NOT NULL THEN
      SELECT id INTO v_spec_id FROM specializations WHERE name = 'Ортопед' LIMIT 1;
      IF v_spec_id IS NOT NULL THEN
        INSERT INTO doctor_specializations (doctor_id, specialization_id) VALUES (v_doctor_id, v_spec_id) ON CONFLICT DO NOTHING;
      END IF;
    END IF;
  END IF;

END $$;

-- 10. ДОПОЛНИТЕЛЬНЫЕ КЛИЕНТЫ (15 человек)
DO $$
DECLARE
  v_clinic1_id BIGINT;
  v_clinic2_id BIGINT;
  v_clinic3_id BIGINT;
  v_user_id BIGINT;
BEGIN
  SELECT id INTO v_clinic1_id FROM clinics WHERE name = 'Клиника "ВитаЛайф"' LIMIT 1;
  SELECT id INTO v_clinic2_id FROM clinics WHERE name = 'Клиника "Медлюкс"' LIMIT 1;
  SELECT id INTO v_clinic3_id FROM clinics WHERE name = 'Клиника "ДокторПлюс"' LIMIT 1;
  
  -- Клиент 13: Мужчина
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'antonov.denis@test.com',
    'denis_antonov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Денис',
    'Антонов',
    'Григорьевич',
    '1990-06-12',
    '+7 (812) 111-22-33',
    'MALE',
    v_clinic1_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, emergency_contact_name, emergency_contact_number, address, snils)
    VALUES (
      v_user_id,
      'A+',
      178,
      82,
      'Антонова Марина (жена)',
      '+7 (812) 111-22-34',
      'Санкт-Петербург, ул. Пушкина, д. 12, кв. 45',
      '123-456-789 11'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 14: Женщина
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'andreeva.svetlana@test.com',
    'svetlana_andreeva',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Светлана',
    'Андреева',
    'Ивановна',
    '1988-03-20',
    '+7 (812) 222-33-44',
    'FEMALE',
    v_clinic1_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, emergency_contact_name, emergency_contact_number, address, passport, dms_oms)
    VALUES (
      v_user_id,
      'B-',
      165,
      58,
      'Андреев Петр (муж)',
      '+7 (812) 222-33-45',
      'Санкт-Петербург, Невский пр., д. 88, кв. 23',
      '4012 345678',
      'ОМС'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 15: Мужчина
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'borisov.konstantin@test.com',
    'konstantin_borisov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Константин',
    'Борисов',
    'Олегович',
    '1995-11-08',
    '+7 (383) 333-44-55',
    'MALE',
    v_clinic2_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, address)
    VALUES (
      v_user_id,
      'O+',
      185,
      90,
      'Новосибирск, ул. Красный проспект, д. 35, кв. 67'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 16: Женщина
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'gerasimova.natalia@test.com',
    'natalia_gerasimova',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Наталья',
    'Герасимова',
    'Сергеевна',
    '1992-07-15',
    '+7 (383) 444-55-66',
    'FEMALE',
    v_clinic2_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, emergency_contact_name, emergency_contact_number, address, snils, dms_oms)
    VALUES (
      v_user_id,
      'A-',
      170,
      62,
      'Герасимова Мария (мать)',
      '+7 (383) 444-55-67',
      'Новосибирск, ул. Ленина, д. 50, кв. 12',
      '987-654-321 44',
      'ДМС Согаз'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 17: Мужчина
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'danilov.viktor@test.com',
    'viktor_danilov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Виктор',
    'Данилов',
    'Андреевич',
    '1987-02-28',
    '+7 (383) 555-66-77',
    'MALE',
    v_clinic2_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, address, passport)
    VALUES (
      v_user_id,
      'B+',
      180,
      85,
      'Новосибирск, ул. Гоголя, д. 22, кв. 8',
      '5401 654321'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 18: Женщина
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'egorova.tatiana@test.com',
    'tatiana_egorova',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Татьяна',
    'Егорова',
    'Викторовна',
    '1994-09-05',
    '+7 (343) 666-77-88',
    'FEMALE',
    v_clinic3_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, emergency_contact_name, emergency_contact_number, address)
    VALUES (
      v_user_id,
      'O-',
      168,
      55,
      'Егоров Николай (отец)',
      '+7 (343) 666-77-89',
      'Екатеринбург, ул. Ленина, д. 78, кв. 90'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 19: Мужчина
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'frolov.oleg@test.com',
    'oleg_frolov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Олег',
    'Фролов',
    'Петрович',
    '1991-12-20',
    '+7 (343) 777-88-99',
    'MALE',
    v_clinic3_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, address, snils, passport)
    VALUES (
      v_user_id,
      'A+',
      176,
      78,
      'Екатеринбург, ул. Малышева, д. 45, кв. 11',
      '456-789-123 55',
      '6601 987654'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 20: Женщина
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'galkina.inna@test.com',
    'inna_galkina',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Инна',
    'Галкина',
    'Дмитриевна',
    '1989-05-10',
    '+7 (343) 888-99-00',
    'FEMALE',
    v_clinic3_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, emergency_contact_name, emergency_contact_number, address, dms_oms)
    VALUES (
      v_user_id,
      'B-',
      172,
      64,
      'Галкин Андрей (брат)',
      '+7 (343) 888-99-01',
      'Екатеринбург, ул. Горького, д. 33, кв. 55',
      'ОМС'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 21: Мужчина
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'ilyin.artem@test.com',
    'artem_ilyin',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Артем',
    'Ильин',
    'Владимирович',
    '1993-08-18',
    '+7 (812) 333-44-55',
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
      'O+',
      182,
      88,
      'Санкт-Петербург, ул. Маяковского, д. 67, кв. 34'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 22: Женщина
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'kiseleva.larisa@test.com',
    'larisa_kiseleva',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Лариса',
    'Киселева',
    'Александровна',
    '1986-01-25',
    '+7 (812) 444-55-66',
    'FEMALE',
    v_clinic1_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, emergency_contact_name, emergency_contact_number, address, snils)
    VALUES (
      v_user_id,
      'A-',
      167,
      60,
      'Киселев Сергей (муж)',
      '+7 (812) 444-55-67',
      'Санкт-Петербург, пр. Ветеранов, д. 100, кв. 77',
      '321-654-987 22'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 23: Мужчина
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'lebedev.grigoriy@test.com',
    'grigoriy_lebedev',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Григорий',
    'Лебедев',
    'Николаевич',
    '1990-10-30',
    '+7 (383) 666-77-88',
    'MALE',
    v_clinic2_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, address, passport, dms_oms)
    VALUES (
      v_user_id,
      'B+',
      179,
      83,
      'Новосибирск, ул. Чехова, д. 19, кв. 5',
      '5402 111222',
      'ДМС Альфа-страхование'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 24: Женщина
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'makarova.kristina@test.com',
    'kristina_makarova',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Кристина',
    'Макарова',
    'Игоревна',
    '1996-04-12',
    '+7 (383) 777-88-99',
    'FEMALE',
    v_clinic2_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, emergency_contact_name, emergency_contact_number, address)
    VALUES (
      v_user_id,
      'O-',
      163,
      52,
      'Макарова Елена (мать)',
      '+7 (383) 777-88-00',
      'Новосибирск, ул. Мира, д. 25, кв. 40'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 25: Мужчина
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'nesterov.andrey@test.com',
    'andrey_nesterov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Андрей',
    'Нестеров',
    'Валентинович',
    '1985-07-22',
    '+7 (343) 999-00-11',
    'MALE',
    v_clinic3_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, address, snils, passport)
    VALUES (
      v_user_id,
      'A+',
      177,
      81,
      'Екатеринбург, ул. Белинского, д. 56, кв. 22',
      '789-123-456 66',
      '6602 333444'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 26: Женщина
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'orlova.veronika@test.com',
    'veronika_orlova',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Вероника',
    'Орлова',
    'Сергеевна',
    '1991-11-14',
    '+7 (343) 000-11-22',
    'FEMALE',
    v_clinic3_id,
    TRUE,
    NOW()
  )
  ON CONFLICT (email) DO NOTHING
  RETURNING id INTO v_user_id;
  
  IF v_user_id IS NOT NULL THEN
    INSERT INTO clients (user_id, blood_type, height, weight, emergency_contact_name, emergency_contact_number, address, dms_oms)
    VALUES (
      v_user_id,
      'B+',
      169,
      59,
      'Орлов Михаил (брат)',
      '+7 (343) 000-11-23',
      'Екатеринбург, ул. Восточная, д. 12, кв. 88',
      'ОМС'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

  -- Клиент 27: Мужчина
  INSERT INTO users (email, login, password_hash, role, name, surname, patronymic, date_of_birth, phone_number, gender, clinic_id, is_active, email_verified_at)
  VALUES (
    'pavlov.boris@test.com',
    'boris_pavlov',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqVHQvLBdm',
    'CLIENT',
    'Борис',
    'Павлов',
    'Михайлович',
    '1988-03-07',
    '+7 (812) 555-66-77',
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
      'O+',
      184,
      92,
      'Санкт-Петербург, ул. Рубинштейна, д. 29, кв. 15'
    )
    ON CONFLICT (user_id) DO NOTHING;
  END IF;

END $$;

COMMIT;

-- Информация для разработчиков:
-- 
-- Созданы:
-- - 6 клиник (Здоровье+, МедСервис, ПроМед, ВитаЛайф, Медлюкс, ДокторПлюс)
-- - 3 админа (admin@test.com, admin2@test.com, admin3@test.com)
-- - 14 докторов:
--   * Первые клиники: dr_petrov (Терапевт), dr_sidorova (Кардиолог), dr_kuznetsov (Невролог),
--     dr_volkova (Педиатр), dr_sokolov (Стоматолог), dr_morozov (Хирург), dr_lebedeva (Офтальмолог)
--   * Новые клиники: dr_ivanov_derm (Дерматолог), dr_gromova (Эндокринолог), dr_belov (Уролог),
--     dr_romanova (Гинеколог), dr_zaitsev (ЛОР), dr_krasnova (Психиатр), dr_titov (Ортопед)
-- - 29 клиентов
--
-- Пароль для всех: password123
--
-- Всего пользователей: 46 (3 админа + 14 докторов + 29 клиентов)
-- Аватарки загружаются отдельно через: python scripts/upload_avatars_via_api.py
