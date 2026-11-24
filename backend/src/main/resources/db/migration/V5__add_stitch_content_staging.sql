CREATE TABLE stitch_content_staging (
    id BIGSERIAL PRIMARY KEY,
    source_key VARCHAR(160) NOT NULL UNIQUE,
    content_type VARCHAR(30) NOT NULL,
    source_file VARCHAR(500) NOT NULL,
    raw_payload JSONB NOT NULL,
    review_reason TEXT NOT NULL,
    review_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_stitch_content_staging_type CHECK (content_type IN ('MOVIE', 'PROMOTION', 'COMBO', 'SHOWTIME', 'STATIC')),
    CONSTRAINT ck_stitch_content_staging_status CHECK (review_status IN ('DRAFT', 'REVIEWED', 'REJECTED'))
);

CREATE INDEX ix_stitch_content_staging_type_status
    ON stitch_content_staging (content_type, review_status, id);
