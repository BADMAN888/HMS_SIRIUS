ALTER TABLE users ADD COLUMN full_name VARCHAR(150);

UPDATE users SET full_name = email WHERE full_name IS NULL;

ALTER TABLE users ALTER COLUMN full_name SET NOT NULL;

ALTER TABLE reservation_history ADD COLUMN user_name VARCHAR(150);

UPDATE reservation_history SET user_name = user_email;

ALTER TABLE reservation_history ALTER COLUMN user_name SET NOT NULL;

ALTER TABLE reservation_history DROP COLUMN user_email;
