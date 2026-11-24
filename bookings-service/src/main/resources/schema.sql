CREATE TABLE IF NOT EXISTS bookings(
  id SERIAL PRIMARY KEY,
  room_id INT NOT NULL,
  room_name TEXT NOT NULL,
  user_id INT NOT NULL,
  user_name TEXT NOT NULL,
  start_time TIMESTAMPTZ NOT NULL,
  end_time TIMESTAMPTZ NOT NULL
);
