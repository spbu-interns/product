insert into users (email, login, password_hash, role) values
  ('alice@example.com','alice','$2b$12$2uJg7mKrbU7ZxO1v1I2cju0c1F4h4E8gLQ1M9c0s7eN5n3cW7d3xK','CLIENT'),
  ('doc@example.com','dr_house','$2b$12$2uJg7mKrbU7ZxO1v1I2cju0c1F4h4E8gLQ1M9c0s7eN5n3cW7d3xK','DOCTOR'),
  ('admin@example.com','root','$2b$12$2uJg7mKrbU7ZxO1v1I2cju0c1F4h4E8gLQ1M9c0s7eN5n3cW7d3xK','ADMIN')
on conflict do nothing;