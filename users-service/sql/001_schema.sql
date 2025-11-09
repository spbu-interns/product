-- (по приколу) справочник клиник
create table if not exists clinics (
  id bigserial primary key,
  name varchar(255) not null
);

CREATE TABLE users (
  id             BIGSERIAL PRIMARY KEY,
  email          VARCHAR(255) UNIQUE NOT NULL,
  login          VARCHAR(100) UNIQUE NOT NULL,
  password_hash  VARCHAR(255) NOT NULL,
  role           VARCHAR(20)  NOT NULL DEFAULT 'CLIENT'
                 CHECK (role IN ('CLIENT','DOCTOR','ADMIN')),
  -- Только новые поля ФИО:
  name           VARCHAR(100),
  surname        VARCHAR(100),
  patronymic     VARCHAR(100),
  date_of_birth  DATE,
  phone_number   VARCHAR(20),
  avatar         TEXT,
  gender         VARCHAR(20) CHECK (gender IN ('MALE','FEMALE')),
  clinic_id      BIGINT REFERENCES clinics(id),
  is_active      BOOLEAN NOT NULL DEFAULT TRUE,

  -- если используешь email верификацию/сброс пароля:
  email_verified_at    TIMESTAMP,
  password_changed_at  TIMESTAMP NOT NULL DEFAULT now(),

  created_at     TIMESTAMP NOT NULL DEFAULT now(),
  updated_at     TIMESTAMP NOT NULL DEFAULT now()
);
-- триггер auto-updated updated_at чтоб обновлять поле updated_at при каждом изменении чего то для данного юзера
create or replace function set_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

drop trigger if exists trg_users_set_updated_at on users;
create trigger trg_users_set_updated_at
before update on users
for each row execute procedure set_updated_at();