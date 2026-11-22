CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE showtimes
    ADD CONSTRAINT ex_showtimes_active_room_occupied
    EXCLUDE USING gist (
        room_id WITH =,
        tsrange(
            starts_at AT TIME ZONE 'UTC',
            (ends_at AT TIME ZONE 'UTC') + INTERVAL '15 minutes',
            '[)'
        ) WITH &&
    )
    WHERE (status IN ('SCHEDULED', 'ONGOING'));
