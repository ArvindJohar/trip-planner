-- Add the owner column first, allowing existing trips to remain valid.
ALTER TABLE trips
ADD COLUMN user_id BIGINT;

-- Link each trip to an existing user.
ALTER TABLE trips
ADD CONSTRAINT fk_trips_user
FOREIGN KEY (user_id) REFERENCES users(id);
