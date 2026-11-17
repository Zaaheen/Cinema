CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Keep the lowest-ID row when legacy data contains an exact duplicate
-- scheduled showtime. Only cancel the duplicate when it has no bookings.
UPDATE showtimes duplicate
SET status = 'CANCELLED'
WHERE duplicate.status IN ('SCHEDULED', 'ONGOING')
  AND NOT EXISTS (
      SELECT 1
      FROM bookings booking
      WHERE booking.showtime_id = duplicate.id
  )
  AND EXISTS (
      SELECT 1
      FROM showtimes original
      WHERE original.id < duplicate.id
        AND original.room_id = duplicate.room_id
        AND original.movie_id = duplicate.movie_id
        AND original.starts_at = duplicate.starts_at
        AND original.ends_at = duplicate.ends_at
        AND original.status IN ('SCHEDULED', 'ONGOING')
  );

ALTER TABLE showtimes
    ADD CONSTRAINT ex_showtimes_room_schedule
    EXCLUDE USING gist (
        room_id WITH =,
        tsrange(
            starts_at AT TIME ZONE 'UTC',
            (ends_at AT TIME ZONE 'UTC') + INTERVAL '15 minutes',
            '[)'
        ) WITH &&
    )
    WHERE (status IN ('SCHEDULED', 'ONGOING'));
