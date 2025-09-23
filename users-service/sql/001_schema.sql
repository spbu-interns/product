-- (по приколу) справочник клиник
create table if not exists clinics (
  id bigserial primary key,
  name varchar(255) not null
);

create table if not exists users (
  id             bigserial primary key,
  email          varchar(255) unique not null,
  login          varchar(100) unique not null,
  password_hash  varchar(255) not null,
  role           varchar(20)  not null default 'CLIENT',
  first_name     varchar(100),
  last_name      varchar(100),
  patronymic     varchar(100),
  phone_number   varchar(20),
  clinic_id      bigint references clinics(id),
  is_active      boolean not null default true,
  created_at     timestamp not null default now(),
  updated_at     timestamp not null default now(),
  -- валидные роли
  constraint users_role_chk check (role in ('CLIENT','DOCTOR','ADMIN')),
  -- для DOCTOR имя/фамилия обязательны
  constraint users_doctor_name_chk
    check (role <> 'DOCTOR' or (first_name is not null and last_name is not null))
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