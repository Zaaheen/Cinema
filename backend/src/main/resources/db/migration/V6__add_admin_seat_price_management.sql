CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE seat_prices
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    ADD COLUMN origin VARCHAR(30) NOT NULL DEFAULT 'LEGACY_UNREVIEWED',
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN created_by_user_id BIGINT REFERENCES users (id),
    ADD COLUMN updated_by_user_id BIGINT REFERENCES users (id),
    ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN movie_version_key VARCHAR(50);

-- Existing rows lack durable provenance in the schema. They are deliberately
-- quarantined rather than silently treated as operational prices.
UPDATE seat_prices
SET movie_version = NULLIF(regexp_replace(btrim(movie_version), '\s+', ' ', 'g'), ''),
    movie_version_key = NULLIF(upper(regexp_replace(btrim(movie_version), '\s+', ' ', 'g')), ''),
    status = 'INACTIVE',
    origin = 'LEGACY_UNREVIEWED',
    updated_at = NOW();

-- These four rows are uniquely documented in the idempotent local
-- stitch-development seed. They stay inactive and are never public prices.
UPDATE seat_prices
SET origin = 'LOCAL_FIXTURE'
WHERE status = 'INACTIVE'
  AND valid_from = DATE '2026-01-01'
  AND valid_to IS NULL
  AND (seat_type, movie_version, price) IN (
      ('STANDARD', '2D Phụ đề', 90000.00),
      ('VIP', '2D Phụ đề', 120000.00),
      ('STANDARD', 'IMAX Laser 3D', 150000.00),
      ('VIP', 'IMAX Laser 3D', 180000.00)
  );

ALTER TABLE seat_prices
    DROP CONSTRAINT ck_seat_prices_price,
    ADD CONSTRAINT ck_seat_prices_price CHECK (price > 0),
    ADD CONSTRAINT ck_seat_prices_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
    ADD CONSTRAINT ck_seat_prices_origin CHECK (origin IN ('ADMIN_APPROVED', 'LOCAL_FIXTURE', 'LEGACY_UNREVIEWED')),
    ADD CONSTRAINT ck_seat_prices_version_key CHECK (
        (movie_version IS NULL AND movie_version_key IS NULL)
        OR (movie_version IS NOT NULL AND movie_version_key IS NOT NULL)
    );

CREATE INDEX ix_seat_prices_resolve_exact
    ON seat_prices (origin, status, seat_type, movie_version_key, valid_from, valid_to)
    WHERE movie_version_key IS NOT NULL;

CREATE INDEX ix_seat_prices_resolve_generic
    ON seat_prices (origin, status, seat_type, valid_from, valid_to)
    WHERE movie_version_key IS NULL;

ALTER TABLE seat_prices
    ADD CONSTRAINT ex_seat_prices_active_generic_overlap
    EXCLUDE USING gist (
        seat_type WITH =,
        daterange(valid_from, COALESCE(valid_to + 1, 'infinity'::date), '[)') WITH &&
    )
    WHERE (origin = 'ADMIN_APPROVED' AND status = 'ACTIVE' AND movie_version_key IS NULL),
    ADD CONSTRAINT ex_seat_prices_active_exact_overlap
    EXCLUDE USING gist (
        seat_type WITH =,
        movie_version_key WITH =,
        daterange(valid_from, COALESCE(valid_to + 1, 'infinity'::date), '[)') WITH &&
    )
    WHERE (origin = 'ADMIN_APPROVED' AND status = 'ACTIVE' AND movie_version_key IS NOT NULL);

CREATE TABLE seat_price_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    seat_price_id BIGINT NOT NULL REFERENCES seat_prices (id),
    revision BIGINT NOT NULL,
    actor_user_id BIGINT REFERENCES users (id),
    action VARCHAR(30) NOT NULL,
    before_state JSONB,
    after_state JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_seat_price_audit_action CHECK (action IN ('CREATE', 'UPDATE', 'ACTIVATE', 'DEACTIVATE', 'REVISE'))
);

CREATE INDEX ix_seat_price_audit_logs_price_created
    ON seat_price_audit_logs (seat_price_id, created_at DESC, id DESC);
