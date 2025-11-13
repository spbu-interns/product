insert into clinics (name) values
  ('Base Clinic')
on conflict do nothing;

with c as (select id from clinics order by id limit 1)
insert into users (email, login, password_hash, role, name, surname, phone_number, clinic_id, is_active)
select 'alice@example.com','alice',
       '$2b$12$2uJg7mKrbU7ZxO1v1I2cju0c1F4h4E8gLQ1M9c0s7eN5n3cW7d3xK',
       'CLIENT', null, null, '+10000000001', c.id, true
from c
on conflict do nothing;

with c as (select id from clinics order by id limit 1)
insert into users (email, login, password_hash, role, name, surname, phone_number, clinic_id, is_active)
select 'doc@example.com','dr_house',
       '$2b$12$2uJg7mKrbU7ZxO1v1I2cju0c1F4h4E8gLQ1M9c0s7eN5n3cW7d3xK',
       'DOCTOR','Gregory','House', '+10000000002', c.id, true
from c
on conflict do nothing;

with c as (select id from clinics order by id limit 1)
insert into users (email, login, password_hash, role, name, surname, phone_number, clinic_id, is_active)
select 'admin@example.com','root',
       '$2b$12$2uJg7mKrbU7ZxO1v1I2cju0c1F4h4E8gLQ1M9c0s7eN5n3cW7d3xK',
       'ADMIN', null, null, '+10000000003', c.id, true
from c
on conflict do nothing;