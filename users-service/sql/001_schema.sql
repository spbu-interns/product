create table if not exists users (
  id serial primary key,
  email text not null unique,
  login text not null unique,
  password_hash text not null,
  role text not null check (role in ('CLIENT','DOCTOR','ADMIN'))
);